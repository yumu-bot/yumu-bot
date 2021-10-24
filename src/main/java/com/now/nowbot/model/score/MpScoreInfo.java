package com.now.nowbot.model.score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MpScoreInfo {
    Long id;
    @JsonProperty("user_id")
    Integer userId;
    Double accuracy;
    String[] mods;
    Integer score;
    @JsonProperty("max_combo")
    Integer maxCombo;
    Boolean passed;
    Integer perfect;
    //待实现
//    Statistics statistics;
    Integer rank;
    @JsonProperty("created_at")
    OffsetDateTime createdAt;
    @JsonProperty("best_id")
    Integer bestId;
    Integer pp;
    //slot, team, pass
    JsonNode match;

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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
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

    public Integer getPp() {
        return pp;
    }

    public void setPp(Integer pp) {
        this.pp = pp;
    }
}
