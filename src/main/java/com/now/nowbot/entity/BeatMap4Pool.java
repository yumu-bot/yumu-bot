package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "beat_map_4_pool")
public class BeatMap4Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    Integer beatmapId;

    Integer poolId;

    String info;

    String checkModel;

    String checkId;

    String data;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}