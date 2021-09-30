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
    Integer osuID;
    // "country": {"code": "CN","name": "China"},            country_code
    String country_code;
    String country_name;

    // "cover": {custom_url}
    String custom_url;
    int beatmap_playcounts_count;
    // "kudosu": {"total": 0, "available": 0}
    int kudosu_total;
    int kudosu_available;

    int ranked_and_approved_beatmapset_count;
    private LocalDateTime join_date;

    int support_level;
    int mapping_follower_count;
    // 以','分隔的列表
    String previous_usernames;
    String comments_count; //评论数

}
