package com.now.nowbot.model.beatmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.BeatMapSet;

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

    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    @JsonProperty("hit_length")
    Integer hitLength;

    Boolean convert;
    @JsonProperty("is_scoreable")
    Boolean scoreable;

    String checksum;

    public Integer getBID() {
        return id;
    }

    public void setBID(Integer bid) {
        this.id = bid;
    }

    public Integer getSID() {
        return beatmapsetId;
    }

    public void setSID(Integer sid) {
        this.beatmapsetId = sid;
    }

    public Double getStarRating() {
        return difficultyRating;
    }

    public void setStarRating(Double starRating) {
        this.difficultyRating = starRating;
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

    public Integer getHostUID() {
        return userId;
    }

    public void setHostUID(Integer hostUID) {
        this.userId = hostUID;
    }

    public BeatMapSet getBeatMapSet() {
        return beatMapSet;
    }

    public void setBeatMapSet(BeatMapSet beatMapSet) {
        this.beatMapSet = beatMapSet;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
