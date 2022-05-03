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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "osu_id")
    private Integer osuID;
    @Column(name = "username")
    private String userName;
    // "country": {"code": "CN","name": "China"},            country_code
    private String countryCode;
    private String countryName;

    // "cover": {custom_url}
    private String customUrl;
    private Integer beatmapPlaycountsCount;
    // "kudosu": {"total": 0, "available": 0}
    private Integer kudosuTotal;
    private Integer kudosuAvailable;

    private Integer rankedAndApprovedBeatmapsetCount;
    private LocalDateTime joinDate;

    private Integer supportLevel;
    private Integer mappingFollowerCount;
    // 以','分隔的列表
    @Column(length = 1000)
    private String previousUsernames;
    private Integer commentsCount; //评论数

    //
    private Boolean isSupporter;
    private Integer followerCount;
    private Integer favouriteBeatmapsetCount;
    private String playmode;
    //上次登录时间
    LocalDateTime lastVisit;
    private Boolean isDeleted;
    private Integer graveyardBeatmapsetCount;

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String username) {
        this.userName = username;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String country_code) {
        this.countryCode = country_code;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String country_name) {
        this.countryName = country_name;
    }

    public String getCustomUrl() {
        return customUrl;
    }

    public void setCustomUrl(String custom_url) {
        this.customUrl = custom_url;
    }

    public Integer getBeatmapPlaycountsCount() {
        return beatmapPlaycountsCount;
    }

    public void setBeatmapPlaycountsCount(Integer beatmap_playcounts_count) {
        this.beatmapPlaycountsCount = beatmap_playcounts_count;
    }

    public Integer getKudosuTotal() {
        return kudosuTotal;
    }

    public void setKudosuTotal(Integer kudosu_total) {
        this.kudosuTotal = kudosu_total;
    }

    public Integer getKudosuAvailable() {
        return kudosuAvailable;
    }

    public void setKudosuAvailable(Integer kudosu_available) {
        this.kudosuAvailable = kudosu_available;
    }

    public Integer getRankedAndApprovedBeatmapsetCount() {
        return rankedAndApprovedBeatmapsetCount;
    }

    public void setRankedAndApprovedBeatmapsetCount(Integer ranked_and_approved_beatmapset_count) {
        this.rankedAndApprovedBeatmapsetCount = ranked_and_approved_beatmapset_count;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime join_date) {
        this.joinDate = join_date;
    }

    public Integer getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(Integer support_level) {
        this.supportLevel = support_level;
    }

    public Integer getMappingFollowerCount() {
        return mappingFollowerCount;
    }

    public void setMappingFollowerCount(Integer mapping_follower_count) {
        this.mappingFollowerCount = mapping_follower_count;
    }

    public String getPreviousUsernames() {
        return previousUsernames;
    }

    public void setPreviousUsernames(String previous_usernames) {
        this.previousUsernames = previous_usernames;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer comments_count) {
        this.commentsCount = comments_count;
    }

    public boolean isIsSupporter() {
        return isSupporter;
    }

    public void setIsSupporter(boolean is_supporter) {
        this.isSupporter = is_supporter;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer follower_count) {
        this.followerCount = follower_count;
    }

    public Integer getFavouriteBeatmapsetCount() {
        return favouriteBeatmapsetCount;
    }

    public void setFavouriteBeatmapsetCount(Integer favourite_beatmapset_count) {
        this.favouriteBeatmapsetCount = favourite_beatmapset_count;
    }

    public String getPlaymode() {
        return playmode;
    }

    public void setPlaymode(String playmode) {
        this.playmode = playmode;
    }

    public LocalDateTime getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(LocalDateTime last_visit) {
        this.lastVisit = last_visit;
    }

    public boolean isIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean is_deleted) {
        this.isDeleted = is_deleted;
    }

    public Integer getGraveyardBeatmapsetCount() {
        return graveyardBeatmapsetCount;
    }

    public void setGraveyardBeatmapsetCount(Integer graveyard_beatmapset_count) {
        this.graveyardBeatmapsetCount = graveyard_beatmapset_count;
    }

}
