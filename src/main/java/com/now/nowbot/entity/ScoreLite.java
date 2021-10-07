package com.now.nowbot.entity;

import org.springframework.context.annotation.Primary;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_score" ,indexes = {
        @Index(name = "uid", columnList = "osu_id")
})
public class ScoreLite {
    @Id
    @Column(name = "id")
    private Long scoreId;
    @Column(name = "osu_id")
    private Integer osuId;
    @Column(name = "beatmap_id")
    private Integer beatmapId;

    private Float accuracy;
    //','分割的字符串
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

    private String rank;
    //created_at
    private LocalDateTime date;

}
