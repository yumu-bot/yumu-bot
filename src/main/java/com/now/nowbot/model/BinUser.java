package com.now.nowbot.model;

import com.now.nowbot.model.enums.OsuMode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

public class BinUser {
    Long baseId;
    /**
     * osu name
     */
    String osuName;
    /**
     * 个人主页后面那串数
     */
    Long osuID;
    /**
     * 当前令牌
     */
    String accessToken;
    /**
     * 刷新令牌
     */
    String refreshToken;
    /**
     * 过期时间戳
     */
    Long time;
    /**
     * 主模式
     */
    OsuMode mode;

    public BinUser() {
        reTime();
    }

    public BinUser(long base) {
        this();
        baseId = base;
    }
    public BinUser(long osuId, String osuName) {
        this.osuID = osuId;
        this.osuName = osuName;
        mode = OsuMode.DEFAULT;
        time=0L;
        reTime();
    }

    public static BinUser create(String refreshToken) {
        var user = new BinUser();
        user.refreshToken = refreshToken;
        user.reTime();
        return user;
    }
    public String getOsuName() {
        return osuName;
    }

    public void setOsuName(String osuName) {
        this.osuName = osuName;
    }

    public Long getOsuID() {
        return osuID;
    }

    public void setOsuID(Long osuID) {
        this.osuID = osuID;
    }

    public void setOsuID(int osuID) {
        this.osuID = (long) osuID;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean isAuthorized() {
        boolean auth = false;
        try {
            // 请求 token ，如果过期会报 Unauthorized
            auth = Objects.nonNull(accessToken) && Objects.nonNull(time) && time > 0;
        } catch (WebClientResponseException.Unauthorized ignored) {

        }
        return auth;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void reTime() {
        time = System.currentTimeMillis();
    }

    public Long nextTime(Long addTime) {
        time = System.currentTimeMillis() + addTime * 1000;
        return time;
    }

    public boolean isPassed() {
        return System.currentTimeMillis() > time;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    /**
     * 记录在数据库中的 id, 非 uid
     *
     * @return base
     */
    public Long getBaseId() {
        return baseId;
    }

    @Override
    public String toString() {
        return "BinUser{" +
                ", osuName='" + osuName + '\'' +
                ", osuID='" + osuID + '\'' +
                ", refresh_token='" + refreshToken + '\'' +
                ", access_token='" + accessToken + '\'' +
                ", time=" + time +
                '}';
    }
}
