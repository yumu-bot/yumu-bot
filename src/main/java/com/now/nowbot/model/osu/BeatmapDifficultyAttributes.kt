package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BeatmapDifficultyAttributes {
    @JsonProperty("max_combo")
    var maxCombo: Int = 0

    @JsonProperty("star_rating")
    var starRating: Double = 0.0

    /**
     * OSU
     */
    @JsonProperty("aim_difficulty")
    var aim: Float? = null

    @JsonProperty("flashlight_difficulty")
    var flashlight: Float? = null

    @JsonProperty("overall_difficulty")
    var overall: Float? = null

    @JsonProperty("speed_difficulty")
    var speed: Float? = null

    @JsonProperty("slider_factor")
    var slider: Float? = null

    /**
     * taiko
     */
    @JsonProperty("stamina_difficulty")
    val stamina: Float? = null

    @JsonProperty("rhythm_difficulty")
    val rhythm: Float? = null

    @JsonProperty("colour_difficulty")
    val colour: Float? = null

    /**
     * mania
     */
    @JsonProperty("score_multiplier")
    val scoreMultiplier: Float? = null

    /**
     * osu & taiko & fruits
     */
    @JsonProperty("approach_rate")
    val approachRate: Float? = null

    /**
     * taiko & mania
     */
    @JsonProperty("great_hit_window")
    val greatHitWindow: Float? = null
}
