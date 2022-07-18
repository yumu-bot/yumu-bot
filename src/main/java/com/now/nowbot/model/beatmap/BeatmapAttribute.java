package com.now.nowbot.model.beatmap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeatmapAttribute {
    Long id;

    @JsonProperty("star_rating")
    double StarRating;

    @JsonProperty("max_combo")
    Integer MaxCombo;

    @JsonProperty("aim_difficulty")
    double AimStarRating;

    @JsonProperty("speed_difficulty")
    double SpeedStarRating;

    @JsonProperty("flashlight_difficulty")
    double FlashlightStarRating;

    @JsonProperty("approach_rate")
    double AR;

    @JsonProperty("overall_difficulty")
    double OD;
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getBeatmapStarRating() {
        return StarRating;
    }

    public void setBeatmapStarRating(double StarRating) {
        this.StarRating = StarRating;
    }
    
    public Integer getBeatmapMaxCombo() {
        return MaxCombo;
    }

    public void setBeatmapMaxCombo(Integer MaxCombo) {
        this.MaxCombo = MaxCombo;
    }

    public double getBeatmapAimStarRating() {
        return AimStarRating;
    }

    public void setBeatmapAimStarRating(double AimStarRating) {
        this.AimStarRating = AimStarRating;
    }

    public double getBeatmapSpeedStarRating() {
        return SpeedStarRating;
    }

    public void setBeatmapSpeedStarRating(double SpeedStarRating) {
        this.SpeedStarRating = SpeedStarRating;
    }

    public double getBeatmapFlashlightStarRating() {
        return FlashlightStarRating;
    }

    public void setBeatmapFlashlightStarRating(double FlashlightStarRating) {
        this.FlashlightStarRating = FlashlightStarRating;
    }

    public double getBeatmapAR() {
        return AR;
    }

    public void setBeatmapAR(double AR) {
        this.AR = AR;
    }

    public double getBeatmapOD() {
        return OD;
    }

    public void setBeatmapOD(double OD) {
        this.OD = OD;
    }
}
