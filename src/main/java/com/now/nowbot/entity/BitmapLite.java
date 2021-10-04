package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(indexes = {@Index(name = "sid", columnList = "map_id")})
public class BitmapLite {
    @Id
    @Column(name = "id")
    private Integer bitmapID;

    @Column(name = "map_id")
    private Integer MapSetId;

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


}
