package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * 注意，成绩内的 Statistics 只有 count_xxx 的指标
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Statistics implements Cloneable {
    @Nullable
    @JsonProperty("count_50")
    Integer count50;
    @Nullable
    @JsonProperty("count_100")
    Integer count100;
    @Nullable
    @JsonProperty("count_300")
    Integer count300;
    @Nullable
    @JsonProperty("count_geki")
    Integer countGeki;
    @Nullable
    @JsonProperty("count_katu")
    Integer countKatu;
    @Nullable
    @JsonProperty("count_miss")
    Integer countMiss;
    Integer countAll;
    @JsonProperty("ranked_score")
    Long rankedScore;
    @JsonProperty("total_score")
    Long totalScore;
    Double pp;
    @JsonProperty("hit_accuracy")
    Double accuracy;
    @JsonProperty("play_count")
    Long playCount;
    @JsonProperty("play_time")
    Long playTime;
    @JsonProperty("total_hits")
    Long totalHits;
    @JsonProperty("maximum_combo")
    Integer maxCombo;
    @JsonProperty("is_ranked")
    Boolean isRanked;
    @JsonProperty("global_rank")
    Long globalRank;
    @JsonProperty("replays_watched_by_others")
    Integer replaysWatchedByOthers;
    @JsonProperty("country_rank")
    Long countryRank;
    @JsonIgnore
    Integer levelCurrent;
    @JsonIgnore
    Integer levelProgress;
    @JsonIgnore
    Integer SS;
    @JsonIgnore
    Integer SSH;
    @JsonIgnore
    Integer S;
    @JsonIgnore
    Integer SH;
    @JsonIgnore
    Integer A;
    @JsonProperty("level")
    public void setLevel(Map<String, Integer> map){
        levelCurrent = map.get("current");
        levelProgress = map.get("progress");
    }
    @JsonProperty("grade_counts")
    void setGrade(Map<String,Integer> map){
        SS = map.get("ss");
        SSH = map.get("ssh");
        S = map.get("s");
        SH = map.get("sh");
        A = map.get("a");
    }

    @JsonIgnore
    Integer countryRank7K;
    @JsonIgnore
    Integer countryRank4K;
    @JsonIgnore
    Integer rank7K;
    @JsonIgnore
    Integer rank4K;
    @JsonIgnore
    Double PP7K;
    @JsonIgnore
    Double PP4K;

    @JsonProperty("variants")
    void setVariants(JsonNode data){
        if (data != null && !data.isEmpty()){
            for (var m : data){
                if (m.get("variant").asText().equals("4k")){
                    countryRank4K = m.get("country_rank").asInt();
                    rank4K = m.get("global_rank").asInt();
                    PP4K = m.get("pp").asDouble();
                } else {
                    countryRank7K = m.get("country_rank").asInt();
                    rank7K = m.get("global_rank").asInt();
                    PP7K = m.get("pp").asDouble();
                }
            }
        }
    }

    @NonNull
    public Integer getCount50() {
        return Objects.requireNonNullElse(count50, 0);
    }

    public void setCount50(@Nullable Integer count50) {
        this.count50 = count50;
    }

    @NonNull
    public Integer getCount100() {
        return Objects.requireNonNullElse(count100, 0);
    }

    public void setCount100(@Nullable Integer count100) {
        this.count100 = count100;
    }

    @NonNull
    public Integer getCount300() {
        return Objects.requireNonNullElse(count300, 0);
    }

    public void setCount300(@Nullable Integer count300) {
        this.count300 = count300;
    }

    @NonNull
    public Integer getCountGeki() {
        return Objects.requireNonNullElse(countGeki, 0);
    }

    public void setCountGeki(@Nullable Integer countGeki) {
        this.countGeki = countGeki;
    }

    @NonNull
    public Integer getCountKatu() {
        return Objects.requireNonNullElse(countKatu, 0);
    }

    public void setCountKatu(@Nullable Integer countKatu) {
        this.countKatu = countKatu;
    }

    @NonNull
    public Integer getCountMiss() {
        return Objects.requireNonNullElse(countMiss, 0);
    }

    public void setCountMiss(@Nullable Integer countMiss) {
        this.countMiss = countMiss;
    }

    public Long getRankedScore() {
        return rankedScore;
    }

    public Integer getCountAll() {
        return getCountAll(OsuMode.MANIA);
    }

    @NonNull
    public Integer getCountAll(OsuMode mode) {
        int s_300 = getCount300();
        int s_100 = getCount100();
        int s_50 = getCount50();
        int s_g = getCountGeki();
        int s_k = getCountKatu();
        int s_0 = getCountMiss();

        return switch (mode) {
            case OSU, DEFAULT -> s_300 + s_100 + s_50 + s_0;
            case TAIKO -> s_300 + s_100 + s_0;
            case CATCH -> s_300 + s_100 + s_50 + s_0 + s_k;
            case MANIA -> s_g + s_300 + s_k + s_100 + s_50 + s_0;
            case null -> s_g + s_300 + s_k + s_100 + s_50 + s_0;
        };
    }

    public void setCountAll(Integer countAll) {
        this.countAll = countAll;
    }

    public void setRankedScore(Long rankedScore) {
        this.rankedScore = rankedScore;
    }

    public Long getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Long totalScore) {
        this.totalScore = totalScore;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    @NonNull
    public Double getAccuracy(OsuMode mode) {
        switch (mode) {
            case OSU, DEFAULT -> {
                return (getCount50() / 6d + getCount100() / 3d + getCount300()) / getCountAll(OsuMode.OSU);
            }
            case TAIKO -> {
                return (getCount100() / 2d + getCount300()) / getCountAll(OsuMode.TAIKO);
            }
            case CATCH -> {
                return (getCount50() + getCount100() + getCount300()) * 1d / getCountAll(OsuMode.CATCH);
            }
            case MANIA -> {
                return (getCount50() / 6d + getCount100() / 3d + getCount300() + getCountKatu() * 2d / 3d + getCountGeki()) / getCountAll(OsuMode.MANIA);
            }
            case null, default -> {
                return (getCount50() / 6d + getCount100() / 3d + getCount300()) / getCountAll(OsuMode.DEFAULT);
            }
        }
    }

    /**
     * 获得指定小数位的 acc
     * @param i 位数
     * @return acc
     */
    public Double getAccuracy(int i) {
        int n = (int) Math.pow(10, i);
        double c = 0;
        if (accuracy != null){
            c = accuracy;
        }
        double out = Math.round(c * n);
        out /= n;
        return out;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public Long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(Long playTime) {
        this.playTime = playTime;
    }

    public Long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Long totalHits) {
        this.totalHits = totalHits;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Boolean getRanked() {
        return isRanked;
    }

    public void setRanked(Boolean ranked) {
        isRanked = ranked;
    }

    public Long getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(Long globalRank) {
        this.globalRank = globalRank;
    }

    public Long getCountryRank() {
        return countryRank;
    }

    public void setCountryRank(Long countryRank) {
        this.countryRank = countryRank;
    }

    @JsonProperty("level_current")
    public Integer getLevelCurrent() {
        return levelCurrent;
    }

    public void setLevelCurrent(Integer levelCurrent) {
        this.levelCurrent = levelCurrent;
    }

    @JsonProperty("level_progress")
    public Integer getLevelProgress() {
        return levelProgress;
    }

    public void setLevelProgress(Integer levelProgress) {
        this.levelProgress = levelProgress;
    }

    public Integer getSS() {
        return SS;
    }

    public void setSS(Integer SS) {
        this.SS = SS;
    }

    public Integer getSSH() {
        return SSH;
    }

    public void setSSH(Integer SSH) {
        this.SSH = SSH;
    }

    public Integer getS() {
        return S;
    }

    public void setS(Integer s) {
        S = s;
    }

    public Integer getSH() {
        return SH;
    }

    public void setSH(Integer SH) {
        this.SH = SH;
    }

    public Integer getA() {
        return A;
    }

    public void setA(Integer a) {
        A = a;
    }

    public Double getPP() {
        return pp;
    }

    /**
     * 获得指定小数位的pp
     * @param x 位数
     * @return pp
     */
    public float getPP(int x){
        int n = (int) Math.pow(10, x);
        double c = 0;
        if (pp != null){
            c = pp;
        }
        float out = Math.round(c * n);
        out /= n;
        return out;
    }

    public void setPP(Double pp) {
        this.pp = pp;
    }

    public Integer getReplaysWatchedByOthers() {
        return replaysWatchedByOthers;
    }

    @JsonProperty("country_rank_7k")
    public Integer getCountryRank7K() {
        return countryRank7K;
    }

    @JsonProperty("country_rank_4k")
    public Integer getCountryRank4K() {
        return countryRank4K;
    }

    @JsonProperty("rank_7k")
    public Integer getRank7K() {
        return rank7K;
    }

    @JsonProperty("rank_4k")
    public Integer getRank4K() {
        return rank4K;
    }

    @JsonProperty("pp_7k")
    public Double getPP7K() {
        return PP7K;
    }

    @JsonProperty("pp_4k")
    public Double getPP4K() {
        return PP4K;
    }

    public boolean nonNull() {
        return countGeki != null && count300 != null && countKatu != null && count100 != null && count50 != null && countMiss != null;
    }

    public boolean isNull() {
        return !this.nonNull();
    }

    @Override
    public String toString() {
        return STR."Statistics{count50=\{count50}, count100=\{count100}, count300=\{count300}, countGeki=\{countGeki}, countKatu=\{countKatu}, countMiss=\{countMiss}, rankedScore=\{rankedScore}, totalScore=\{totalScore}, pp=\{pp}, accuracy=\{accuracy}, playCount=\{playCount}, playTime=\{playTime}, totalHits=\{totalHits}, maxCombo=\{maxCombo}, isRanked=\{isRanked}, globalRank=\{globalRank}, replaysWatchedByOthers=\{replaysWatchedByOthers}, countryRank=\{countryRank}, levelCurrent=\{levelCurrent}, levelProgress=\{levelProgress}, SS=\{SS}, SSH=\{SSH}, S=\{S}, SH=\{SH}, A=\{A}, countryRank7K=\{countryRank7K}, countryRank4K=\{countryRank4K}, rank7K=\{rank7K}, rank4K=\{rank4K}, PP7K=\{PP7K}, PP4K=\{PP4K}\{'}'}";
    }

    @Override
    public Statistics clone() {
        try {
            return (Statistics) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
