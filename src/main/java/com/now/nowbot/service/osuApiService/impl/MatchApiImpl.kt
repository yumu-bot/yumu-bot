package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.Match.Companion.append
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import io.netty.channel.unix.Errors
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

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
                .retrieve()
                .bodyToMono(MatchLobby::class.java)
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
                .uri("rooms/${roomID}/events", )
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Room::class.java)
        }
    }

    override fun getRoomInfo(roomID: Long): RoomInfo {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}")
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(RoomInfo::class.java)
        }
    }

    override fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard {
        return request { client ->
            client.get()
                .uri("rooms/${roomID}/leaderboard")
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(RoomLeaderBoard::class.java)
        }
    }

    private fun getMatchFromAPI(matchID: Long): Match {
        return request { client ->
            client.get()
                .uri("matches/${matchID}")
                .headers(base::insertHeader)
                .retrieve()
            .bodyToMono(Match::class.java)
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
                .retrieve()
                .bodyToMono(Match::class.java)
        }
    }

    /**
     * 错误包装
     */
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(request)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<WebClientException>()

            when (ex) {
                is WebClientResponseException.BadRequest -> {
                    throw NetworkException.MatchException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    throw NetworkException.MatchException.Unauthorized()
                }

                is WebClientResponseException.Forbidden -> {
                    throw NetworkException.MatchException.Forbidden()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.MatchException.NotFound()
                }

                is WebClientResponseException.TooManyRequests -> {
                    throw NetworkException.MatchException.TooManyRequests()
                }

                is WebClientResponseException.InternalServerError -> {
                    throw NetworkException.MatchException.InternalServerError()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.MatchException.BadGateway()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.MatchException.ServiceUnavailable()
                }

                else -> if (e.findCauseOfType<Errors.NativeIoException>() != null) {
                    throw NetworkException.MatchException.GatewayTimeout()
                } else if (e.findCauseOfType<ReadTimeoutException>() != null) {
                    throw NetworkException.MatchException.RequestTimeout()
                } else {
                    throw NetworkException.MatchException.Undefined(e)
                }
            }
        }
    }
}