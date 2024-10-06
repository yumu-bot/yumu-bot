package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.json.Match
import com.now.nowbot.model.multiplayer.MatchQuery
import com.now.nowbot.model.multiplayer.MonitoredMatch
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

@Service
class MatchApiImpl(
    private val base: OsuApiBaseService,
) : OsuMatchApiService {
    @Throws(WebClientResponseException::class)
    override fun queryMatch(limit: Int, sort: String, cursor: String?): MatchQuery {
        return base.osuApiWebClient.get()
            .uri {
                it.path("matches")
                it.queryParam("limit", limit)
                it.queryParam("sort", sort)
                if (cursor != null) it.queryParam("cursor_string", cursor)
                it.build()
            }
            .headers(base::insertHeader)
            .retrieve()
            .bodyToMono(MatchQuery::class.java)
            .block()!!
    }

    override fun getMonitoredMatchInfo(mid: Long, before: Long?, after: Long?, limit: Int) : MonitoredMatch {
        return base.osuApiWebClient.get()
            .uri {
                it.path("matches/{mid}")
                if (before != null) it.queryParam("before", before)
                if (after != null) it.queryParam("after", after)
                it.queryParam("limit", limit)
                it.build(mid)
            }
            .headers(base::insertHeader)
            .retrieve()
            .bodyToMono(MonitoredMatch::class.java)
            .timeout(Duration.ofSeconds(5))
            .block()!!
    }

    private fun getMatchInfo(mid: Long): Match {
        return base.osuApiWebClient.get()
            .uri("matches/{mid}", mid)
            .headers(base::insertHeader)
            .retrieve()
            .bodyToMono(Match::class.java)
            .block()!!
    }

    private fun getMatchInfo(mid: Long, before: Long, after: Long): Match {
        return base.osuApiWebClient.get()
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
            .timeout(Duration.ofSeconds(5))
            .block()!!
    }

    @Throws(WebClientResponseException::class)
    override fun getMatchInfo(mid: Long, limit: Int): Match {
        var l = limit
        var eventId: Long
        val match: Match = getMatchInfo(mid)
        do {
            val newMatch = getMatchInfoBefore(mid, match.events.first().eventID)
            match.parseNextData(newMatch)
            eventId = match.events.first().eventID
        } while (match.firstEventID != eventId && --l >= 0)
        return match
    }

    @Throws(WebClientResponseException::class)
    override fun getMatchInfoFirst(mid: Long): Match {
        return getMatchInfo(mid)
    }

    @Throws(WebClientResponseException::class)
    override fun getMatchInfoBefore(mid: Long, id: Long): Match {
        return getMatchInfo(mid, id, 0)
    }

    @Throws(WebClientResponseException::class)
    override fun getMatchInfoAfter(mid: Long, id: Long): Match {
        return getMatchInfo(mid, 0, id)
    }
}