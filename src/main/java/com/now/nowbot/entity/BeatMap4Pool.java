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
    @Column(name = "order_number", nullable = false)
    Integer order;

    Integer beatmapId;

    Integer poolId;

    String info;

    String data;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}