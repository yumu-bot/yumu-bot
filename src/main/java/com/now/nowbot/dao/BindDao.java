package com.now.nowbot.dao;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.entity.OsuNameToIdLite;
import com.now.nowbot.entity.bind.DiscordBindLite;
import com.now.nowbot.entity.bind.QQBindLite;
import com.now.nowbot.mapper.BindQQMapper;
import com.now.nowbot.mapper.BindUserMapper;
import com.now.nowbot.mapper.DiscordBindMapper;
import com.now.nowbot.mapper.OsuFindNameMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.throwable.ServiceException.BindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BindDao {
    Logger            log = LoggerFactory.getLogger(BindDao.class);
    BindUserMapper    bindUserMapper;
    BindQQMapper      bindQQMapper;
    DiscordBindMapper discordBindMapper;
    OsuFindNameMapper osuFindNameMapper;

    @Autowired
    public BindDao(BindUserMapper mapper, OsuFindNameMapper nameMapper) {
        bindUserMapper = mapper;
        osuFindNameMapper = nameMapper;
    }

    public BinUser getUser(Long qq) throws BindException {
        var liteData = bindQQMapper.findById(qq);
        if (liteData.isEmpty()) {
            throw new BindException(BindException.Type.BIND_Me_NoBind);
        }

        return fromLite(liteData.get().getOsuUser());
    }

    public BinUser getUser(int qq) throws BindException {
        return getUser((long) qq);
    }

    public BinUser getUserFromOsuid(Long osuId) throws BindException {
        var liteData = bindUserMapper.getByOsuId(osuId);
        if (liteData.isEmpty()) throw new BindException(BindException.Type.BIND_Player_NoBind);
        return fromLite(liteData.get());
    }

    public Optional<QQBindLite> getQQLiteFromOsuId(Long osuId) {
        return bindQQMapper.findByOsuId(osuId);
    }

    public QQBindLite bindQQ(Long qq, BinUser user) {
        return bindQQ(qq, fromModel(user));
    }

    public QQBindLite bindQQ(Long qq, OsuBindUserLite user) {
        bindQQMapper.deleteOtherBind(user.getOsuId(), qq);
        var qqBind = new QQBindLite();
        qqBind.setQq(qq);
        if (user.getRefreshToken() == null) {
            var uLiteOpt = bindUserMapper.getByOsuId(user.getOsuId());
            if (uLiteOpt.isPresent()) {
                var uLite = uLiteOpt.get();
                qqBind.setOsuUser(uLite);
                return bindQQMapper.save(qqBind);
            }
        }

        qqBind.setOsuUser(user);
        return bindQQMapper.save(qqBind);
    }

    public DiscordBindLite bindDiscord(String discordId, BinUser user) {
        return bindDiscord(discordId,fromModel(user));
    }
    public DiscordBindLite bindDiscord(String discordId, OsuBindUserLite user) {
        var discordBind = new DiscordBindLite();
        discordBind.setId(discordId);
        discordBind.setOsuUser(user);
        return discordBindMapper.save(discordBind);
    }

    public BinUser getBindUser(String name) {
        var id = getOsuId(name);
        if (id == null) return null;
        var data = bindUserMapper.getByOsuId(id);
        return fromLite(data.orElseGet(null));
    }

    public void saveUser(Long qqId, String name, Long osuId) {
        var data = new BinUser();
        data.setMode(OsuMode.OSU);
        data.setQq(qqId);
        data.setOsuName(name);
        data.setOsuID(osuId);
        data.setTime(0L);
        saveUser(data);
    }

    public void saveUser(BinUser user) {
        var data = new OsuBindUserLite(user);
        try {
            var t = bindUserMapper.getByOsuId(user.getOsuID());
            if (t.isPresent()) data.setId(t.get().getId());
            else {
                throw new BindException(BindException.Type.BIND_Player_NotFound);
            }
        } catch (Exception e) {
            // do nothing
        }
        if (data.getMainMode() == null) data.setMainMode(OsuMode.OSU);
        bindUserMapper.save(data);
    }

    public void update(QQBindLite user) {
        bindQQMapper.save(user);
    }
    public void update(DiscordBindLite user) {
        discordBindMapper.save(user);
    }

    public void update(OsuBindUserLite user) {
        bindUserMapper.save(user);
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
//        buser.setQq(data.getQq());
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
}
