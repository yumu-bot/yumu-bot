package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

// 由于从 API 拿到的数据需要排序，请使用 ArrayList
// 而且这个最好存自己数据库，一周更新一次
public class MaiRanking {
    @JsonProperty("username")
    String name;

    @JsonProperty("ra")
    Integer rating;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
