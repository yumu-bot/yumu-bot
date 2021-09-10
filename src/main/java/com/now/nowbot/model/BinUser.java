package com.now.nowbot.model;

import com.now.nowbot.service.OsuGetService;

public class BinUser {
    //qq号
    long qq;
    //osu id
    String osuName;
    //个人主页后面那串数
    int osuID;
    //当前令牌
    String accessToken;
    //刷新令牌
    String refreshToken;
    //过期时间戳
    long time;

    public BinUser(){
        reTime();
    }

    public BinUser(long qq, String refreshToken) {
        this.qq = qq;
        this.refreshToken = refreshToken;
        reTime();
    }

    public long getQq() {
        return qq;
    }

    public void setQq(long qq) {
        this.qq = qq;
    }

    public String getOsuName() {
        return osuName;
    }

    public void setOsuName(String osuName) {
        this.osuName = osuName;
    }

    public int getOsuID() {
        return osuID;
    }

    public void setOsuID(int osuID) {
        this.osuID = osuID;
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
        if(isPassed())
            service.refreshToken(this);
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void reTime(){
        time = System.currentTimeMillis();
    }
    public void nextTime(long addTime){
        time = System.currentTimeMillis() + addTime * 1000;
    }

    public boolean isPassed(){
        return System.currentTimeMillis() > time;
    }

    @Override
    public String toString() {
        return "BinUser{" +
                "qq=" + qq +
                ", osuName='" + osuName + '\'' +
                ", osuID='" + osuID + '\'' +
                ", refresh_token='" + refreshToken + '\'' +
                ", access_token='" + accessToken + '\'' +
                ", time=" + time +
                '}';
    }
}
