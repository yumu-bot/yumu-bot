package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "osu_beatmap",indexes = {
        @Index(name = "sid", columnList = "map_id,beatmap_id"),
        @Index(name = "find", columnList = "beatmap_id"),
        @Index(name = "map_find", columnList = "map_id")
})
public class BeatmapLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "beatmap_id")
    private Integer beatmap_id;

    @Column(name = "map_id")
    private Integer mapset_id;

    //是否为转谱
    private Boolean convert;
    //难度名
    private String version;

    int playcount;
    int passcount;

    //四维
    //accuracy值
    private Float od;
    private Float cs;
    private Float ar;
    //drain值
    private Float hp;

    private Float difficulty_rating;
    private Float bpm;
    private Integer max_combo;

    //物件数
    private Integer count_circles;
    private Integer count_sliders;
    private Integer count_spinners;

    //秒
    private Integer total_length;
    private Integer hit_length;


    //mode_init 0->osu ...
    private Integer mode;

    public Integer getBeatmap_id() {
        return beatmap_id;
    }

    public void setBeatmap_id(Integer bitmapID) {
        this.beatmap_id = bitmapID;
    }

    public Integer getMapset_id() {
        return mapset_id;
    }

    public void setMapset_id(Integer mapset_id) {
        this.mapset_id = mapset_id;
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

    public Float getDifficulty_rating() {
        return difficulty_rating;
    }

    public void setDifficulty_rating(Float difficulty_rating) {
        this.difficulty_rating = difficulty_rating;
    }

    public Float getBpm() {
        return bpm;
    }

    public void setBpm(Float bpm) {
        this.bpm = bpm;
    }

    public Integer getMax_combo() {
        return max_combo;
    }

    public void setMax_combo(Integer max_combo) {
        this.max_combo = max_combo;
    }

    public Integer getCount_circles() {
        return count_circles;
    }

    public void setCount_circles(Integer count_circles) {
        this.count_circles = count_circles;
    }

    public Integer getCount_sliders() {
        return count_sliders;
    }

    public void setCount_sliders(Integer count_sliders) {
        this.count_sliders = count_sliders;
    }

    public Integer getCount_spinners() {
        return count_spinners;
    }

    public void setCount_spinners(Integer count_spinners) {
        this.count_spinners = count_spinners;
    }

    public Integer getTotal_length() {
        return total_length;
    }

    public void setTotal_length(Integer total_length) {
        this.total_length = total_length;
    }

    public Integer getHit_length() {
        return hit_length;
    }

    public void setHit_length(Integer hit_length) {
        this.hit_length = hit_length;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }
}
