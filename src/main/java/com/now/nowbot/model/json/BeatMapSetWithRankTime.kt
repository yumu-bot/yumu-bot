package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode

class BeatMapSetWithRankTime {

    @JsonProperty("id")
    val beatMapID: Long = 0

    @JsonProperty("rank_date")
    val rankDate: String = ""

    @JsonProperty("rank_date_early")
    val rankDateEarly: String = ""

    @JsonProperty("artist")
    val artist: String = ""

    @JsonProperty("title")
    val title: String = ""

    @JsonProperty("beatmaps")
    val beatMaps: List<BeatMapWithRankTime> = listOf()

    @JsonProperty("rank_early")
    val isEarly: Boolean = false

    class BeatMapWithRankTime {
        @JsonProperty("id")
        val beatMapID: Long = 0

        @JsonProperty("ver")
        val difficultyName: String = ""

        @JsonProperty("spin")
        val spinner: Int = 0

        @JsonProperty("sr")
        val starRating: Float = 0f

        @JsonProperty("len")
        val length: Int = 0

        @set:JsonProperty("mode")
        @get:JsonIgnore
        var modeInt: Int = 0

        @get:JsonProperty("mode")
        val mode: OsuMode
            get() = OsuMode.getMode(modeInt)
    }

}