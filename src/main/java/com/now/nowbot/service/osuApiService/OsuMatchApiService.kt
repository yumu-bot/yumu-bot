package com.now.nowbot.service.osuApiService

import RoomInfo
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
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

    fun getRoom(roomID: Long): Room

    fun getRoomInfo(roomID: Long): RoomInfo

    fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard
}