package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeatmapUserScore {
    @JsonProperty("position")
    Integer position;
    @JsonProperty("score")
    Score score;

    public Integer getPosition() {
        return position;
    }

    public Score getScore() {
        return score;
    }

    @Override
    public String toString() {
        return STR."BeatmapUserScore{position=\{position}, score=\{score}\{'}'}";
    }
}
