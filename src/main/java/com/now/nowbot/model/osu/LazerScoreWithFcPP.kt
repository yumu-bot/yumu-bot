package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class LazerScoreWithFcPP(
    @field:JsonUnwrapped
    @get:JsonUnwrapped
    val score: LazerScore,

    @field:JsonProperty("fc_pp")
    var fcPP: Double = 0.0,

    @field:JsonProperty("index")
    var index: Int = 0,

    @field:JsonProperty("index_after")
    var indexAfter: Int = 0
) {
    override fun hashCode(): Int {
        return score.scoreID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val otherID = when (other) {
            is LazerScore -> other.scoreID
            is LazerScoreWithFcPP -> other.score.scoreID
            is ScoreWithUserProfile -> other.score.scoreID
            else -> return false
        }

        return this.score.scoreID == otherID
    }
}
