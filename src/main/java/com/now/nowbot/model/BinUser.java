package com.now.nowbot.model;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;

public class BinUser {
    /**
     * qq号
     */
    Long qq;
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

    public BinUser(long qq, String refreshToken) {
        this.qq = qq;
        this.refreshToken = refreshToken;
        reTime();
    }

    public Long getQq() {
        return qq;
    }

    public void setQq(Long qq) {
        this.qq = qq;
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

    public String getAccessToken(OsuGetService service) {
        if (accessToken == null) {
            return service.getToken();
        } else if (isPassed()) {
            accessToken = service.refreshToken(this).findValue("access_token").asText();
        }

        return accessToken;
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

    @Override
    public String toString() {
        return "BinUser{" +
                "QQ=" + qq +
                ", osuName='" + osuName + '\'' +
                ", osuID='" + osuID + '\'' +
                ", refresh_token='" + refreshToken + '\'' +
                ", access_token='" + accessToken + '\'' +
                ", time=" + time +
                '}';
    }
}
