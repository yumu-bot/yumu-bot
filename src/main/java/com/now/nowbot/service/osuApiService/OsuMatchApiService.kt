package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.multiplayer.MatchLobby
import com.now.nowbot.model.multiplayer.Match
import org.springframework.web.reactive.function.client.WebClientResponseException

interface OsuMatchApiService {
    /**
     * @param descending false 旧的在前, true 新的在前
     */
    @Throws(WebClientResponseException::class)
    fun getMatchLobby(limit: Int = 50, descending: Boolean = true, cursor: String? = null): MatchLobby

    fun getMatch(matchID: Long, times: Int = 10): Match

    fun getMatchBefore(matchID: Long, eventID: Long): Match

    fun getMatchAfter(matchID: Long, eventID: Long): Match
}