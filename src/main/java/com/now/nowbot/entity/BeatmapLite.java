package com.now.nowbot.entity;

import com.now.nowbot.model.enums.OsuMode;

import javax.persistence.*;
@Entity
@Table(name = "osu_beatmap",indexes = {
        @Index(name = "map_find", columnList = "map_id"),
        @Index(name = "sid", columnList = "map_id, id"),
})
public class BeatmapLite {
    @Id
    private Long id;

    @Column(name = "map_id", insertable = false, updatable = false)
    private Integer beatmapsetId;

    @Column(name = "mapper_id")
    private Integer userId;

    //是否为转谱
    @Column(name = "is_convert")
    private Boolean convert;
    //难度名
    @Column(columnDefinition = "text")
    private String version;
    @Column(columnDefinition = "text")
    private String status;

    Integer playcount;
    Integer passcount;

    //四维
    //accuracy值
    private Float od;
    private Float cs;
    private Float ar;
    //drain值
    private Float hp;

    private Float difficultyRating;
    private Float bpm;
    private Integer maxCombo;

    //物件数
    private Integer circles;
    private Integer sliders;
    private Integer spinners;

    //秒
    private Integer totalLength;
    private Integer hitLength;


    //mode_init 0->osu ...
    private Integer modeInt;
    Integer ranked;

    @ManyToOne()
    @JoinColumn(name = "map_id")
    MapSetLite mapSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBeatmapsetId() {
        return beatmapsetId;
    }

    public void setBeatmapsetId(Integer beatmapsetId) {
        this.beatmapsetId = beatmapsetId;
    }

    public Boolean getConvert() {
        return convert;
    }

    public void setConvert(Boolean convert) {
        this.convert = convert;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPlaycount() {
        return playcount;
    }

    public void setPlaycount(int playcount) {
        this.playcount = playcount;
    }

    public int getPasscount() {
        return passcount;
    }

    public void setPasscount(int passcount) {
        this.passcount = passcount;
    }

    public Float getOd() {
        return od;
    }

    public void setOd(Float od) {
        this.od = od;
    }

    public Float getCs() {
        return cs;
    }

    public void setCs(Float cs) {
        this.cs = cs;
    }

    public Float getAr() {
        return ar;
    }

    public void setAr(Float ar) {
        this.ar = ar;
    }

    public Float getHp() {
        return hp;
    }

    public void setHp(Float hp) {
        this.hp = hp;
    }

    public Float getDifficultyRating() {
        return difficultyRating;
    }

    public void setDifficultyRating(Float difficultyRating) {
        this.difficultyRating = difficultyRating;
    }

    public Float getBpm() {
        return bpm;
    }

    public void setBpm(Float bpm) {
        this.bpm = bpm;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCircles() {
        return circles;
    }

    public void setCircles(Integer circles) {
        this.circles = circles;
    }

    public Integer getSliders() {
        return sliders;
    }

    public void setSliders(Integer sliders) {
        this.sliders = sliders;
    }

    public Integer getSpinners() {
        return spinners;
    }

    public void setSpinners(Integer spinners) {
        this.spinners = spinners;
    }

    public Integer getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(Integer totalLength) {
        this.totalLength = totalLength;
    }

    public Integer getHitLength() {
        return hitLength;
    }

    public void setHitLength(Integer hitLength) {
        this.hitLength = hitLength;
    }

    public OsuMode getModeInt() {
        return OsuMode.getMode(modeInt);
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setPlaycount(Integer playcount) {
        this.playcount = playcount;
    }

    public void setPasscount(Integer passcount) {
        this.passcount = passcount;
    }

    public Integer getRanked() {
        return ranked;
    }

    public void setRanked(Integer ranked) {
        this.ranked = ranked;
    }

    public MapSetLite getMapSet() {
        return mapSet;
    }

    public void setMapSet(MapSetLite mapSet) {
        this.mapSet = mapSet;
    }
}
