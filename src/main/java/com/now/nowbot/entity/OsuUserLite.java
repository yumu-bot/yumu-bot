package com.now.nowbot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu")
public class OsuUserLite {
    @Id
    @Column(name = "id")
    private Integer osuID;
    private String username;
    // "country": {"code": "CN","name": "China"},            country_code
    private String country_code;
    private String country_name;

    // "cover": {custom_url}
    private String custom_url;
    private Integer beatmap_playcounts_count;
    // "kudosu": {"total": 0, "available": 0}
    private Integer kudosu_total;
    private Integer kudosu_available;

    private Integer ranked_and_approved_beatmapset_count;
    private LocalDateTime join_date;

    private Integer support_level;
    private Integer mapping_follower_count;
    // 以','分隔的列表
    private String previous_usernames;
    private String comments_count; //评论数

    //四模式不同
    private String rank_history_osu;
    private String rank_history_taoko;
    private String rank_history_catch;
    private String rank_history_mania;

    //
    boolean is_supporter;
    private Integer follower_count;
    private Integer favourite_beatmapset_count;
    private String playmode;
    //上次登录时间
    LocalDateTine last_visit;
    boolean is_deleted;
    private Integer graveyard_beatmapset_count;
    boolean is_online;

    //等级
    private Integer level_current;
    private Integer level_progress;
    //rank
    private Integer global_rank;
    private Integer country_rank;

    private Float pp;
    private Float hit_accuracy;

    private Integer play_count;
    private Long play_time;

    private Long ranked_score;
    private Long total_score;

    private Integer maximum_combo;
    boolean is_ranked;

    private Integer grade_counts_ss;
    private Integer grade_counts_ssh;
    private Integer grade_counts_s;
    private Integer grade_counts_sh;
    private Integer grade_counts_a;


}
