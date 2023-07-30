package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.enums.OsuMode;

import java.util.Map;

/**
 * 注意,countxxx只有成绩相关的statustucs存在,而且不包含其他部分,别问为啥掺在一起,问就是ppysb
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Statistics {
    @JsonProperty("count_50")
    Integer count50;
    @JsonProperty("count_100")
    Integer count100;
    @JsonProperty("count_300")
    Integer count300;
    @JsonProperty("count_geki")
    Integer countGeki;
    @JsonProperty("count_katu")
    Integer countKatu;
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
        if (data != null && data.size() > 0){
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

    public Integer getCount50() {
        return count50;
    }

    public void setCount50(Integer count50) {
        this.count50 = count50;
    }

    public Integer getCount100() {
        return count100;
    }

    public void setCount100(Integer count100) {
        this.count100 = count100;
    }

    public Integer getCount300() {
        return count300;
    }

    public void setCount300(Integer count300) {
        this.count300 = count300;
    }

    public Integer getCountGeki() {
        return countGeki;
    }

    public void setCountGeki(Integer countGeki) {
        this.countGeki = countGeki;
    }

    public Integer getCountKatu() {
        return countKatu;
    }

    public void setCountKatu(Integer countKatu) {
        this.countKatu = countKatu;
    }

    public Integer getCountMiss() {
        return countMiss;
    }

    public void setCountMiss(Integer countMiss) {
        this.countMiss = countMiss;
    }

    public Long getRankedScore() {
        return rankedScore;
    }

    public Integer getCountAll(OsuMode mode) {
        int countAll = 0;
        int s_300 = getCount300();
        int s_100 = getCount100();
        int s_50 = getCount50();
        int s_g = getCountGeki();
        int s_k = getCountKatu();
        int s_0 = getCountMiss();

        switch (mode) {
            case OSU : countAll = s_300 + s_100 + s_50 + s_0; break;
            case TAIKO:
            case CATCH : countAll = s_300 + s_100 + s_0; break;
            case MANIA : countAll = s_g + s_300 + s_k + s_100 + s_50 + s_0; break;
        }
        return countAll;
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

    /**
     * 获得指定小数位的pp
     * @param i 位数
     * @return
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
     * @return
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

    public void setPp(Double pp) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Statistics{");
        sb.append("count50=").append(count50);
        sb.append(", count100=").append(count100);
        sb.append(", count300=").append(count300);
        sb.append(", countGeki=").append(countGeki);
        sb.append(", countKatu=").append(countKatu);
        sb.append(", countMiss=").append(countMiss);
        sb.append(", rankedScore=").append(rankedScore);
        sb.append(", totalScore=").append(totalScore);
        sb.append(", pp=").append(pp);
        sb.append(", accuracy=").append(accuracy);
        sb.append(", playCount=").append(playCount);
        sb.append(", playTime=").append(playTime);
        sb.append(", totalHits=").append(totalHits);
        sb.append(", maxCombo=").append(maxCombo);
        sb.append(", isRanked=").append(isRanked);
        sb.append(", globalRank=").append(globalRank);
        sb.append(", replaysWatchedByOthers=").append(replaysWatchedByOthers);
        sb.append(", countryRank=").append(countryRank);
        sb.append(", levelCurrent=").append(levelCurrent);
        sb.append(", levelProgress=").append(levelProgress);
        sb.append(", SS=").append(SS);
        sb.append(", SSH=").append(SSH);
        sb.append(", S=").append(S);
        sb.append(", SH=").append(SH);
        sb.append(", A=").append(A);
        sb.append(", countryRank7K=").append(countryRank7K);
        sb.append(", countryRank4K=").append(countryRank4K);
        sb.append(", rank7K=").append(rank7K);
        sb.append(", rank4K=").append(rank4K);
        sb.append(", PP7K=").append(PP7K);
        sb.append(", PP4K=").append(PP4K);
        sb.append('}');
        return sb.toString();
    }
}
