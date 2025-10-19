package com.now.nowbot.service.osuApiService.impl

import RoomInfo
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.Match.Companion.append
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
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
        var l = times
        var eventId: Long
        val match: Match = getMatch(matchID)
        do {
            val newMatch = getMatchBefore(matchID, match.events.first().eventID)
            match.append(newMatch)
            eventId = match.events.first().eventID
        } while (match.firstEventID != eventId && --l >= 0)
        return match
    }

    override fun getMatchBefore(matchID: Long, eventID: Long): Match {
        return getMatch(matchID, eventID, 0)
    }

    override fun getMatchAfter(matchID: Long, eventID: Long): Match {
        return getMatch(matchID, 0, eventID)
    }

    override fun getRoom(roomID: Long): Room {
        return request { client ->
            client.get()
                .uri("rooms/{room}/events", roomID)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Room::class.java)
        }
    }

    override fun getRoomInfo(roomID: Long): RoomInfo {
        return request { client ->
            client.get()
                .uri("rooms/{room}", roomID)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(RoomInfo::class.java)
        }
    }

    override fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard {
        return request { client ->
            client.get()
                .uri("rooms/{room}/leaderboard", roomID)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(RoomLeaderBoard::class.java)
        }
    }

    private fun getMatch(mid: Long): Match {
        return request { client ->
            client.get()
                .uri("matches/{mid}", mid)
                .headers(base::insertHeader)
                .retrieve()
            .bodyToMono(Match::class.java)
            /*
            .bodyToMono(JsonNode::class.java)
            .map {
                JacksonUtil.parseObject(it, Match::class.java)
            }

             */
        }
    }

    private fun getMatch(mid: Long, before: Long, after: Long): Match {
        return request { client ->
            client.get()
                .uri {
                    it.path("matches/{mid}")
                    if (before != 0L) it.queryParam("before", before)
                    if (after != 0L) it.queryParam("after", after)
                    it.queryParam("limit", 100)
                    it.build(mid)
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

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.MatchException.BadGateway()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.MatchException.ServiceUnavailable()
                }

                else -> throw NetworkException.MatchException(e.message)
            }
        }
    }
}