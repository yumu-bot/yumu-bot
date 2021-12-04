package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMap {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("beatmapset_id")
    Integer beatmapsetId;
    @JsonProperty("difficulty_rating")
    Integer difficultyRating;
    @JsonProperty("mode")
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    @JsonProperty("status")
    String status;
    @JsonProperty("total_length")
    Integer totalLength;
    @JsonProperty("hit_length")
    Boolean hitLength;
    @JsonProperty("user_id")
    String userId;
    @JsonProperty("version")
    String version;
    @JsonProperty("accuracy")
    Float od;
    @JsonProperty("ar")
    Float ar;
    @JsonProperty("cs")
    Float cs;
    @JsonProperty("drain")
    Float hp;
    @JsonProperty("bpm")
    Float bpm;
    @JsonProperty("convert")
    Boolean convert;
    @JsonProperty("is_scoreable")
    Boolean scoreable;
    @JsonProperty("last_updated")
    String updatedTime;
    @JsonProperty("passcount")
    String passcount;
    @JsonProperty("playcount")
    String playcount;
    @JsonProperty("url")
    String url;
    @JsonProperty("checksum")
    String md5;

    @Override
    public String toString() {
        return "BeatMap{" +
                "id=" + id +
                ", beatmapsetId=" + beatmapsetId +
                ", difficultyRating=" + difficultyRating +
                ", mode='" + mode + '\'' +
                ", modeInt=" + modeInt +
                ", status='" + status + '\'' +
                ", totalLength=" + totalLength +
                ", hitLength=" + hitLength +
                ", userId='" + userId + '\'' +
                ", version='" + version + '\'' +
                ", od=" + od +
                ", ar=" + ar +
                ", cs=" + cs +
                ", hp=" + hp +
                ", bpm=" + bpm +
                ", convert=" + convert +
                ", scoreable=" + scoreable +
                ", updatedTime='" + updatedTime + '\'' +
                ", passcount='" + passcount + '\'' +
                ", playcount='" + playcount + '\'' +
                ", url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
