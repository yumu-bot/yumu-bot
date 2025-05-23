package com.now.nowbot.entity;

import com.now.nowbot.model.BindUser;
import com.now.nowbot.model.enums.OsuMode;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "osu_bind_user", indexes = {
        @Index(name = "bind_oid", columnList = "osu_id"),
})
public class OsuBindUserLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osu_id")
    private Long osuId;

    @Column(name = "osu_name", columnDefinition = "TEXT")
    private String osuName;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "update_count")
    private Integer updateCount = 0;
    private Long time;

    //一些额外信息
    //创号时间
    @Column(name = "join_date")
    private LocalDateTime joinDate;
    //主模式
    @Column(name = "main_mode")
    private OsuMode mainMode;

    public OsuBindUserLite(BindUser data) {
        this.joinDate = LocalDateTime.now();
        this.osuId = data.userID;
        this.osuName = data.username;
        this.accessToken = data.accessToken;
        this.refreshToken = data.getRefreshToken();
        this.time = data.time;
        this.mainMode = data.getMode();
        if (data.baseID != null) {
            this.id = data.baseID;
        }
    }

    public OsuBindUserLite() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getID() {
        return id;
    }

    public void setID(Long id) {
        this.id = id;
    }

    public Long getOsuID() {
        return osuId;
    }

    public void setOsuID(Long osuId) {
        this.osuId = osuId;
    }

    public String getOsuName() {
        return osuName;
    }

    public void setOsuName(String osuName) {
        this.osuName = osuName;
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

    public OsuMode getMainMode() {
        return mainMode;
    }

    public void setMainMode(OsuMode mainMode) {
        this.mainMode = mainMode;
    }

    public Integer getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(Integer updateCount) {
        this.updateCount = updateCount;
    }
}
