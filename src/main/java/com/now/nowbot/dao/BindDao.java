package com.now.nowbot.dao;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.entity.OsuNameToIdLite;
import com.now.nowbot.entity.bind.DiscordBindLite;
import com.now.nowbot.entity.bind.QQBindLite;
import com.now.nowbot.mapper.BindDiscordMapper;
import com.now.nowbot.mapper.BindQQMapper;
import com.now.nowbot.mapper.BindUserMapper;
import com.now.nowbot.mapper.OsuFindNameMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import jakarta.persistence.NonUniqueResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class BindDao {
    Logger log = LoggerFactory.getLogger(BindDao.class);
    BindUserMapper bindUserMapper;
    BindQQMapper bindQQMapper;
    BindDiscordMapper bindDiscordMapper;
    OsuFindNameMapper osuFindNameMapper;

    @Autowired
    public BindDao(BindUserMapper mapper, OsuFindNameMapper nameMapper, BindQQMapper QQMapper, BindDiscordMapper discordMapper) {
        bindUserMapper = mapper;
        osuFindNameMapper = nameMapper;
        bindQQMapper = QQMapper;
        bindDiscordMapper = discordMapper;
    }

    public BinUser getUserFromQQ(Long qq) throws BindException {
        var liteData = bindQQMapper.findById(qq);
        if (liteData.isEmpty()) {
            throw new BindException(BindException.Type.BIND_Me_TokenExpired);
        }

        return fromLite(liteData.get().getOsuUser());
    }

    public BinUser getUserFromQQ(int qq) throws BindException {
        return getUserFromQQ((long) qq);
    }

    public BinUser getUserFromOsuid(Long osuId) throws BindException {
        Optional<OsuBindUserLite> liteData = null;
        try {
            liteData = bindUserMapper.getByOsuId(osuId);
        } catch (IncorrectResultSizeDataAccessException e) {
            bindUserMapper.deleteOldByOsuId(osuId);
            liteData = bindUserMapper.getByOsuId(osuId);
        }
        if (liteData.isEmpty()) throw new BindException(BindException.Type.BIND_Player_NoBind);
        return fromLite(liteData.get());
    }

    public Optional<QQBindLite> getQQLiteFromOsuId(Long osuId) {
        return bindQQMapper.findByOsuId(osuId);
    }

    public Optional<QQBindLite> getQQLiteFromQQ(Long qq) {
        return bindQQMapper.findById(qq);
    }

    public QQBindLite bindQQ(Long qq, BinUser user) {
        return bindQQ(qq, fromModel(user));
    }

    public QQBindLite bindQQ(Long qq, OsuBindUserLite user) {
        bindQQMapper.deleteOtherBind(user.getOsuId(), qq);
        var qqBind = new QQBindLite();
        qqBind.setQq(qq);
        if (user.getRefreshToken() == null) {
            Optional<OsuBindUserLite> uLiteOpt = null;
            try {
                uLiteOpt = bindUserMapper.getByOsuId(user.getOsuId());
            } catch (NonUniqueResultException e) {
                // 查出多个
                bindUserMapper.deleteOldByOsuId(user.getOsuId());
                uLiteOpt = bindUserMapper.getByOsuId(user.getOsuId());
            }
            if (uLiteOpt.isPresent()) {
                var uLite = uLiteOpt.get();
                qqBind.setOsuUser(uLite);
                return bindQQMapper.save(qqBind);
            }
        }

        qqBind.setOsuUser(bindUserMapper.checkSave(user));
        return bindQQMapper.save(qqBind);
    }

    public DiscordBindLite bindDiscord(String discordId, BinUser user) {
        return bindDiscord(discordId, fromModel(user));
    }

    public DiscordBindLite bindDiscord(String discordId, OsuBindUserLite user) {
        var discordBind = new DiscordBindLite();
        discordBind.setId(discordId);
        discordBind.setOsuUser(user);
        return bindDiscordMapper.save(discordBind);
    }

    public BinUser getBindUser(String name) {
        var id = getOsuId(name);
        if (id == null) return null;
        var data = bindUserMapper.getByOsuId(id);
        return fromLite(data.orElseGet(null));
    }

    public void updateToken(Long uid, String accessToken, String refreshToken, Long time) {
        bindUserMapper.updateToken(uid, accessToken, refreshToken, time);
    }

    public void updateMod(Long uid, OsuMode mode) {
        bindUserMapper.updateMode(uid, mode);
    }

    public boolean unBindQQ(BinUser user) {
        try {
            bindQQMapper.unBind(user.getOsuID());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void removeBind(long uid) {
        bindUserMapper.deleteAllByOsuId(uid);
    }

    public Long getQQ(Long uid) {
        var q = bindQQMapper.findByOsuId(uid);
        if (q.isPresent()) return q.get().getQq();
        throw new RuntimeException("没有绑定");
    }

    public Long getOsuId(String name) {
        Long uid;
        try {
            uid = osuFindNameMapper.getFirstByNameOrderByIndex(name.toUpperCase()).getUid();
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                log.error("get name Error", e);
            }
            return null;
        }
        return uid;
    }


    public void removeOsuNameToId(Long osuId) {
        osuFindNameMapper.deleteByUid(osuId);
    }

    public void saveOsuNameToId(Long id, String... name) {
        if (name.length == 0) return;
        for (int i = 0; i < name.length; i++) {
            var x = new OsuNameToIdLite(id, name[i], i);
            osuFindNameMapper.save(x);
        }
    }

    public static BinUser fromLite(OsuBindUserLite osuBindUserLite) {
        if (osuBindUserLite == null) return null;
        var data = osuBindUserLite;
        var buser = new BinUser();
        buser.setOsuID(data.getOsuId());
        buser.setOsuName(data.getOsuName());
        buser.setAccessToken(data.getAccessToken());
        buser.setRefreshToken(data.getRefreshToken());
        buser.setTime(data.getTime());
        buser.setMode(data.getMainMode());
        return buser;
    }

    public static OsuBindUserLite fromModel(BinUser user) {
        if (user == null) return null;
        return new OsuBindUserLite(user);
    }

    @Async
    public void refreshOldUserToken(OsuUserApiService osuGetService) {
        long now = System.currentTimeMillis();
        List<OsuBindUserLite> users;
        int succeedCount = 0;
        int errCount = 0;

        while (!(users = bindUserMapper.getOldBindUser(now)).isEmpty()) {
            for (var u : users) {
                log.info("更新用户 [{}]", u.getOsuName());
                try {
                    refreshOldUserToken(fromLite(u), osuGetService);
                    errCount= 0;
                } catch (Exception e) {
                    errCount++;
                }
                if (errCount > 10) {
                    // 连续失败10次, 终止本次更新
                    log.error("连续失败, 停止更新, 更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
                    return;
                }
                succeedCount++;
            }
            try {
                Thread.sleep(Duration.ofSeconds(30));
            } catch (InterruptedException ignore) {
            }
        }
        log.info("更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
    }

    private void refreshOldUserToken(BinUser u, OsuUserApiService osuGetService) {
        int sleepSecond = 5;
        int badRequest = 0;
        while (true) {
            try {
                Thread.sleep(Duration.ofSeconds(sleepSecond));
                u.getAccessToken(osuGetService);
                return;
            } catch (WebClientResponseException.TooManyRequests e) {
                sleepSecond *= 2;
            } catch (WebClientResponseException.Unauthorized e) {
                log.info("更新 [{}] 令牌失败, refresh token 失效", u.getOsuName());
                removeBind(u.getOsuID());
                throw e;
            } catch (WebClientResponseException.BadRequest e) {
                badRequest++;
                if (badRequest > 6) {
                    log.error("更新 [{}] 令牌失败, 请求异常", u.getOsuName());
                    throw new RuntimeException("network error");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
