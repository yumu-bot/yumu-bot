package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BeatmapsetSearch {
    @JsonProperty("rule") var rule: String? = null

    @JsonProperty("recommended_difficulty") var recommendedDifficulty: Double? = 0.0

    @get:JsonProperty("result_count") val resultCount: Int
        get() = this.beatmapsets.size

    @JsonProperty("beatmapsets") var beatmapsets: List<Beatmapset> = listOf()

    @JsonProperty("total") var total: Int = 0

    @JsonProperty("cursor_string") var cursorString: String? = null

    @JsonProperty("cursor") var cursor: SearchCursor? = null

    @JsonProperty("search") var info: SearchInfo? = null

    fun sortBeatmapDiff() {
        if (this.beatmapsets.isEmpty()) return

        for (s in this.beatmapsets) {
            if (s.beatmaps.isNullOrEmpty()) continue

            s.beatmaps = s.beatmaps!!.sortedBy { it.starRating }.sortedBy { it.modeInt ?: 0 }
        }
    }

    fun combine(search: BeatmapsetSearch): BeatmapsetSearch {
        search.rule?.let { this.rule = it }
        search.recommendedDifficulty?.let { this.recommendedDifficulty = it }

        this.beatmapsets += search.beatmapsets

        this.total = search.total
        this.cursor = search.cursor
        this.cursorString = search.cursorString
        this.info = search.info

        return this
    }
}
