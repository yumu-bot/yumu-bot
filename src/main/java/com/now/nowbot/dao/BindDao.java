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
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BindDao {
    private Set<Long> WAIT_UPDATE_USERS = new HashSet<>();
    Logger            log = LoggerFactory.getLogger(BindDao.class);
    BindUserMapper    bindUserMapper;
    BindQQMapper      bindQQMapper;
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
            throw new BindException(BindException.Type.BIND_Me_NotBind);
        }
        var u = liteData.get().getOsuUser();
        // 此处防止全局更新中再次被更新
        WAIT_UPDATE_USERS.remove(u.getID());
        return fromLite(u);
    }

    public BinUser getUserFromQQ(int qq) throws BindException {
        return getUserFromQQ((long) qq);
    }

    public BinUser getUserFromOsuid(Long osuId) throws BindException {
        if (Objects.isNull(osuId)) throw new BindException(BindException.Type.BIND_Receive_NoName);

        Optional<OsuBindUserLite> liteData;
        try {
            liteData = bindUserMapper.getByOsuId(osuId);
        } catch (IncorrectResultSizeDataAccessException e) {
            bindUserMapper.deleteOldByOsuId(osuId);
            liteData = bindUserMapper.getByOsuId(osuId);
        }

        if (liteData.isEmpty()) throw new BindException(BindException.Type.BIND_Player_NoBind);
        var u = liteData.get();
        // 此处防止全局更新中再次被更新
        WAIT_UPDATE_USERS.remove(u.getID());
        return fromLite(u);
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
        bindQQMapper.deleteOtherBind(user.getOsuID(), qq);
        var qqBind = new QQBindLite();
        qqBind.setQq(qq);
        if (user.getRefreshToken() == null) {
            Optional<OsuBindUserLite> buLiteOpt;
            try {
                buLiteOpt = bindUserMapper.getByOsuId(user.getOsuID());
            } catch (NonUniqueResultException e) {
                // 查出多个
                bindUserMapper.deleteOldByOsuId(user.getOsuID());
                buLiteOpt = bindUserMapper.getByOsuId(user.getOsuID());
            }
            if (buLiteOpt.isPresent()) {
                var uLite = buLiteOpt.get();
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

    public void backupBind(long uid) {
        bindUserMapper.backupBindByOsuId(uid);
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
                log.error("get data Error", e);
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

    public static BinUser fromLite(OsuBindUserLite buLite) {
        if (buLite == null) return null;
        var binUser = new BinUser();
        binUser.setOsuID(buLite.getOsuID());
        binUser.setOsuName(buLite.getOsuName());
        binUser.setAccessToken(buLite.getAccessToken());
        binUser.setRefreshToken(buLite.getRefreshToken());
        binUser.setTime(buLite.getTime());
        binUser.setOsuMode(buLite.getMainMode());
        return binUser;
    }

    public static OsuBindUserLite fromModel(BinUser user) {
        if (user == null) return null;
        return new OsuBindUserLite(user);
    }

    @Async
    public void refreshOldUserToken(OsuUserApiService osuGetService) {
        long now = System.currentTimeMillis() + 6*60*60;
        int succeedCount = 0;
        int errCount = 0;
        List<OsuBindUserLite> users;
        // 更新暂时没失败过的
        while (!(users = bindUserMapper.getOldBindUser(now)).isEmpty()) {
            OsuBindUserLite u;
            WAIT_UPDATE_USERS = Collections.synchronizedSet(users.stream().map(OsuBindUserLite::getID).collect(Collectors.toSet()));
            while (!users.isEmpty()) {
                u = users.removeLast();
                if (!WAIT_UPDATE_USERS.remove(u.getID())) continue;
                if (ObjectUtils.isEmpty(u.getRefreshToken())) {
                    bindUserMapper.backupBindByOsuId(u.getOsuID());
                    continue;
                }
                log.info("更新用户 [{}]", u.getOsuName());
                try {
                    refreshOldUserToken(u, osuGetService);
                    errCount = 0;
                } catch (Exception e) {
                    bindUserMapper.addUpdateCount(u.getID());
                    errCount++;
                    sleepNoException(Duration.ofSeconds(errCount * 5L));
                }
                if (errCount > 5) {
                    // 一般连续错误意味着网络寄了
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
        // 重新尝试失败的
        while (! (users = bindUserMapper.getOldBindUserHasWrong(now)).isEmpty()) {
            OsuBindUserLite u;
            WAIT_UPDATE_USERS = Collections.synchronizedSet(users.stream().map(OsuBindUserLite::getID).collect(Collectors.toSet()));
            while (! users.isEmpty()) {
                u = users.removeLast();
                if (! WAIT_UPDATE_USERS.remove(u.getID())) continue;
                if (ObjectUtils.isEmpty(u.getRefreshToken())) {
                    bindUserMapper.backupBindByOsuId(u.getOsuID());
                    continue;
                }
                // 出错超15次默认无法再次更新了
                if (u.getUpdateCount() > 15) {
                    // 回退到用户名绑定
                    bindUserMapper.backupBindByOsuId(u.getID());
                }
                log.info("更新历史失败用户 [{}]", u.getOsuName());
                try {
                    refreshOldUserToken(u, osuGetService);
                    bindUserMapper.clearUpdateCount(u.getID());
                    errCount = 0;
                } catch (Exception e) {
                    bindUserMapper.addUpdateCount(u.getID());
                    errCount++;
                    sleepNoException(Duration.ofSeconds(errCount * 10L));
                }
                if (errCount > 15) {
                    // 连续失败15次, 终止本次更新
                    log.error("连续失败, 停止更新, 更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
                    return;
                }
                succeedCount++;
            }
            sleepNoException(Duration.ofSeconds(30));
        }
        log.info("更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
    }

    private void refreshOldUserToken(OsuBindUserLite u, OsuUserApiService osuGetService) {
        int sleepSecond = 5;
        int badRequest = 0;
        while (true) {
            try {
                Thread.sleep(Duration.ofSeconds(sleepSecond));
                osuGetService.refreshUserToken(fromLite(u));
                return;
            } catch (HttpClientErrorException.TooManyRequests | WebClientResponseException.TooManyRequests e) {
                sleepSecond *= 2;
            } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
                log.info("更新 [{}] 令牌失败, refresh token 失效, 不做任何处理", u.getOsuName());
                throw e;
            } catch (HttpClientErrorException.BadRequest | WebClientResponseException.BadRequest e) {
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

    private static void sleepNoException(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignore) {
        }
    }
}
