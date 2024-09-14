package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MaiBestPerformance {
    // 看用户段位信息，其中0-10对应初学者-十段，11-20对应真初段-真十段，21-22对应真皆传-里皆传
    @JsonProperty("additional_rating")
    Integer additional;

    // b40 和 b50 一样，但区别是 dx 其实是最新版本的 b15，sd 是综合的 b35
    // 这就是 BP
    @JsonProperty("charts")
    Charts charts;

    public record Charts(
            @JsonProperty("dx") List<MaiScore> deluxe,
            @JsonProperty("sd") List<MaiScore> standard) {}

    // 在游戏里的名字
    @JsonProperty("nickname")
    String name;

    // 牌子信息，比如“舞神”
    String plate;

    // PP，理论上限 1w6 多
    Integer rating;

    // 没有用
    //@JsonIgnoreProperties("user_general_data")
    //String general;

    // 在查分器里的名字
    @JsonProperty("username")
    String probername;

    public Integer getAdditional() {
        return additional;
    }

    public void setAdditional(Integer additional) {
        this.additional = additional;
    }

    public Charts getCharts() {
        return charts;
    }

    public void setCharts(Charts charts) {
        this.charts = charts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getProbername() {
        return probername;
    }

    public void setProbername(String probername) {
        this.probername = probername;
    }
}
