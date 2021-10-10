package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_user", indexes = {
        @Index(name = "oid",columnList = "osu_id"),
        @Index(name = "oname",columnList = "username")
})
public class OsuUserLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "osu_id")
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

    //
    private Boolean is_supporter;
    private Integer follower_count;
    private Integer favourite_beatmapset_count;
    private String playmode;
    //上次登录时间
    LocalDateTime last_visit;
    private Boolean is_deleted;
    private Integer graveyard_beatmapset_count;
    private Boolean is_online;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOsuID() {
        return osuID;
    }

    public void setOsuID(Integer osuID) {
        this.osuID = osuID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getCountry_name() {
        return country_name;
    }

    public void setCountry_name(String country_name) {
        this.country_name = country_name;
    }

    public String getCustom_url() {
        return custom_url;
    }

    public void setCustom_url(String custom_url) {
        this.custom_url = custom_url;
    }

    public Integer getBeatmap_playcounts_count() {
        return beatmap_playcounts_count;
    }

    public void setBeatmap_playcounts_count(Integer beatmap_playcounts_count) {
        this.beatmap_playcounts_count = beatmap_playcounts_count;
    }

    public Integer getKudosu_total() {
        return kudosu_total;
    }

    public void setKudosu_total(Integer kudosu_total) {
        this.kudosu_total = kudosu_total;
    }

    public Integer getKudosu_available() {
        return kudosu_available;
    }

    public void setKudosu_available(Integer kudosu_available) {
        this.kudosu_available = kudosu_available;
    }

    public Integer getRanked_and_approved_beatmapset_count() {
        return ranked_and_approved_beatmapset_count;
    }

    public void setRanked_and_approved_beatmapset_count(Integer ranked_and_approved_beatmapset_count) {
        this.ranked_and_approved_beatmapset_count = ranked_and_approved_beatmapset_count;
    }

    public LocalDateTime getJoin_date() {
        return join_date;
    }

    public void setJoin_date(LocalDateTime join_date) {
        this.join_date = join_date;
    }

    public Integer getSupport_level() {
        return support_level;
    }

    public void setSupport_level(Integer support_level) {
        this.support_level = support_level;
    }

    public Integer getMapping_follower_count() {
        return mapping_follower_count;
    }

    public void setMapping_follower_count(Integer mapping_follower_count) {
        this.mapping_follower_count = mapping_follower_count;
    }

    public String getPrevious_usernames() {
        return previous_usernames;
    }

    public void setPrevious_usernames(String previous_usernames) {
        this.previous_usernames = previous_usernames;
    }

    public String getComments_count() {
        return comments_count;
    }

    public void setComments_count(String comments_count) {
        this.comments_count = comments_count;
    }

    public boolean isIs_supporter() {
        return is_supporter;
    }

    public void setIs_supporter(boolean is_supporter) {
        this.is_supporter = is_supporter;
    }

    public Integer getFollower_count() {
        return follower_count;
    }

    public void setFollower_count(Integer follower_count) {
        this.follower_count = follower_count;
    }

    public Integer getFavourite_beatmapset_count() {
        return favourite_beatmapset_count;
    }

    public void setFavourite_beatmapset_count(Integer favourite_beatmapset_count) {
        this.favourite_beatmapset_count = favourite_beatmapset_count;
    }

    public String getPlaymode() {
        return playmode;
    }

    public void setPlaymode(String playmode) {
        this.playmode = playmode;
    }

    public LocalDateTime getLast_visit() {
        return last_visit;
    }

    public void setLast_visit(LocalDateTime last_visit) {
        this.last_visit = last_visit;
    }

    public boolean isIs_deleted() {
        return is_deleted;
    }

    public void setIs_deleted(boolean is_deleted) {
        this.is_deleted = is_deleted;
    }

    public Integer getGraveyard_beatmapset_count() {
        return graveyard_beatmapset_count;
    }

    public void setGraveyard_beatmapset_count(Integer graveyard_beatmapset_count) {
        this.graveyard_beatmapset_count = graveyard_beatmapset_count;
    }

    public boolean isIs_online() {
        return is_online;
    }

    public void setIs_online(boolean is_online) {
        this.is_online = is_online;
    }
}
