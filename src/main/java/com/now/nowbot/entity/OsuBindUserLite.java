package com.now.nowbot.entity;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_bind_user", indexes = {
        @Index(name = "bind_qid", columnList = "qq"),
        @Index(name = "bind_oid", columnList = "osu_id"),
})
public class OsuBindUserLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osu_id")
    private Long osuId;
    @Column(name = "osu_name")
    @Type(type = "org.hibernate.type.TextType")
    @Lob
    private String osuName;
    private Long qq;
    @Column(name = "access_token")
    @Type(type = "org.hibernate.type.TextType")
    @Lob
    private String accessToken;
    @Column(name = "refresh_token")
    @Type(type = "org.hibernate.type.TextType")
    @Lob
    private String refreshToken;
    private Long time;

    //一些额外信息
    //创号时间
    @Column(name = "join_date")
    private LocalDateTime joinDate;
    //主模式
    @Column(name = "main_mode")
    private OsuMode mainMode;

    public OsuBindUserLite(BinUser data) {
        this.joinDate = LocalDateTime.now();
        this.osuId = data.getOsuID();
        this.osuName = data.getOsuName();
        this.qq = data.getQq();
        this.accessToken = data.getAccessToken();
        this.refreshToken = data.getRefreshToken();
        this.time = data.getTime();
        this.mainMode = data.getMode();
    }

    public OsuBindUserLite() {
    }

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

    public OsuMode getMainMode() {
        return mainMode;
    }

    public void setMainMode(OsuMode mainMode) {
        this.mainMode = mainMode;
    }
}
