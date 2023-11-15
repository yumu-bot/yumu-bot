package com.now.nowbot.model.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.enums.OsuMode;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MPScore {
    Long id;
    @JsonProperty("user_id")
    Integer UID;
    String userName;
    Double accuracy;
    String[] mods;
    Integer score;
    @JsonProperty("max_combo")
    Integer maxCombo;

    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;

    Boolean passed;
    Integer perfect;

    Statistics statistics;

    // rank 默认是 F
    String rank;
    @JsonProperty("created_at")
    OffsetDateTime createdAt;
    @JsonProperty("best_id")
    Integer bestId;
    Integer pp;
    //slot, team, pass
    JsonNode match;

    OsuUser osuUser;
    String team;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public OsuUser getOsuUser() {
        return osuUser;
    }

    public void setOsuUser(OsuUser osuUser) {
        this.osuUser = osuUser;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public JsonNode getMatch() {
        return match;
    }

    public void setMatch(JsonNode match) {
        this.match = match;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUID() {
        return UID;
    }

    public void setUID(Integer uid) {
        this.UID = uid;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public String[] getMods() {
        return mods;
    }

    public void setMods(String[] mods) {
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

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Integer getPerfect() {
        return perfect;
    }

    public void setPerfect(Integer perfect) {
        this.perfect = perfect;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getBestId() {
        return bestId;
    }

    public void setBestId(Integer bestId) {
        this.bestId = bestId;
    }

    public Integer getPP() {
        return pp;
    }

    public void setPP(Integer pp) {
        this.pp = pp;
    }
}
