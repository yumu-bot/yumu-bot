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
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService;
import com.now.nowbot.throwable.serviceException.BindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BindDao {
    private final Set<Long>     UPDATE_USERS = new CopyOnWriteArraySet<>();
    private final AtomicBoolean NOW_UPDATE   = new AtomicBoolean(false);

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
        return getUserFromQQ(qq, false);
    }

    /**
     * 获取绑定的玩家
     * @param qq qq
     * @param isMyself 仅影响报错信息，不影响结果
     * @return 绑定的玩家
     */
    public BinUser getUserFromQQ(Long qq, boolean isMyself) throws BindException {
        if (qq < 0) {
            try {
                return getUserFromOsuID(-qq);
            } catch (BindException e) {
                return new BinUser(-qq, "unknown");
            }
        }
        var liteData = bindQQMapper.findById(qq);
        if (liteData.isEmpty()) {
            if (isMyself) {
                throw new BindException(BindException.Type.BIND_Me_NotBind);
            } else {
                throw new BindException(BindException.Type.BIND_Player_HadNotBind);
            }
        }
        var u = liteData.get().getOsuUser();
        return fromLite(u);
    }

    public BinUser getUserFromQQ(int qq) throws BindException {
        return getUserFromQQ((long) qq);
    }

    public BinUser saveBind(BinUser user) {
        OsuBindUserLite lite = new OsuBindUserLite(user);

        lite = bindUserMapper.save(lite);

        return fromLite(lite);
    }

    public BinUser getUserFromOsuID(Long osuId) throws BindException {
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
        OsuBindUserLite osuBind = user;
        if (user.getRefreshToken() != null) {
            var count = bindQQMapper.countByOsuId(user.getOsuID());
            if (count > 0) bindUserMapper.deleteAllByOsuId(user.getOsuID());
            osuBind = bindUserMapper.checkSave(osuBind);
        } else {
            Optional<OsuBindUserLite> buLiteOpt =bindUserMapper.getFirstByOsuId(user.getOsuID());
            if (buLiteOpt.isPresent()) {
                osuBind = buLiteOpt.get();
            } else {
                osuBind = bindUserMapper.checkSave(osuBind);
            }
        }

        var qqBind = new QQBindLite();
        qqBind.setQq(qq);

        qqBind.setOsuUser(osuBind);
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
        return data.map(BindDao::fromLite).orElse(null);
    }

    public BinUser getBindUser(Long id) {
        if (id == null) return null;
        var data = bindUserMapper.getByOsuId(id);
        return data.map(BindDao::fromLite).orElse(null);
    }

    public void updateToken(Long uid, String accessToken, String refreshToken, Long time) {
        if (NOW_UPDATE.get()) {
            UPDATE_USERS.add(uid);
        }
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
    public void  refreshOldUserToken(OsuUserApiService osuGetService) {
        NOW_UPDATE.set(true);
        UPDATE_USERS.clear();
        try {
            refreshOldUserTokenPack(osuGetService);
        } finally {
            UPDATE_USERS.clear();
            NOW_UPDATE.set(false);
        }
    }
    private void refreshOldUserTokenPack(OsuUserApiService osuGetService) {
        long now = System.currentTimeMillis();
        int succeedCount = 0;
        List<OsuBindUserLite> users;
        // 降低更新 token 时的优先级
        OsuApiBaseService.setPriority(10);
        // 更新暂时没失败过的
        while (!(users = bindUserMapper.getOldBindUser(now)).isEmpty()) {
            try {
                succeedCount += refreshOldUserList(osuGetService, users);
            } catch (RefreshException e) {
                succeedCount += e.successCount;
                log.error("连续失败, 停止更新, 更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
                return;
            }
        }
        // 重新尝试失败的
        while (!(users = bindUserMapper.getOldBindUserHasWrong(now)).isEmpty()) {
            try {
                succeedCount += refreshOldUserList(osuGetService, users);
            } catch (RefreshException e) {
                succeedCount += e.successCount;
                log.error("停止更新, 更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
                return;
            }
        }
        log.info("更新用户数量: [{}], 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000);
    }

    private int refreshOldUserList(OsuUserApiService osuGetService, List<OsuBindUserLite> users) {
        int errCount = 0;
        int succeedCount = 0;
        while (!users.isEmpty()) {
            var u = users.removeLast();

            if (UPDATE_USERS.remove(u.getID())) continue;
            if (ObjectUtils.isEmpty(u.getRefreshToken())) {
                bindUserMapper.backupBindByOsuId(u.getOsuID());
                continue;
            }
            // 出错超 5 次默认无法再次更新了
            if (u.getUpdateCount() > 5) {
                // 回退到用户名绑定
                bindUserMapper.backupBindByOsuId(u.getID());
            }
            log.info("更新用户 [{}]", u.getOsuName());
            try {
                refreshOldUserToken(u, osuGetService);
                if (u.getUpdateCount() > 0) bindUserMapper.clearUpdateCount(u.getID());
                errCount = 0;
            } catch (WebClientResponseException.Unauthorized e) {
                // 绑定被取消或者过期, 不再尝试
                bindUserMapper.backupBindByOsuId(u.getID());
            } catch (Exception e) {
                bindUserMapper.addUpdateCount(u.getID());
                errCount++;
            }
            if (errCount > 5) {
                // 一般连续错误意味着网络寄了
                throw new RefreshException(succeedCount);
            }
            succeedCount++;
        }
        return succeedCount;
    }

    private void refreshOldUserToken(OsuBindUserLite u, OsuUserApiService osuGetService) {
        int badRequest = 0;
        while (true) {
            try {
                osuGetService.refreshUserToken(fromLite(u));
                return;
            } catch (WebClientResponseException.Unauthorized e) {
                log.info("更新 [{}] 令牌失败, refresh token 失效, 绑定被取消", u.getOsuName());
                throw e;
            } catch (WebClientResponseException.BadRequest e) {
                badRequest++;
                if (badRequest < 3) {
                    log.error("更新 [{}] 令牌失败, api 服务器异常, 正在重试 {}", u.getOsuName(), badRequest);
                } else {
                    log.error("更新 [{}] 令牌失败, 重试 {} 次失败, 放弃更新", u.getOsuName(), badRequest);
                    throw new RuntimeException("network error");
                }
            }
        }
    }

    private static class RefreshException extends RuntimeException {
        int successCount;
        RefreshException(int i) {
            successCount = i;
        }
    }
}
