package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchLobby
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.multiplayer.RoomLeaderBoard
import org.springframework.web.client.HttpClientErrorException

interface OsuMatchApiService {
    /**
     * @param descending false 旧的在前, true 新的在前
     */
    @Throws(HttpClientErrorException::class)
    fun getMatchLobby(limit: Int = 50, descending: Boolean = true, cursor: String? = null): MatchLobby

    fun getMatch(matchID: Long, times: Int = 10): Match

    fun getMatchBefore(matchID: Long, eventID: Long): Match

    fun getMatchAfter(matchID: Long, eventID: Long): Match

    /**
     * 获取房间数据。
     *
     * [fetchAllEvents] 为 true 时会把 `before` 翻页全部走完。`room/xxx/events` 默认只返回最近 100 条事件，
     * 长时间的房间会把开头的对局挤出去，算木斗力时必须取全。
     */
    fun getRoom(roomID: Long, fetchAllEvents: Boolean = false): Room

    fun getRoomInfo(roomID: Long): RoomInfo

    fun getRoomLeaderboard(roomID: Long): RoomLeaderBoard

    fun getRooms(infos: List<RoomInfo>, batch: Int = 30): List<Room>
}