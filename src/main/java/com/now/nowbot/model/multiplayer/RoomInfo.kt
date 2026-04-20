package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.MicroUser
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RoomInfo(
    @field:JsonProperty("id")
    val roomID: Long,

    @field:JsonProperty("name")
    val name: String,

    @field:JsonProperty("category")
    val category: String, // 只有normal?

    @field:JsonProperty("status")
    val status: String, // playing, idle

    @field:JsonProperty("type")
    val type: String, // head_to_head, ranked_play, matchmaking

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
    val queueMode: String, // all_players_round_robin, host_only

    @field:JsonProperty("auto_skip")
    val autoSkip: Boolean,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("current_user_score")
    val currentUserScore: RoomScore?,

    // 只有在获取 room/xxx、user/quickplay 的时候有
    @field:JsonProperty("host")
    val host: MicroUser?,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("playlist")
    val playlist: List<Playlist>?,

    // 只有在获取 room/xxx 的时候有
    @field:JsonProperty("recent_participants")
    val recentParticipants: List<MicroUser>?,

    // 只有在获取 user/quickplay 的时候有
    @field:JsonProperty("pinned")
    val pinned: Boolean?,

    // 只有在获取 user/quickplay 的时候有
    @field:JsonProperty("current_playlist_item")
    val currentPlaylistItem: Playlist?,

    // 只有在获取 user/quickplay 的时候有
    @field:JsonProperty("difficulty_range")
    val difficultyRange: DifficultyRange?,

    // 只有在获取 user/quickplay 的时候有
    @field:JsonProperty("playlist_item_stats")
    val playlistItemStats: Playlist.PlaylistItemStats?,

    ) {
    data class DifficultyRange(
        @field:JsonProperty("max")
        val max: Float,

        @field:JsonProperty("min")
        val min: Float,
    )
}