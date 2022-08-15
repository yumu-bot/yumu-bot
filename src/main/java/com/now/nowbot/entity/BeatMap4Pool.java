package com.now.nowbot.entity;

import com.now.nowbot.util.MapPoolUtil;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;

@Entity
@Table(name = "beat_map_4_pool")
public class BeatMap4Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_number")
    Integer order;

    Integer beatmapId;

    Integer poolId;

    @Lob
@Type(type = "org.hibernate.type.TextType")
    String info;

    @Lob
@Type(type = "org.hibernate.type.TextType")
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

    public Integer getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(Integer beatmapId) {
        this.beatmapId = beatmapId;
    }

    public Integer getPoolId() {
        return poolId;
    }

    public void setPoolId(Integer poolId) {
        this.poolId = poolId;
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