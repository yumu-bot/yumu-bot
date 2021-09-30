package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class QQUserLite {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "osu_id")
    private Long osuId;
    @Column(name = "osu_name")
    private String osuName;
    private Long qq;
    @Column(name = "access_token")
    private String accessToken;
    @Column(name = "refresh_token")
    private String refreshToken;
    private Long time;

    //一些额外信息
    //创号时间
    @Column(name = "join_date")
    private LocalDateTime joinDate;
    //主模式
    @Column(name = "play_mode")
    private String playMode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOsuId() {
        return osuId;
    }

    public void setOsuId(Long osuId) {
        this.osuId = osuId;
    }

    public String getOsuName() {
        return osuName;
    }

    public void setOsuName(String osuName) {
        this.osuName = osuName;
    }

    public Long getQq() {
        return qq;
    }

    public void setQq(Long qq) {
        this.qq = qq;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public String getPlayMode() {
        return playMode;
    }

    public void setPlayMode(String playMode) {
        this.playMode = playMode;
    }
}
