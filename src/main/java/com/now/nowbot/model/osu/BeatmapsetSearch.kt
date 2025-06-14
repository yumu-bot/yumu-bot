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

    @JsonProperty("result_count") var resultCount: Int = 0

    @JsonProperty("beatmapsets") var beatmapSets: List<Beatmapset> = listOf()

    @JsonProperty("total") var total: Int = 0

    @JsonProperty("cursor_string") var cursorString: String? = null

    @JsonProperty("cursor") var cursor: SearchCursor? = null

    @JsonProperty("search") var info: SearchInfo? = null

    fun sortBeatmapDiff() {
        if (this.beatmapSets.isEmpty()) return

        for (s in this.beatmapSets) {
            if (s.beatmaps.isNullOrEmpty()) return

            s.beatmaps = s.beatmaps!!.sortedBy { it.starRating }.sortedBy { it.modeInt ?: 0 }
        }
    }
}
