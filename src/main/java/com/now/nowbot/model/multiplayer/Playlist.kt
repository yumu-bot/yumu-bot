package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import java.time.OffsetDateTime

/**
 * 等于 MatchRound
 */
data class Playlist(
    @JsonProperty("id")
    val listID: Long,

    @JsonProperty("room_id")
    val roomID: Long,

    @JsonProperty("beatmap_id")
    val beatmapID: Long,

    @JsonProperty("created_at")
    val createdTime: OffsetDateTime,

    @JsonProperty("ruleset_id")
    val rulesetID: Byte,

    @JsonProperty("allowed_mods")
    val allowedMods: List<LazerMod>,

    @JsonProperty("required_mods")
    val requiredMods: List<LazerMod>,

    @JsonProperty("freestyle")
    val freestyle: Boolean,

    @JsonProperty("expired")
    val expired: Boolean,

    @JsonProperty("owner_id")
    val ownerID: Long,

    @JsonProperty("playlist_order")
    val order: Int,

    @JsonProperty("played_at")
    val playedTime: OffsetDateTime,

    // 只有 room/xxx/events 有
    @JsonProperty("details")
    val details: PlaylistDetail?,

    // 只有 room/xxx/events 有
    @JsonProperty("scores")
    val scores: List<LazerScore>?,

    // 只有 room/xxx 有
    @JsonProperty("beatmap")
    val beatmap: Beatmap?,

    ) {

    data class PlaylistDetail(
        @JsonProperty("teams")
        val teams: String?,

        @JsonProperty("room_type")
        val roomType: String,

        @JsonProperty("started_at")
        val startedTime: OffsetDateTime,
    )

}
