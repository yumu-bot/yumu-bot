package com.now.nowbot.model.beatmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.BeatMapSet;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatmapInfo4Match {

    @JsonProperty("id")
    Integer BID;
    @JsonProperty("beatmapset_id")
    Integer SID;
    @JsonProperty("difficulty_rating")
    Double starRating;
    String mode;
    String status;
    @JsonProperty("total_length")
    Integer totalLength;
    @JsonProperty("user_id")
    Integer hostUID;
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
        return BID;
    }

    public void setBID(Integer bid) {
        this.BID = bid;
    }

    public Integer getSID() {
        return SID;
    }

    public void setSID(Integer sid) {
        this.SID = sid;
    }

    public Double getStarRating() {
        return starRating;
    }

    public void setStarRating(Double starRating) {
        this.starRating = starRating;
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
        return hostUID;
    }

    public void setHostUID(Integer hostUID) {
        this.hostUID = hostUID;
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
