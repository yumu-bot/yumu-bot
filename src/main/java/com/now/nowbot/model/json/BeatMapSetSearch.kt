package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BeatMapSetSearch {
    @JsonProperty("rule") var rule: String? = null

    @JsonProperty("recommended_difficulty") var recommendedDifficulty: Double? = 0.0

    @JsonProperty("result_count") var resultCount: Int = 0

    @JsonProperty("beatmapsets") var beatmapSets: MutableList<BeatMapSet> = mutableListOf()

    @JsonProperty("total") var total: Int = 0

    @JsonProperty("cursor_string") var cursorString: String? = null

    @JsonProperty("cursor") var cursor: SearchCursor? = null

    @JsonProperty("search") var info: SearchInfo? = null

    companion object {
        fun sortBeatmapDiff(search: BeatMapSetSearch) {
            if (search.beatmapSets.isEmpty()) return

            for (s in search.beatmapSets) {
                if (s.beatMaps.isNullOrEmpty()) return

                s.beatMaps = s.beatMaps!!
                    .stream()
                    .sorted(
                        Comparator.comparing { obj: BeatMap -> obj.getStarRating() }
                    )
                    .sorted(Comparator.comparing { obj: BeatMap -> obj.getModeInt() })
                    .toList()
            }
        }
    }
}
