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
    long score_id;
    int osu_id;
    @Column(name = "beatmap_id")
    int beatmap_id;

    float accuracy;
    //','分割的字符串
    String mods;
    int score;
    int max_combo;
    boolean passed;
    boolean perfect;

    int count_50;
    int count_100;
    int count_300;
    int count_geki;
    int count_katu;
    int count_miss;

    String rank;
    //created_at
    LocalDateTime date;

}
