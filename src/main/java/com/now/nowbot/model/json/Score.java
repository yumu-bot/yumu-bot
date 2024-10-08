package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.*;
import com.now.nowbot.model.enums.OsuMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, allowSetters = true, allowGetters = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Score {
    static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd")
    .appendLiteral("T")
    .appendPattern("HH:mm:ss")
    .appendZoneId().toFormatter();

    //    @JsonProperty("statistics")
    Double accuracy;

    @JsonProperty("best_id")
    Long bestID;

    @JsonProperty("max_combo")
    Integer maxCombo;

    @JsonProperty("user_id")
    Long UID;

    @JsonAlias("created_at")
    String createTime;

    @JsonProperty("id")
    Long scoreID;

    @JsonIgnoreProperties
    OsuMode mode;

    @JsonProperty("mode_int")
    Integer modeInt;

    List<String> osuMods;

    Boolean passed;

    Boolean perfect;

    @JsonProperty("pp")
    Float PP;

    String rank;

    Boolean replay;

    Integer score;

    Statistics statistics;

    String type;

    @JsonIgnoreProperties
    boolean legacy;

    // 仅查询bp时存在
    @JsonProperty("weight")
    Weight weight;

    public record Weight(
    @JsonProperty("percentage") Float percentage,
    @JsonProperty("pp") Float weightedPP) {
        public int getIndex() {
            var i = Math.log(percentage / 100) / Math.log(0.95);
            return (int) Math.round(i);
        }
    }

    @JsonProperty("beatmap")
    BeatMap beatMap;

    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    MicroUser user;

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

    public Long getBestID() {
        return bestID;
    }

    public void setBestID(Long bestID) {
        this.bestID = bestID;
    }

    public Long getUID() {
        return UID;
    }

    public void setUID(Long UID) {
        this.UID = UID;
    }

    public LocalDateTime getCreateTimePretty() {
        if (createTime != null) return LocalDateTime.parse(createTime, formatter).plusHours(8L);
        return LocalDateTime.now();
    }

    @JsonProperty("create_at_str")
    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Long getScoreID() {
        return scoreID;
    }

    public void setScoreID(Long scoreID) {
        this.scoreID = scoreID;
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
        return osuMods;
    }

    public void setMods(List<String> osuMods) {
        this.osuMods = osuMods;
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
        if (Objects.nonNull(PP)) {
            return PP;
        }

        // PPY PP 有时候是 null
        if (Objects.nonNull(weight) && Objects.nonNull(weight.percentage) && Objects.nonNull(weight.weightedPP) && weight.percentage > 0) {
            return weight.weightedPP / (weight.percentage / 100f);
        }

        return 0f;
    }

    public void setPP(Float pp) {
        this.PP = pp;
    }

    public Float getWeightedPP() {
        if (Objects.nonNull(weight) && Objects.nonNull(weight.weightedPP)) {
            return weight.weightedPP;
        }

        return 0f;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isLegacy() {
        legacy = Objects.nonNull(type) && !Objects.equals(type, "solo_score"); //目前只看见有这个类别，mp 房也是这个类别
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
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

    public Boolean isPerfect() {
        return perfect;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }


    @Override
    public String toString() {
        return STR."Score{accuracy=\{accuracy}, bestID=\{bestID}, maxCombo=\{maxCombo}, UID=\{UID}, createTime='\{createTime}\{'\''}, scoreID=\{scoreID}, mode=\{mode}, modeInt=\{modeInt}, osuMods=\{osuMods}, passed=\{passed}, perfect=\{perfect}, PP=\{PP}, rank='\{rank}\{'\''}, replay=\{replay}, score=\{score}, statistics=\{statistics}, type='\{type}\{'\''}, legacy=\{legacy}, weight=\{weight}, beatMap=\{beatMap}, beatMapSet=\{beatMapSet}, user=\{user}\{'}'}";
    }
}
