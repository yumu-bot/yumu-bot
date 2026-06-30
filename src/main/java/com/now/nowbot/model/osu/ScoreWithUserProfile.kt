package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.now.nowbot.entity.UserProfileLite
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonNaming(
    PropertyNamingStrategies.SnakeCaseStrategy::class
)

data class ScoreWithUserProfile(
    @field:JsonUnwrapped
    @get:JsonUnwrapped
    val score: LazerScore,

    @JvmField
    @field:JsonProperty("profile")
    var profile: UserProfileLite? = null
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
