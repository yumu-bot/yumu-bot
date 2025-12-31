package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomInfo(
    @field:JsonProperty("id")
    val roomID: Long,

    @field:JsonProperty("name")
    val name: String,

    @field:JsonProperty("category")
    val category: String, // normal，看起来可以放月赛或每日挑战之类的东西

    @field:JsonProperty("status")
    val status: String, // playing

    @field:JsonProperty("type")
    val type: String, // head_to_head

    @field:JsonProperty("user_id")
    val userID: Long,

    @field:JsonProperty("starts_at")
    val startedTime: OffsetDateTime,

    @field:JsonProperty("ends_at")
    val endedTime: OffsetDateTime?,

    @field:JsonProperty("max_attempts")
    val maxAttempts: Int?, // 应该是无上限

    @field:JsonProperty("participant_count")
    val participantCount: Int,

    @field:JsonProperty("channel_id")
    val channelID: Long,

    @field:JsonProperty("active")
    val active: Boolean,

    @field:JsonProperty("has_password")
    val hasPassword: Boolean,

    @field:JsonProperty("queue_mode")
    val queueMode: String, // all_players_round_robin

    @field:JsonProperty("auto_skip")
    val autoSkip: Boolean,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("current_user_score")
    val currentUserScore: RoomScore?,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("host")
    val host: MicroUser?,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("playlist")
    val playlist: List<Playlist>?,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("recent_participants")
    val recentParticipants: List<MicroUser>?
    )