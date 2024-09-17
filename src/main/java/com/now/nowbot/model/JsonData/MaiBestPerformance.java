package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class MaiBestPerformance {
    // 看用户段位信息，其中0-10对应初学者-十段，11-20对应真初段-真十段，21-22对应真皆传-里皆传
    @JsonProperty("additional_rating")
    Integer dan;

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

    public Integer getDan() {
        return dan;
    }

    public void setDan(Integer dan) {
        this.dan = dan;
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

    public record User(String name, String probername, Integer dan, String plate, Integer rating, Integer base, Integer additional) {}

    public User getUser() {
        var best35 = this.charts.standard.stream().map(MaiScore::getRating)
                .filter(Objects::nonNull).reduce(Integer::sum).orElse(0);
        var best15 = this.charts.deluxe.stream().map(MaiScore::getRating)
                .filter(Objects::nonNull).reduce(Integer::sum).orElse(0);

        return new User(this.name, this.probername, this.dan, this.plate, this.rating, best35, best15);
    }
}
