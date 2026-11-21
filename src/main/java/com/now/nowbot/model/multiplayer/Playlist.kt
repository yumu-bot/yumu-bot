package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

/**
 * 等于 MatchRound
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Playlist(
    @field:JsonProperty("id")
    val listID: Long,

    @field:JsonProperty("room_id")
    val roomID: Long,

    @field:JsonProperty("beatmap_id")
    val beatmapID: Long,

    @field:JsonProperty("created_at")
    val createdTime: OffsetDateTime,

    @field:JsonProperty("ruleset_id")
    val rulesetID: Byte,

    @field:JsonProperty("allowed_mods")
    val allowedMods: List<LazerMod>,

    @field:JsonProperty("required_mods")
    val requiredMods: List<LazerMod>,

    @field:JsonProperty("freestyle")
    val freestyle: Boolean,

    @field:JsonProperty("expired")
    val expired: Boolean,

    @field:JsonProperty("owner_id")
    val ownerID: Long,

    @field:JsonProperty("playlist_order")
    val order: Int?,

    @field:JsonProperty("played_at")
    val playedTime: OffsetDateTime?,

    // 只有 room/xxx/events 有
    @field:JsonProperty("details")
    val details: PlaylistDetail?,

    // 只有 room/xxx/events 有
    @field:JsonProperty("scores")
    val scores: List<LazerScore>?,

    // 只有 room/xxx 有
    @field:JsonProperty("beatmap")
    val beatmap: Beatmap?,

    ) {

    data class PlaylistDetail(
        /**
         * 用户 ID 到红/蓝队伍的映射，只有 team_versus 房间才有。
         *
         * room/xxx/events 返回的是对象（`{"123": "red"}`），不是字符串。
         */
        @field:JsonProperty("teams")
        val teams: Map<Long, String>? = null,

        /**
         * 用户 ID 到房间槽位的映射，只有限制参与人数的房间才有。
         */
        @field:JsonProperty("slots")
        val slots: Map<Long, Int>? = null,

        @field:JsonProperty("room_type")
        val roomType: String,

        @field:JsonProperty("started_at")
        val startedTime: OffsetDateTime,
    )

    data class PlaylistItemStats(
        @field:JsonProperty("count_active")
        val active: Int,

        @field:JsonProperty("count_total")
        val total: Int,

        @field:JsonProperty("ruleset_ids")
        val rulesets: List<Int>,
    )

}
