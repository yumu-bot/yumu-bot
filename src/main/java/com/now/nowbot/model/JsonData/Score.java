package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Score {
    static final DateTimeFormatter formatter = BpInfo.formatter;
//    @JsonProperty("statistics")
    Double accuracy;
    @JsonProperty("best_id")
    Long bestId;
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

    record User(String avatarUrl, String countryCode, Integer id, Boolean active, String name){}
    @JsonIgnoreProperties
    User user;
    @JsonProperty("user")
    public void setUser(HashMap<String, String> date){
        if (date!=null){
            String url = date.get("avatar_url");
            String cc = date.get("country_code");
            int id = date.containsKey("id")?Integer.parseInt(date.get("id")):0;
            boolean act = date.containsKey("is_active") && date.get("is_active").equals("true");
            String name = date.get("username");
            user = new User(url,cc,id,act,name);
        }
    }

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

    public Float getPp() {
        return pp;
    }

    public void setPp(Float pp) {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


}
