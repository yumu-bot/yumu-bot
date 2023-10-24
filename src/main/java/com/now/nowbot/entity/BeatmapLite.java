package com.now.nowbot.entity;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.enums.OsuMode;

import jakarta.persistence.*;
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
    private Long userId;

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
    Float od;
    Float cs;
    Float ar;
    //drain值
    Float hp;

    Float difficultyRating;
    Float bpm;
    Integer maxCombo;

    //物件数
    Integer circles;
    Integer sliders;
    Integer spinners;

    //秒
    Integer totalLength;
    Integer hitLength;


    //mode_init 0->osu ...
    Integer modeInt;
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

    public Float getOD() {
        return od;
    }

    public void setOD(Float od) {
        this.od = od;
    }

    public Float getCS() {
        return cs;
    }

    public void setCS(Float cs) {
        this.cs = cs;
    }

    public Float getAR() {
        return ar;
    }

    public void setAR(Float ar) {
        this.ar = ar;
    }

    public Float getHP() {
        return hp;
    }

    public void setHP(Float hp) {
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

    public Integer getModeInt() {
        return modeInt;
    }
    public OsuMode getMode() {
        return OsuMode.getMode(modeInt);
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public BeatMap toBeatMap(){
        var b = new BeatMap();
        b.setBID(getId());
        b.setBeatmapsetId(getBeatmapsetId());
        b.setConvert(getConvert());
        b.setVersion(getVersion());
        b.setPlaycount(getPlaycount());
        b.setPasscount(getPasscount());
        b.setOD(getOD());
        b.setCS(getCS());
        b.setAR(getAR());
        b.setHP(getHP());
        b.setDifficultyRating(getDifficultyRating());
        b.setBpm(getBpm());
        b.setMaxCombo(getMaxCombo());
        b.setStatus(getStatus());
        b.setCircles(getCircles());
        b.setSliders(getSliders());
        b.setSpinners(getSpinners());
        b.setTotalLength(getTotalLength());
        b.setHitLength(getHitLength());
        b.setModeInt(getModeInt());
        b.setUserId(getUserId());
        b.setPlaycount(getPlaycount());
        return b;
    }
}
