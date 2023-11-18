package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchScore {

    Double accuracy;

    @JsonProperty("best_id")
    Long bestId;
    OffsetDateTime timestamp;

    @JsonProperty("id")
    Long scoreId;
    @JsonProperty("max_combo")
    Integer maxCombo;
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    String[] mods;
    Boolean passed;
    Boolean perfect;
    Double pp;
    String rank;
    Boolean replay;
    Integer score;
    Statistics statistics;
    String type;
    @JsonProperty("user_id")
    Long userId;

    @JsonProperty("match")
    MatchPlayerStat matchPlayerStat;

    //这个需要自己设定，在MatchCal里赋予
    @JsonProperty("user_name")
    String userName;

    //这个需要自己设定，在MatchCal里赋予
    MicroUser user;

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    @Nullable
    public Long getBestId() {
        return bestId;
    }

    public void setBestId(@Nullable Long bestId) {
        this.bestId = bestId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public Long getScoreId() {
        return scoreId;
    }

    public void setScoreId(@Nullable Long scoreId) {
        this.scoreId = scoreId;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public OsuMode getMode() {
        return OsuMode.getMode(mode);
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

    public String[] getMods() {
        return mods;
    }

    public void setMods(String[] mods) {
        this.mods = mods;
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

    public Double getPp() {
        return pp;
    }

    public void setPp(Double pp) {
        this.pp = pp;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Boolean isReplay() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public MatchPlayerStat getMatchPlayerStat() {
        return matchPlayerStat;
    }

    public void setMatchPlayerStat(MatchPlayerStat matchPlayerStat) {
        this.matchPlayerStat = matchPlayerStat;
    }

    public Boolean getPassed() {
        return passed;
    }

    public Boolean getPerfect() {
        return perfect;
    }

    public Boolean getReplay() {
        return replay;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public MicroUser getUser() {
        return user;
    }

    public void setUser(MicroUser user) {
        this.user = user;
    }
}
