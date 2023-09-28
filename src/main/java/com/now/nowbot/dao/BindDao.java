package com.now.nowbot.dao;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.entity.OsuNameToIdLite;
import com.now.nowbot.mapper.BindMapper;
import com.now.nowbot.mapper.OsuFindNameMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.throwable.ServiceException.BindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BindDao {
    Logger log = LoggerFactory.getLogger(BindDao.class);
    BindMapper bindMapper;
    OsuFindNameMapper osuFindNameMapper;

    @Autowired
    public BindDao(BindMapper mapper, OsuFindNameMapper nameMapper) {
        bindMapper = mapper;
        osuFindNameMapper = nameMapper;
    }

    public BinUser getUser(Long qq) throws BindException {
        var liteData = bindMapper.getByQq(qq);
        if (liteData.isEmpty()) {
            return null;
            //throw new BindException(BindException.Type.BIND_Me_NoBind);
        } else if (liteData.size() > 1) {
            var liteLastUser = bindMapper.getFirstByQqOrderByTimeDesc(qq);
            bindMapper.deleteByQqAndIdNot(liteLastUser.getQq(),liteLastUser.getId());
            return fromLite(liteLastUser);
        }
        return fromLite(liteData.get(0));
    }
    public BinUser getUser(int qq) throws BindException {
        return getUser((long) qq);
    }
    public BinUser getUserFromOsuid(Long osuId) throws BindException {
        var liteData = bindMapper.getByOsuId(osuId);
        if (liteData == null) throw new BindException(BindException.Type.BIND_Player_NoBind);
        return fromLite(liteData);
    }
    public OsuBindUserLite getUserLiteFromOsuid(Long osuId) throws BindException {
        var liteData = bindMapper.getByOsuId(osuId);
        if (liteData == null) throw new BindException(BindException.Type.BIND_Player_NoBind);
        return liteData;
    }

    public BinUser getBindUser(String name) {
        var id = getOsuId(name);
        if (id == null) return null;
        var data = bindMapper.getByOsuId(id);
        return fromLite(data);
    }

    public void saveUser(Long qqId,String name, Long osuId){
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
            var t = bindMapper.getByOsuId(user.getOsuID());
            if (t != null) data.setId(t.getId());
            else {
                throw new BindException(BindException.Type.BIND_Player_NotFound);
            }
        } catch (Exception e) {
            // do nothing
        }
        if (data.getMainMode() == null) data.setMainMode(OsuMode.OSU);
        bindMapper.save(data);
    }

    public void update(OsuBindUserLite user){
        bindMapper.save(user);
    }
    public void updateToken(Long uid, String accessToken, String refreshToken, Long time) {
        bindMapper.updateToken(uid, accessToken, refreshToken, time);
    }

    public void updateMod(Long uid, OsuMode mode) {
        bindMapper.updateMode(uid, mode);
    }

    public boolean unBind(BinUser user) {
        try {
            bindMapper.unBind(user.getOsuID());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void removeBind(long uid){
        bindMapper.deleteAllByOsuId(uid);
    }

    public Long getQQ(Long uid) {
        return bindMapper.getqq(uid);
    }

    //todo 一会切换到name表
    public Long getOsuId(String name) {
        Long uid;
        try {
            uid = osuFindNameMapper.getFirstByNameOrderByIndex(name.toUpperCase()).getUid();
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                log.error("get name Error",e);
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
        buser.setQq(data.getQq());
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
