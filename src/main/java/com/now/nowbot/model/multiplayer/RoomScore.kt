package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.MicroUser

data class RoomScore(
    @field:JsonProperty("accuracy")
    val accuracy: Double,

    @field:JsonProperty("attempts")
    val attempts: Int,

    @field:JsonProperty("completed")
    val completed: Int,

    @field:JsonProperty("pp")
    val pp: Double,

    @field:JsonProperty("room_id")
    val roomID: Long,

    @field:JsonProperty("total_score")
    val totalScore: Long,

    @field:JsonProperty("user_id")
    val userID: Long,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("playlist_item_attempts")
    val itemAttempts: List<Playlist>?,

    // 只有在获取 room/xxx/leaderboard 的时候有
    @field:JsonProperty("user")
    val user: MicroUser?,

    )