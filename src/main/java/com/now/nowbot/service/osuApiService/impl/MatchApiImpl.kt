package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.toBody
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

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
                .toBody<MatchLobby>()
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

    override fun getRoom(roomID: Long): Room {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}/events")
                .headers(base::insertHeader)
                .toBody<Room>()
        }
    }

    override fun getRoomInfo(roomID: Long): RoomInfo {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}")
                .headers(base::insertHeader)
                .toBody<RoomInfo>()
        }
    }

    override fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}/leaderboard")
                .headers(base::insertHeader)
                .toBody<RoomLeaderBoard>()
        }
    }

    private fun getMatchFromAPI(matchID: Long): Match {
        return request { client ->
            client.get()
                .uri("matches/${matchID}")
                .headers(base::insertHeader)
                .toBody<Match>()
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
                .toBody<Match>()
        }
    }

    /**
     * 错误包装
     */
    private fun <T : Any> request(isBackground: Boolean = false, request: (RestClient) -> T): T {
        return try {
            base.request(isBackground, request)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<RestClientResponseException>()

            when {
                ex == null -> {
                    throw NetworkException.MatchException.Undefined(e)
                }

                ex.statusCode == org.springframework.http.HttpStatus.BAD_REQUEST -> {
                    throw NetworkException.MatchException.BadRequest()
                }

                ex.statusCode == org.springframework.http.HttpStatus.UNAUTHORIZED -> {
                    throw NetworkException.MatchException.Unauthorized()
                }

                ex.statusCode == org.springframework.http.HttpStatus.FORBIDDEN -> {
                    throw NetworkException.MatchException.Forbidden()
                }

                ex.statusCode == org.springframework.http.HttpStatus.NOT_FOUND -> {
                    throw NetworkException.MatchException.NotFound()
                }

                ex.statusCode == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS -> {
                    throw NetworkException.MatchException.TooManyRequests()
                }

                ex.statusCode == org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR -> {
                    throw NetworkException.MatchException.InternalServerError()
                }

                ex.statusCode == org.springframework.http.HttpStatus.BAD_GATEWAY -> {
                    throw NetworkException.MatchException.BadGateway()
                }

                ex.statusCode == org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE -> {
                    throw NetworkException.MatchException.ServiceUnavailable()
                }

                e.findCauseOfType<java.net.SocketException>() != null -> {
                    throw NetworkException.MatchException.GatewayTimeout()
                }

                else -> {
                    throw NetworkException.MatchException.Undefined(e)
                }
            }
        }
    }
}