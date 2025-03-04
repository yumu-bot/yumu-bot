package com.now.nowbot.model;

import com.now.nowbot.model.enums.OsuMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

public class BindUser {
    private static final Logger log = LoggerFactory.getLogger(BindUser.class);

    /**
     * 看起来这个 ID 没用到，只有 BindQQ 那边的一个 User 才是真正的存储
     */
    Long baseId;
    /**
     * osu data
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

    public BindUser() {
        setTimeToNow();
    }

    public BindUser(long base) {
        this();
        baseId = base;
    }
    public BindUser(long osuID, String name) {
        this.osuID = osuID;
        this.osuName = name;
        mode = OsuMode.DEFAULT;
        time = 0L;
        setTimeToNow();
    }

    public static BindUser create(String refreshToken) {
        var user = new BindUser();
        user.refreshToken = refreshToken;
        user.setTimeToNow();
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

    public String getAccessToken() throws WebClientResponseException.Unauthorized {
        return accessToken;
    }

    // 是否绑定过
    public boolean isAuthorized() {
        boolean expired = true; // auth 的反
        try {
            // 请求 token ，如果过期会报 Unauthorized
            expired = time == null || time <= 0 || accessToken == null;
        } catch (Exception ignored) {
            log.info("玩家 {} 已掉绑", osuName);
        }
        return ! expired;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Nullable
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void setTimeToNow() {
        time = System.currentTimeMillis();
    }

    public Long setTimeToAfter(Long millis) {
        time = System.currentTimeMillis() + millis;
        return time;
    }

    public boolean isPassed() {
        return time != null && System.currentTimeMillis() > time;
    }

    @NonNull
    public OsuMode getOsuMode() {
        return mode != null ? mode : OsuMode.DEFAULT;
    }

    public void setOsuMode(OsuMode mode) {
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
        return baseId +
               "," + osuName +
               "," + osuID +
               "," + accessToken +
               "," + refreshToken +
               "," + time +
               "," + mode;
    }

    // 重写 equals 必须要重写 hashCode, 如果别的地方使用 HashSet/HashMap 会炸
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindUser bindUser)) return false;

        return osuName.equalsIgnoreCase(bindUser.osuName) || osuID.equals(bindUser.osuID);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(osuName);
        result = 31 * result + Objects.hashCode(osuID);
        return result;
    }

    public String getUsername() {
        return getOsuName();
    }
}
