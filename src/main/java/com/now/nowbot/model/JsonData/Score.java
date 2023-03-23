package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Score {
    static final DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
//    @JsonProperty("statistics")
    Double accuracy;
    @JsonProperty("best_id")
    Long bestId;
    @JsonProperty("max_combo")
    Integer maxCombo;
    @JsonProperty("user_id")
    Long userId;
    @JsonProperty("created_at")
    String createTime;
    @JsonProperty("id")
    Long scoreId;
    @JsonIgnoreProperties
    OsuMode mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    List<String> mods;
    Boolean passed;
    Boolean perfect;
    Float pp;
    String rank;
    Boolean replay;
    Integer score;
    Statistics statistics;

    @JsonProperty("beatmap")
    BeatMap beatMap;
    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    MicroUser user;
    /***
     * 仅查询bp时存在
     */
    Weight weight;

    @JsonProperty("mode")
    public void setMode(String mode){
        this.mode = OsuMode.getMode(mode);
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Long getBestId() {
        return bestId;
    }

    public void setBestId(Long bestId) {
        this.bestId = bestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreateTime() {
        if (createTime != null) return LocalDateTime.parse(createTime, formatter);
        return LocalDateTime.now();
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Long getScoreId() {
        return scoreId;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    public Integer getModeInt() {
        return modeInt;
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
    }

    public List<String> getMods() {
        return mods;
    }

    public void setMods(List<String> mods) {
        this.mods = mods;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getPerfect() {
        return perfect;
    }

    public void setPerfect(Boolean perfect) {
        this.perfect = perfect;
    }

    public Float getPP() {
        if (pp == null) return 0f;
        return pp;
    }

    public void setPP(Float pp) {
        this.pp = pp;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Boolean getReplay() {
        return replay;
    }

    public void setReplay(Boolean replay) {
        this.replay = replay;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public BeatMap getBeatMap() {
        return beatMap;
    }

    public void setBeatMap(BeatMap beatMap) {
        this.beatMap = beatMap;
    }

    public BeatMapSet getBeatMapSet() {
        return beatMapSet;
    }

    public void setBeatMapSet(BeatMapSet beatMapSet) {
        this.beatMapSet = beatMapSet;
    }

    public MicroUser getUser() {
        return user;
    }

    public void setUser(MicroUser user) {
        this.user = user;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Float getPp() {
        return pp;
    }

    public void setPp(Float pp) {
        this.pp = pp;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Boolean isPerfect() {
        return perfect;
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

        public Float getPP() {
            return pp;
        }

        public void setPP(Float pp) {
            this.pp = pp;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Weight{");
            sb.append("percentage=").append(percentage);
            sb.append(", pp=").append(pp);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Score{");
        sb.append("accuracy=").append(accuracy);
        sb.append(", bestId=").append(bestId);
        sb.append(", maxCombo=").append(maxCombo);
        sb.append(", userId=").append(userId);
        sb.append(", createTime='").append(createTime).append('\'');
        sb.append(", scoreId=").append(scoreId);
        sb.append(", mode=").append(mode);
        sb.append(", modeInt=").append(modeInt);
        sb.append(", mods=").append(mods);
        sb.append(", passed=").append(passed);
        sb.append(", perfect=").append(perfect);
        sb.append(", pp=").append(pp);
        sb.append(", rank='").append(rank).append('\'');
        sb.append(", replay=").append(replay);
        sb.append(", score=").append(score);
        sb.append(", statistics=").append(statistics);
        sb.append(", beatMap=").append(beatMap);
        sb.append(", beatMapSet=").append(beatMapSet);
        sb.append(", user=").append(user);
        sb.append('}');
        return sb.toString();
    }
}
