package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChuScore {
    @JsonProperty("cid")
    Integer chartID;

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds")
    Double star;

    // 纯分数
    @JsonProperty("score")
    Integer score;

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc")
    String combo;

    // 定数的实际显示，0.6-0.9 后面会多一个 +
    @JsonProperty("level")
    String level;

    // 定数的位置，0-4
    @JsonProperty("level_index")
    Integer index;

    // 实际所属的难度分类，Basic，Advanced，Expert，Master，Ultima, World's End
    @JsonProperty("level_label")
    String difficulty;

    @JsonProperty("mid")
    Integer musicID;

    // CHUNITHM rating，也就是 PP，通过计算向下取整
    @JsonProperty("ra")
    Integer rating;

    String title;

    public Integer getChartID() {
        return chartID;
    }

    public void setChartID(Integer chartID) {
        this.chartID = chartID;
    }

    public Double getStar() {
        return star;
    }

    public void setStar(Double star) {
        this.star = star;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getCombo() {
        return combo;
    }

    public void setCombo(String combo) {
        this.combo = combo;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getMusicID() {
        return musicID;
    }

    public void setMusicID(Integer musicID) {
        this.musicID = musicID;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
