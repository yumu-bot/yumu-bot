package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BpInfo {
    @JsonProperty("id")
    Long id;
    @JsonProperty("user_id")
    Integer userId;
    @JsonProperty("accuracy")
    Float accuracy;
    @JsonProperty("mods")
    List<String> mods;
    @JsonProperty("score")
    Integer score;
    @JsonProperty("max_combo")
    Integer maxCombo;
    @JsonProperty("passed")
    Boolean passed;
    @JsonProperty("perfect")
    Boolean perfect;
    @JsonProperty("rank")
    String rank;
    @JsonProperty("created_at")
    String createTime;
    @JsonProperty("best_id")
    Long bestId;
    @JsonProperty("pp")
    Float pp;
    @JsonProperty("mode")
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    @JsonProperty("replay")
    Boolean replay;
    @JsonProperty("beatmap")
    BeatMap beatmap;
    @JsonProperty("beatmapset")
    BeatMapSet beatmapset;
    @JsonProperty("weight")
    Weight weight;
    @JsonProperty("statistics")
    Statustucs statistics;

    @Override
    public String toString() {
        return "BpInfo{" +
                "id=" + id +
                ", userId=" + userId +
                ", accuracy=" + accuracy +
                ", mods=" + mods +
                ", score=" + score +
                ", maxCombo=" + maxCombo +
                ", passed=" + passed +
                ", perfect=" + perfect +
                ", rank='" + rank + '\'' +
                ", createTime='" + createTime + '\'' +
                ", bestId=" + bestId +
                ", pp=" + pp +
                ", mode='" + mode + '\'' +
                ", modeInt=" + modeInt +
                ", replay=" + replay +
                ", \nbeatmap=" + beatmap +
                ", \nbeatmapset=" + beatmapset +
                ", \nweight=" + weight +
                ", \nstatistics=" + statistics +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Weight {
        @JsonProperty("percentage")
        Float percentage;
        @JsonProperty("pp")
        Float pp;

        public Float getPercentage() {
            return percentage;
        }

        public void setPercentage(Float percentage) {
            this.percentage = percentage;
        }

        public Float getPp() {
            return pp;
        }

        public void setPp(Float pp) {
            this.pp = pp;
        }

        @Override
        public String toString() {
            return "Weight{" +
                    "percentage=" + percentage +
                    ", pp=" + pp +
                    '}';
        }
    }
}

