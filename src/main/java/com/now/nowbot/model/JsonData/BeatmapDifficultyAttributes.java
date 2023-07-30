package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeatmapDifficultyAttributes {
    @JsonProperty("max_combo")
    Integer maxCombo;
    @JsonProperty("star_rating")
    Float starRating;
    /**
     * OSU
     */

    @JsonProperty("aim_difficulty")
    Float aim;
    @JsonProperty("flashlight_difficulty")
    Float flashlight;
    @JsonProperty("overall_difficulty")
    Float overall;
    @JsonProperty("speed_difficulty")
    Float speed;
    @JsonProperty("slider_factor")
    Float slider;

    /**
     * taiko
     */
    @JsonProperty("stamina_difficulty")
    Float stamina;
    @JsonProperty("rhythm_difficulty")
    Float rhythm;
    @JsonProperty("colour_difficulty")
    Float colour;

    /**
     * mania
     */
    @JsonProperty("score_multiplier")
    Float scoreMultiplier;

    /**
     * osu & taiko & fruits
     */
    @JsonProperty("approach_rate")
    Float approachRate;


    /**
     * tokio & mania
     */
    @JsonProperty("great_hit_window")
    Float great_hit_window;


    public Integer getMaxCombo() {
        return maxCombo;
    }

    public Float getStarRating() {
        return starRating;
    }

    public Float getAim() {
        return aim;
    }

    public Float getFlashlight() {
        return flashlight;
    }

    public Float getOverall() {
        return overall;
    }

    public Float getSpeed() {
        return speed;
    }

    public Float getSlider() {
        return slider;
    }

    public Float getStamina() {
        return stamina;
    }

    public Float getRhythm() {
        return rhythm;
    }

    public Float getColour() {
        return colour;
    }

    public Float getScoreMultiplier() {
        return scoreMultiplier;
    }

    public Float getApproachRate() {
        return approachRate;
    }

    public Float getGreat_hit_window() {
        return great_hit_window;
    }
}
