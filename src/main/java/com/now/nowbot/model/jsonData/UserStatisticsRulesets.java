package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

// 这不就是 osumode? 这是四 mode 下的数据
public class UserStatisticsRulesets {
    @JsonProperty("osu")
    Statistics osu;
    @JsonProperty("taiko")
    Statistics taiko;
    @JsonProperty("fruits")
    Statistics fruits;
    @JsonProperty("mania")
    Statistics mania;

    public Statistics getOsu() {
        return osu;
    }

    public Statistics getTaiko() {
        return taiko;
    }

    public Statistics getFruits() {
        return fruits;
    }

    public Statistics getMania() {
        return mania;
    }
}
