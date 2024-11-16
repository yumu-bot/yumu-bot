package com.now.nowbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "beat_map_4_pool")
public class BeatMap4Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "order_number")
    Integer order = 0;

    @Column(name = "beatmap_id")
    Integer beatmapID;

    @Column(name = "pool_id")
    Integer poolID;

    //@Lob
    @Column(columnDefinition = "TEXT")
    String info;

    //@Lob
    @Column(columnDefinition = "TEXT")
    String data;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getBeatmapID() {
        return beatmapID;
    }

    public void setBeatmapID(Integer beatmapID) {
        this.beatmapID = beatmapID;
    }

    public Integer getPoolID() {
        return poolID;
    }

    public void setPoolID(Integer poolID) {
        this.poolID = poolID;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}