package com.now.nowbot.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.entity.OsuGroupConfigLite;
import com.now.nowbot.entity.OsuNameToIdLite;
import com.now.nowbot.entity.bind.DiscordBindLite;
import com.now.nowbot.entity.bind.QQBindLite;
import com.now.nowbot.mapper.*;
import com.now.nowbot.model.BindUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService;
import com.now.nowbot.throwable.serviceException.BindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class BindDao {
    private final Set<Long>     UPDATE_USERS = new CopyOnWriteArraySet<>();
    private final AtomicBoolean NOW_UPDATE   = new AtomicBoolean(false);

    Logger                   log = LoggerFactory.getLogger(BindDao.class);
    BindUserMapper           bindUserMapper;
    BindQQMapper             bindQQMapper;
    BindDiscordMapper        bindDiscordMapper;
    OsuFindNameMapper        osuFindNameMapper;
    OsuGroupConfigRepository osuGroupConfigRepository;

    private final Cache<String, Long> CAPTCHA_CACHE;
    private final Map<Long, String>   INDEX_CACHE = new ConcurrentHashMap<>();
    private final Random              random      = new Random();

    @Autowired
    public BindDao(
            BindUserMapper mapper,
            OsuFindNameMapper nameMapper,
            BindQQMapper QQMapper,
            BindDiscordMapper discordMapper,
            OsuGroupConfigRepository osuGroupConfigRepository
    ) {
        bindUserMapper = mapper;
        osuFindNameMapper = nameMapper;
        bindQQMapper = QQMapper;
        bindDiscordMapper = discordMapper;
        this.osuGroupConfigRepository = osuGroupConfigRepository;

        CAPTCHA_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .removalListener((_, id, _) -> {
                    INDEX_CACHE.remove(id);
                })
                .build();
    }

    public static BindUser fromLite(OsuBindUserLite buLite) {
        if (buLite == null) return null;
        var bu = new BindUser();
        bu.setBaseId(buLite.getID());
        bu.setOsuID(buLite.getOsuID());
        bu.setOsuName(buLite.getOsuName());
        bu.setAccessToken(buLite.getAccessToken());
        bu.setRefreshToken(buLite.getRefreshToken());
        bu.setTime(buLite.getTime());
        bu.setOsuMode(buLite.getMainMode());
        return bu;
    }

    private String generateRandomCode() {
        int code = 100000 + random.nextInt(900000); // 6位数
        return String.valueOf(code);
    }

    public String generateCaptcha(Long userId) {
        String oldCode = INDEX_CACHE.remove(userId);
        if (oldCode != null) {
            CAPTCHA_CACHE.invalidate(oldCode);
        }

        String code;
        do {
            code = generateRandomCode();
        } while (CAPTCHA_CACHE.getIfPresent(code) != null);

        CAPTCHA_CACHE.put(code, userId);
        INDEX_CACHE.put(userId, code);
        return code;
    }

    public BindUser getBindFromQQ(Long qq) throws BindException {
        return getBindFromQQ(qq, false);
    }

    /**
     * 获取绑定的玩家
     *
     * @param qq       qq
     * @param isMyself 仅影响报错信息，不影响结果
     * @return 绑定的玩家
     */
    public BindUser getBindFromQQ(Long qq, boolean isMyself) throws BindException {
        if (qq < 0) {
            try {
                return getBindUserFromOsuID(-qq);
            } catch (BindException e) {
                return new BindUser(-qq, "unknown");
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

    public BindUser getBindFromQQ(int qq) throws BindException {
        return getBindFromQQ((long) qq);
    }

    public BindUser saveBind(BindUser user) {
        if (user == null) {
            return null;
        }
        OsuBindUserLite lite = new OsuBindUserLite(user);
        lite = bindUserMapper.save(lite);
        return fromLite(lite);
    }

    public BindUser getBindUserFromOsuID(Long osuId) throws BindException {
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

    public List<OsuBindUserLite> getAllBindUser(Collection<Long> osuId) {
        return bindUserMapper.getAllByOsuId(osuId);
    }

    public Optional<QQBindLite> getQQLiteFromOsuId(Long osuId) {
        return bindQQMapper.findByOsuId(osuId);
    }

    public Optional<QQBindLite> getQQLiteFromQQ(Long qq) {
        return bindQQMapper.findById(qq);
    }

    public long verifyCaptcha(String code) {
        Long cachedUserId = CAPTCHA_CACHE.getIfPresent(code);
        if (cachedUserId != null) {
            CAPTCHA_CACHE.invalidate(code);
            INDEX_CACHE.remove(cachedUserId);
            return cachedUserId;
        }
        return -1;
    }

    public QQBindLite bindQQ(Long qq, OsuBindUserLite user) {
        OsuBindUserLite osuBind = user;
        if (user.getRefreshToken() != null) {
            var count = bindQQMapper.countByOsuId(user.getOsuID());
            if (count > 1) {
                bindUserMapper.deleteAllByOsuId(user.getOsuID());
            }
            osuBind = bindUserMapper.checkSave(osuBind);
        } else {
            Optional<OsuBindUserLite> buLiteOpt = bindUserMapper.getFirstByOsuId(user.getOsuID());
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

    public DiscordBindLite bindDiscord(String discordId, BindUser user) {
        return bindDiscord(discordId, fromModel(user));
    }

    public DiscordBindLite bindDiscord(String discordId, OsuBindUserLite user) {
        var discordBind = new DiscordBindLite();
        discordBind.setId(discordId);
        discordBind.setOsuUser(user);
        return bindDiscordMapper.save(discordBind);
    }

    public BindUser getBindUser(String name) {
        var id = getOsuID(name);
        if (id == null) return null;
        var data = bindUserMapper.getByOsuId(id);
        return data.map(BindDao::fromLite).orElse(null);
    }

    public BindUser getBindUser(Long id) {
        if (id == null) return null;
        var data = bindUserMapper.getByOsuId(id);
        return data.map(BindDao::fromLite).orElse(null);
    }

    public QQBindLite bindQQ(Long qq, BindUser user) {
        var data = bindUserMapper.getByOsuId(user.getOsuID());
        if (data.isEmpty()) {
            return bindQQ(qq, fromModel(user));
        } else {
            var userLite = data.get();
            userLite.setAccessToken(user.getAccessToken());
            userLite.setRefreshToken(user.getRefreshToken());
            userLite.setTime(user.getTime());
            userLite.setOsuName(user.getOsuName());
            return bindQQ(qq, userLite);
        }
    }

    public void updateToken(Long uid, String accessToken, String refreshToken, Long time) {
        if (NOW_UPDATE.get()) {
            UPDATE_USERS.add(uid);
        }
        bindUserMapper.updateToken(uid, accessToken, refreshToken, time);
    }

    public void updateMode(Long uid, OsuMode mode) {
        bindUserMapper.updateMode(uid, mode);
    }

    public boolean unBindQQ(BindUser user) {
        try {
            bindQQMapper.unBind(user.getOsuID());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 高危权限
     *
     * @param user 绑定
     * @return qq
     */
    public Long getQQ(BindUser user) {
        return getQQ(user.getOsuID());
    }

    public Long getQQ(Long osuID) {
        var qqBind = bindQQMapper.findByOsuId(osuID);

        if (qqBind.isPresent()) {
            return qqBind.get().getQq();
        } else {
            return -1L;
        }
    }

    @Nullable
    public QQBindLite getQQBindInfo(BindUser user) {
        return getQQBindInfo(user.getOsuID());
    }

    @Nullable
    public QQBindLite getQQBindInfo(Long osuID) {
        var qqBind = bindQQMapper.findByOsuId(osuID);

        return qqBind.orElse(null);
    }


    public void removeBind(long uid) {
        bindUserMapper.deleteAllByOsuId(uid);
    }

    public void backupBind(long uid) {
        bindUserMapper.backupBindByOsuId(uid);
    }

    public Long getOsuID(String name) {
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

    public BindUser getBindUserByDbId(Long id) {
        if (id == null) return null;
        var data = bindUserMapper.findById(id);
        return data.map(BindDao::fromLite).orElse(null);
    }

    public static OsuBindUserLite fromModel(BindUser user) {
        if (user == null) return null;
        return new OsuBindUserLite(user);
    }

    @Async
    public void refreshOldUserToken(OsuUserApiService osuGetService) {
        NOW_UPDATE.set(true);
        UPDATE_USERS.clear();
        try {
            refreshOldUserTokenOne(osuGetService);
//            refreshOldUserTokenPack(osuGetService);
        } catch (Exception e) {
            if (!(e instanceof WebClientResponseException.Unauthorized)) {
                log.error("更新用户出现异常", e);
            }
        } finally {
            UPDATE_USERS.clear();
            NOW_UPDATE.set(false);
        }
    }

    private void refreshOldUserTokenOne(OsuUserApiService osuGetService) {
        long now = System.currentTimeMillis();
        var user = bindUserMapper.getOneOldBindUser(now);
        if (user.isPresent()) {
            var u = user.get();
            if (UPDATE_USERS.remove(u.getID())) return;

            if (ObjectUtils.isEmpty(u.getRefreshToken())) {
                bindUserMapper.backupBindByOsuId(u.getOsuID());
                return;
            }

            log.info("更新用户: [{}]", u.getOsuName());
            refreshOldUserToken(u, osuGetService);
            return;
        }

        user = bindUserMapper.getOneOldBindUserHasWrong(now);
        if (user.isPresent()) {
            var u = user.get();
            if (UPDATE_USERS.remove(u.getID())) return;
            if (ObjectUtils.isEmpty(u.getRefreshToken())) {
                bindUserMapper.backupBindByOsuId(u.getOsuID());
                return;
            }
            // 出错超 5 次默认无法再次更新了
            if (u.getUpdateCount() > 5) {
                bindUserMapper.backupBindByOsuId(u.getID());
            }

            log.info("更新用户: [{}]", u.getOsuName());
            refreshOldUserToken(u, osuGetService);
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
                log.info("{} 取消绑定", u.getOsuName());
                bindUserMapper.backupBindByOsuId(u.getID());
            } catch (Exception e) {
                bindUserMapper.addUpdateCount(u.getID());
                log.error("出现异常 [{}]: ", errCount, e);
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
                try {
                    osuGetService.refreshUserToken(fromLite(u));
                } catch (Exception e) {
                    if (e instanceof WebClientResponseException) {
                        throw e;
                    } else {
                        throw e.getCause();
                    }
                }
                return;
            } catch (WebClientResponseException.Unauthorized e) {
                log.info("更新 [{}] 令牌失败, refresh token 失效, 绑定被取消", u.getOsuName());
                bindUserMapper.backupBindByOsuId(u.getOsuID());
                throw e;
            } catch (WebClientResponseException.BadRequest e) {
                badRequest++;
                if (badRequest < 3) {
                    log.error("更新 [{}] 令牌失败, api 服务器异常, 正在重试 {}", u.getOsuName(), badRequest);
                } else {
                    log.error("更新 [{}] 令牌失败, 重试 {} 次失败, 放弃更新", u.getOsuName(), badRequest);
                    throw e;
                }
            } catch (Throwable e) {
                log.error("神秘错误: ", e);
            }
        }
    }

    public Map<Long, OsuMode> getAllGroupMode() {
        return osuGroupConfigRepository
                .findAll()
                .stream()
                .collect(Collectors.toMap(OsuGroupConfigLite::getGroupId, it -> Optional.ofNullable(it.getMainMode()).orElse(OsuMode.DEFAULT)));
    }

    public OsuMode getGroupModeConfig(@Nullable MessageEvent event) {
        if (event == null || !(event.getSubject() instanceof Group group)) {
            return OsuMode.DEFAULT;
        }

        var config = osuGroupConfigRepository.findById(group.getId());
        return config.map(OsuGroupConfigLite::getMainMode).orElse(OsuMode.DEFAULT);
    }

    public List<Long> getAllUserIdLimit50(int start) {
        return bindUserMapper.getAllBindUserIdLimit50(start);
    }

    public List<QQBindLite.QQUser> getAllQQBindUser(Collection<Long> qqId) {
        return bindQQMapper.findAllUserByQQ(qqId);
    }

    public void saveGroupModeConfig(long groupId, OsuMode mode) {
        if (OsuMode.isDefaultOrNull(mode)) {
            osuGroupConfigRepository.deleteById(groupId);
        } else {
            osuGroupConfigRepository.save(new OsuGroupConfigLite(groupId, mode));
        }
    }

    private static class RefreshException extends RuntimeException {
        int successCount;

        RefreshException(int i) {
            successCount = i;
        }
    }
}
