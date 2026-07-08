package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "tachyon_score")
public class ScoreLite {
    @Id
    @Column(name = "id", nullable = false)
    private Long scoreID;

    @Column(name = "user_id", nullable = false)
    private Long userID;

    @Column(name = "beatmap_id", nullable = false)
    private Long beatmapID;

    @Column(name = "build_id")
    private Integer buildID;

    // @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "mods", columnDefinition = "char(2)[]")
    private List<String> mods = null;

    // 注意：如果是较新版本的 Hibernate 6+，@Type 的用法可能需要微调，这里保留你原本的类名
    // @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private String modsData = null;

    @Column(name = "pp", nullable = false)
    private float pp = 0f;

    @Column(name = "accuracy", nullable = false)
    private float accuracy = 0f;

    @Column(name = "combo", nullable = false)
    private int maxCombo = 0;

    @Column(name = "time", nullable = false)
    private OffsetDateTime time = OffsetDateTime.now();

    @Column(name = "fc", nullable = false)
    private boolean perfect = false;

    @Column(name = "pass", nullable = false)
    private boolean passed = false;

    @Column(name = "legacy")
    private Integer legacy = null;

    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "mode", nullable = false)
    private byte mode = (byte) -1;

    @Column(name = "rank", nullable = false)
    private byte rank = (byte) 0;

    public Long getScoreID() {
        return scoreID;
    }

    public void setScoreID(Long scoreID) {
        this.scoreID = scoreID;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public Long getBeatmapID() {
        return beatmapID;
    }

    public void setBeatmapID(Long beatmapID) {
        this.beatmapID = beatmapID;
    }

    public Integer getBuildID() {
        return buildID;
    }

    public void setBuildID(Integer buildID) {
        this.buildID = buildID;
    }

    public List<String> getMods() {
        return mods;
    }

    public void setMods(List<String> mods) {
        this.mods = mods;
    }

    public String getModsData() {
        return modsData;
    }

    public void setModsData(String modsData) {
        this.modsData = modsData;
    }

    public float getPp() {
        return pp;
    }

    public void setPp(float pp) {
        this.pp = pp;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(int maxCombo) {
        this.maxCombo = maxCombo;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public void setTime(OffsetDateTime time) {
        this.time = time;
    }

    public boolean isPerfect() {
        return perfect;
    }

    public void setPerfect(boolean perfect) {
        this.perfect = perfect;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Integer getLegacy() {
        return legacy;
    }

    public void setLegacy(Integer legacy) {
        this.legacy = legacy;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public byte getMode() {
        return mode;
    }

    public void setMode(byte mode) {
        this.mode = mode;
    }

    public byte getRank() {
        return rank;
    }

    public void setRank(byte rank) {
        this.rank = rank;
    }
}
