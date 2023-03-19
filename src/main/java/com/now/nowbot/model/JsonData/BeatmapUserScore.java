package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        final StringBuilder sb = new StringBuilder("BeatmapUserScore{");
        sb.append("position=").append(position);
        sb.append(", score=").append(score);
        sb.append('}');
        return sb.toString();
    }
}
