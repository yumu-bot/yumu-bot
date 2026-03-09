package com.now.nowbot.model.osu


import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class BeatmapsetWithRankTime(
    @field:JsonProperty("id")
    val beatmapID: Long = 0,

    @field:JsonProperty("rank_date")
    val rankDate: String = "",

    @field:JsonProperty("rank_date_early")
    val rankDateEarly: String = "",

    @field:JsonProperty("artist")
    val artist: String = "",

    @field:JsonProperty("title")
    val title: String = "",

    @field:JsonProperty("beatmaps")
    val beatmaps: List<BeatmapWithRankTime> = listOf(),

    @field:JsonProperty("rank_early")
    val isEarly: Boolean = false,
) {
    data class BeatmapWithRankTime(
        @field:JsonProperty("id")
        val beatmapID: Long = 0,

        @field:JsonProperty("ver")
        val difficultyName: String = "",

        @field:JsonProperty("spin")
        val spinner: Int = 0,

        @field:JsonProperty("sr")
        val starRating: Float = 0f,

        @field:JsonProperty("len")
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