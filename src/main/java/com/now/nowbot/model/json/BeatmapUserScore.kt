package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BeatmapUserScore {
    @JvmField @JsonProperty("position") var position: Int? = null

    @JvmField @JsonProperty("score") var score: LazerScore? = null

    override fun toString(): String {
        return "BeatmapUserScore{position=${position}, score=${score}${'}'}"
    }
}
