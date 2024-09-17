package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class MaiScore {
    // 也就是准确率
    Double achievements;

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds")
    @Nullable
    Double star;

    // DX 分，Critical Perfect 3 分，Perfect 2 分，Great 1 分
    // 在查别人的时候，这个值是 0
    @JsonProperty("dxScore")
    @Nullable
    Integer score;

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc")
    @NonNull
    String combo = "";

    // 同步状态，主要是综合看你和拼机的玩家
    @JsonProperty("fs")
    @NonNull
    String sync = "";

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level")
    @NonNull
    String level = "?";

    // 定数的位置，0-4
    @JsonProperty("level_index")
    @NonNull
    Integer index = 0;

    // 实际所属的难度分类，Basic，Advanced，Expert，Master，Re:Master
    @JsonProperty("level_label")
    @Nullable
    String difficulty;

    // DX rating，也就是 PP，通过计算向下取整
    @JsonProperty("ra")
    @Nullable
    Integer rating;

    // 实际的评级
    @JsonProperty("rate")
    @Nullable
    String rank;

    // 等于 sid
    @JsonProperty("song_id")
    @Nullable
    Long songID;

    // 歌名
    @NonNull
    String title = "";

    // 谱面种类，有DX和SD之分
    @JsonProperty("type")
    @NonNull
    String type = "";

    // 通过 MaiSong 算出来的理论 DX Score
    @JsonIgnoreProperties
    Integer max;

    public Double getAchievements() {
        return achievements;
    }

    public void setAchievements(Double achievements) {
        this.achievements = achievements;
    }

    @Nullable
    public Double getStar() {
        return star;
    }

    public void setStar(@Nullable Double star) {
        this.star = star;
    }

    @Nullable
    public Integer getScore() {
        return score;
    }

    public void setScore(@Nullable Integer score) {
        this.score = score;
    }

    @NonNull
    public String getCombo() {
        return combo;
    }


    public void setCombo(@NonNull String combo) {
        this.combo = combo;
    }

    @NonNull
    public String getSync() {
        return sync;
    }


    public void setSync(@NonNull String sync) {
        this.sync = sync;
    }

    @NonNull
    public String getLevel() {
        return level;
    }


    public void setLevel(@NonNull String level) {
        this.level = level;
    }

    @NonNull
    public Integer getIndex() {
        return index;
    }

    public void setIndex(@NonNull Integer index) {
        this.index = index;
    }

    @Nullable
    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(@Nullable String difficulty) {
        this.difficulty = difficulty;
    }

    @Nullable
    public Integer getRating() {
        return rating;
    }

    public void setRating(@Nullable Integer rating) {
        this.rating = rating;
    }

    @Nullable
    public String getRank() {
        return rank;
    }

    public void setRank(@Nullable String rank) {
        this.rank = rank;
    }

    @Nullable
    public Long getSongID() {
        return songID;
    }

    public void setSongID(@Nullable Long songID) {
        this.songID = songID;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(MaiSong song) {
        var notes = song.getCharts().get(this.index).getNotes();

        if (notes != null) {
            this.max = 3 * (notes.tap() + notes.touch() + notes.hold() + notes.slide() + notes.break_());
        }
    }
}
