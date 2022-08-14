package com.now.nowbot.model.beatmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatmapInfo4Match {

    Integer id;
    @JsonProperty("beatmapset_id")
    Integer beatmapsetId;
    @JsonProperty("difficulty_rating")
    Double difficultyRating;
    String mode;
    String status;
    @JsonProperty("total_length")
    Integer totalLength;
    @JsonProperty("user_id")
    Integer userId;
    String version;

    @JsonProperty("accuracy")
    Float od;
    Float ar;
    Float cs;
    @JsonProperty("drain")

    Float hp;
    Float bpm;

    @JsonProperty("hit_length")
    Integer hitLength;

    Boolean convert;
    @JsonProperty("is_scoreable")
    Boolean scoreable;

    String checksum;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBeatmapsetId() {
        return beatmapsetId;
    }

    public void setBeatmapsetId(Integer beatmapsetId) {
        this.beatmapsetId = beatmapsetId;
    }

    public Double getDifficultyRating() {
        return difficultyRating;
    }

    public void setDifficultyRating(Double difficultyRating) {
        this.difficultyRating = difficultyRating;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(Integer totalLength) {
        this.totalLength = totalLength;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
