package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode

data class BeatmapsetWithRankTime(
    @JsonProperty("id")
    val beatmapID: Long = 0,

    @JsonProperty("rank_date")
    val rankDate: String = "",

    @JsonProperty("rank_date_early")
    val rankDateEarly: String = "",

    @JsonProperty("artist")
    val artist: String = "",

    @JsonProperty("title")
    val title: String = "",

    @JsonProperty("beatmaps")
    val beatmaps: List<BeatmapWithRankTime> = listOf(),

    @JsonProperty("rank_early")
    val isEarly: Boolean = false,
) {
    data class BeatmapWithRankTime(
        @JsonProperty("id")
        val beatmapID: Long = 0,

        @JsonProperty("ver")
        val difficultyName: String = "",

        @JsonProperty("spin")
        val spinner: Int = 0,

        @JsonProperty("sr")
        val starRating: Float = 0f,

        @JsonProperty("len")
        val length: Int = 0,

        @set:JsonProperty("mode")
        @get:JsonIgnore
        var modeInt: Int = 0,
    ) {
        @get:JsonProperty("mode")
        val mode: OsuMode
            get() = OsuMode.getMode(modeInt)
    }

    override fun hashCode(): Int {
        return beatmapID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeatmapsetWithRankTime

        return beatmapID == other.beatmapID
    }
}