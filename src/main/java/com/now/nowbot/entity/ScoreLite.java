package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_score", indexes = {
        @Index(name = "uid", columnList = "osu_id")
})
public class ScoreLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "score_id")
    private Long scoreId;
    @Column(name = "osu_id")
    private Integer osuId;
    @Column(name = "beatmap_id")
    private Integer beatmapId;

    private Float accuracy;
    //','分割的字符串
    @Lob
    private String mods;
    private Integer score;
    private Integer maxCombo;
    boolean passed;
    boolean perfect;

    private Integer count50;
    private Integer count100;
    private Integer count300;
    private Integer countgeki;
    private Integer countkatu;
    private Integer countmiss;

    @Column(name = "rank_list")
    private Integer rank;
    //created_at
    private LocalDateTime date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScoreId() {
        return scoreId;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public Integer getOsuId() {
        return osuId;
    }

    public void setOsuId(Integer osuId) {
        this.osuId = osuId;
    }

    public Integer getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(Integer beatmapId) {
        this.beatmapId = beatmapId;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String getMods() {
        return mods;
    }

    public void setMods(String mods) {
        this.mods = mods;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isPerfect() {
        return perfect;
    }

    public void setPerfect(boolean perfect) {
        this.perfect = perfect;
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

    public Integer getCountgeki() {
        return countgeki;
    }

    public void setCountgeki(Integer countgeki) {
        this.countgeki = countgeki;
    }

    public Integer getCountkatu() {
        return countkatu;
    }

    public void setCountkatu(Integer countkatu) {
        this.countkatu = countkatu;
    }

    public Integer getCountmiss() {
        return countmiss;
    }

    public void setCountmiss(Integer countmiss) {
        this.countmiss = countmiss;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
