package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.exchangeToBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.seconds

@Service
class MatchApiImpl(
    private val base: OsuApiBaseService,
) : OsuMatchApiService {
    override fun getMatchLobby(limit: Int, descending: Boolean, cursor: String?): MatchLobby {
        return request { client ->
            client.get()
                .uri {
                    it.path("matches")
                    it.queryParam("limit", limit)
                    it.queryParam("sort", if (descending) "id_desc" else "id_asc")
                    if (cursor != null) it.queryParam("cursor_string", cursor)
                    it.build()
                }
                .headers(base::insertHeader)
                .exchangeToBody<MatchLobby>()
        }
    }

    override fun getMatch(matchID: Long, times: Int): Match {
        var remaining = times
        val match: Match = getMatchFromAPI(matchID)

        while (remaining > 0) {
            // 1. 记录当前最旧（最小）的 ID
            val currentFirstID = match.events.firstOrNull()?.eventID ?: break

            // 2. 尝试获取比当前最小 ID 更早的数据
            val newMatch = getMatchBefore(matchID, currentFirstID)

            // 3. 执行追加逻辑
            match.append(newMatch)

            // 4. 获取追加后的最小 ID
            val earlierID = match.events.firstOrNull()?.eventID

            // 【核心停止逻辑】：
            // 如果追加后的最小 ID 依然等于追加前的 ID，
            // 或者 newMatch 为空，说明已经溯源到顶了。
            if (earlierID == null || earlierID >= currentFirstID) {
                break
            }

            remaining--
        }

        return match
    }

    override fun getMatchBefore(matchID: Long, eventID: Long): Match {
        return getMatchFromAPI(matchID, eventID, 0)
    }

    override fun getMatchAfter(matchID: Long, eventID: Long): Match {
        return getMatchFromAPI(matchID, 0, eventID)
    }

    override fun getRoom(roomID: Long, fetchAllEvents: Boolean): Room {
        val latest = getRoomEvents(roomID)

        if (!fetchAllEvents) return latest

        var room = latest
        var before = room.events.firstOrNull()?.eventID ?: return room
        var pages = 0

        // events 接口一次最多返回 100 条，且默认只给最近的一页；
        // 用 before 往前翻，才能把长时间房间里早期进行的对局也取回来。
        while (before > room.firstEventID && pages < MAX_ROOM_EVENT_PAGES) {
            val older = getRoomEvents(roomID, before)

            if (older.events.isEmpty()) break

            room = mergeOlder(room, older)

            val next = older.events.firstOrNull()?.eventID ?: break

            if (next >= before) break

            before = next
            pages++
        }

        if (before > room.firstEventID) {
            log.warn(
                "房间 {} 的事件超过 {} 页，只取了最近的 {} 条；更早的轮次可能缺失",
                roomID, MAX_ROOM_EVENT_PAGES + 1, room.events.size
            )
        }

        return room
    }

    private fun getRoomEvents(roomID: Long, before: Long? = null): Room {
        return request { client ->
            client.get()
                .uri {
                    it.path("rooms/${roomID}/events")
                    if (before != null) it.queryParam("before", before)
                    it.queryParam("limit", ROOM_EVENT_PAGE_SIZE)
                    it.build()
                }
                .headers(base::insertHeader)
                .exchangeToBody<Room>()
        }
    }

    /**
     * 把更早的一页合并进房间数据。
     *
     * 只有 [Room.events] 需要拼到前面；谱面、playlist、用户取并集，房间信息保留最新一页的。
     */
    private fun mergeOlder(latest: Room, older: Room): Room = latest.copy(
        beatmaps = (latest.beatmaps + older.beatmaps).distinctBy { it.beatmapID },
        beatmapsets = (latest.beatmapsets + older.beatmapsets).distinctBy { it.beatmapsetID },
        events = (older.events + latest.events).distinctBy { it.eventID }.sortedBy { it.eventID },
        items = (latest.items + older.items).distinctBy { it.listID },
        users = (latest.users + older.users).distinctBy { it.userID },
        firstEventID = minOf(latest.firstEventID, older.firstEventID),
        lastEventID = maxOf(latest.lastEventID, older.lastEventID),
    )

    override fun getRoomInfo(roomID: Long): RoomInfo {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}")
                .headers(base::insertHeader)
                .exchangeToBody<RoomInfo>()
        }
    }

    override fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}/leaderboard")
                .headers(base::insertHeader)
                .exchangeToBody<RoomLeaderBoard>()
        }
    }

    override fun getRooms(infos: List<RoomInfo>, batch: Int): List<Room> {
        val pairs = AsyncMethodExecutor.awaitBatch(
            infos.map { info ->
                Callable { info to getRoom(info.roomID) }
            }, batch, 15.seconds
        )

        return pairs.map { (info, room) ->
            room.copy(roomInfo = info)
        }
    }

    private fun getMatchFromAPI(matchID: Long): Match {
        return request { client ->
            client.get()
                .uri("matches/${matchID}")
                .headers(base::insertHeader)
                .exchangeToBody<Match>()
        }
    }

    private fun getMatchFromAPI(matchID: Long, before: Long, after: Long): Match {
        return request { client ->
            client.get()
                .uri {
                    it.path("matches/${matchID}")
                    if (before != 0L) it.queryParam("before", before)
                    if (after != 0L) it.queryParam("after", after)
                    it.queryParam("limit", 100)
                    it.build()
                }
                .headers(base::insertHeader)
                .exchangeToBody<Match>()
        }
    }

    /**
     * 错误包装
     */
    private fun <T : Any> request(isBackground: Boolean = false, request: (RestClient) -> T): T {
        return try {
            if (isBackground) {
                base.request(isBackground = true, request)
            } else {
                request(base.osuApiRestClient)
            }
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<HttpStatusCodeException>()

            when(ex?.statusCode?.value()) {
                400 -> throw NetworkException.MatchException.BadRequest()
                401 -> throw NetworkException.MatchException.Unauthorized()
                403 -> throw NetworkException.MatchException.Forbidden()
                404 -> throw NetworkException.MatchException.NotFound()
                408 -> throw NetworkException.MatchException.RequestTimeout()

                429 -> throw NetworkException.MatchException.TooManyRequests()

                500 -> throw NetworkException.MatchException.InternalServerError()
                502 -> throw NetworkException.MatchException.BadGateway()
                503 -> throw NetworkException.MatchException.ServiceUnavailable()
                504 -> throw NetworkException.MatchException.GatewayTimeout()

                else -> {
                    if (e !is CancellationException && e !is NetworkException.MatchException) {
                        log.error("比赛请求：未定义的错误：", e)
                        throw NetworkException.MatchException.Undefined(e)
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MatchApiImpl::class.java)

        /** events 接口允许的最大 limit 是 101，这里用 100 保持常规分页。 */
        private const val ROOM_EVENT_PAGE_SIZE = 100

        /** 最多往前翻 20 页（约 2000 条事件），避免异常房间把请求数撑爆。 */
        private const val MAX_ROOM_EVENT_PAGES = 20
    }
}
