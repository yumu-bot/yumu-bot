package com.now.nowbot.dao;

import com.now.nowbot.entity.QQUserLite;
import com.now.nowbot.mapper.BindMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.throwable.TipsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BindDao {
    Logger log = LoggerFactory.getLogger(BindDao.class);
    BindMapper bindMapper;

    @Autowired
    public BindDao(BindMapper mapper) {
        bindMapper = mapper;
    }

    public BinUser getUser(Long qq) throws TipsException {
        var liteData = bindMapper.getByQq(qq);
        if (liteData == null) throw new TipsException("当前用户未绑定");
        return fromLite(liteData);
    }

    public void saveUser(BinUser user) {
        var data = new QQUserLite(user);
        if (data.getMainMode() == null) data.setMainMode(OsuMode.OSU);
        bindMapper.save(data);
    }

    public void updateToken(Long uid, String accessToken, String refreshToken, Long time) {
        bindMapper.updateToken(uid, accessToken, refreshToken, time);
    }

    public void updateMod(Long uid, OsuMode mode) {
        bindMapper.updateMode(uid, mode);
    }

    public boolean unBind(BinUser user){
        try {
            bindMapper.unBind(user.getOsuID());
            return true;
        } catch (Exception e) {
            log.error("unbind error",e);
            return false;
        }
    }

    public Long getQQ(Long uid) {
        return bindMapper.getqq(uid);
    }

    //todo 一会切换到name表
    public Long getOsuId(String name) {
        var data = bindMapper.getByOsuNameLike('%' + name + '%');
        if (data != null && data.getOsuId() != null) return data.getOsuId();
        else return null;
    }
    public void removeOsuNameToId(Long osuId){

    }
    public void saveOsuNameToId(Long id, String... name){

    }

    public static BinUser fromLite(QQUserLite qqUserLite) {
        var data = qqUserLite;
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

    public static QQUserLite fromModel(BinUser user) {
        return new QQUserLite(user);
    }
}
