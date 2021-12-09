package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BpInfo {
    Long id;
    @JsonProperty("user_id")
    Integer userId;
    Float accuracy;
    List<String> mods;
    @JsonProperty("score")
    Integer score;
    @JsonProperty("max_combo")
    Integer maxCombo;
    Boolean passed;
    Boolean perfect;
    String rank;
    @JsonProperty("created_at")
    String createTime;
    @JsonProperty("best_id")
    Long bestId;
    Float pp;
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    Boolean replay;
    BeatMap beatmap;
    BeatMapSet beatmapset;
    Weight weight;
    Statustucs statistics;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public List<String> getMods() {
        return mods;
    }

    public void setMods(List<String> mods) {
        this.mods = mods;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Boolean isPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean isPerfect() {
        return perfect;
    }

    public void setPerfect(Boolean perfect) {
        this.perfect = perfect;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Long getBestId() {
        return bestId;
    }

    public void setBestId(Long bestId) {
        this.bestId = bestId;
    }

    public Float getPp() {
        return pp;
    }

    public void setPp(Float pp) {
        this.pp = pp;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getModeInt() {
        return modeInt;
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
    }

    public Boolean getReplay() {
        return replay;
    }

    public void setReplay(Boolean replay) {
        this.replay = replay;
    }

    public BeatMap getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(BeatMap beatmap) {
        this.beatmap = beatmap;
    }

    public BeatMapSet getBeatmapset() {
        return beatmapset;
    }

    public void setBeatmapset(BeatMapSet beatmapset) {
        this.beatmapset = beatmapset;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Statustucs getStatistics() {
        return statistics;
    }

    public void setStatistics(Statustucs statistics) {
        this.statistics = statistics;
    }

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

