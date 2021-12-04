package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Statustucs {
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

    @Override
    public String toString() {
        return "Statustucs{" +
                "count50=" + count50 +
                ", count100=" + count100 +
                ", count300=" + count300 +
                ", countGeki=" + countGeki +
                ", countKatu=" + countKatu +
                ", countMiss=" + countMiss +
                '}';
    }
}
