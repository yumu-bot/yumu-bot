package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.PPMinusException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("PPMINUS")
public class PPMinusService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(PPMinusService.class);
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;
    @Autowired
    ImageService imageService;

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(ppminus|(p?(pm))(?![a-rt-uw-zA-RT-UW-Z_]))\\s*(?<vs>vs)?\\s*([:：](?<mode>[\\w\\d]+))?(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }
        var from = event.getSubject();
        // 获得可能的 at
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        Ppm ppm;
        OsuUser user;
        List<Score> bps;
        var mode = OsuMode.getMode(matcher.group("mode"));
            // 不包含@ 分为查自身/查他人
        if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
            // 查他人
            try {
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = osuGetService.getPlayerInfo(id, mode);
                bps = osuGetService.getBestPerformance(id, mode, 0, 100);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_TokenExpired);
            } catch (HttpClientErrorException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_NotFound);
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Player_FetchFailed);
            }

            //默认无主模式
            if (mode == OsuMode.DEFAULT && user.getPlayMode() != null) mode = user.getPlayMode();

        } else if (at != null) {
            try {
                var binUser = bindDao.getUserFromQQ(at.getTarget());//处理默认mode
                if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
                user = osuGetService.getPlayerInfo(binUser, mode);
                bps = osuGetService.getBestPerformance(binUser, mode, 0, 100);

            } catch (HttpClientErrorException.Unauthorized e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_TokenExpired);
            } catch (HttpClientErrorException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_NotFound);
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Player_FetchFailed);
            }

        } else {
            try {
                var binUser = bindDao.getUserFromQQ(event.getSender().getId());//处理默认mode
                if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
                user = osuGetService.getPlayerInfo(binUser, mode);
                bps = osuGetService.getBestPerformance(binUser, mode, 0, 100);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Me_TokenExpired);
            } catch (HttpClientErrorException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Me_NotFound);
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Me_FetchFailed);
            }
        }

        if (user.getStatistics().getPlayTime() < 60 || user.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_PlayTimeTooShort);
        }

        ppm = Ppm.getInstance(mode, user, bps);

        try {
            var img = imageService.getPanelB1(user, mode, ppm);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("PPM 数据请求失败", e);
            throw new PPMinusException(PPMinusException.Type.PPM_Default_Error);
            //from.sendMessage("PPM 渲染图片超时，请重试。\n或尝试旧版渲染 !p2。");
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        // 获得可能的 at
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        OsuUser userMe;
        var userBin = bindDao.getUserFromQQ(event.getSender().getId());
        List<Score> bpListMe;
        OsuUser userOther;
        List<Score> bpListOther;
        Ppm ppmMe;
        Ppm ppmOther;

        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
        //自己的信息
        try {
            userMe = osuGetService.getPlayerInfo(userBin, mode);
            bpListMe = osuGetService.getBestPerformance(userBin, mode,0,100);
        } catch (BindException e) {
            throw new PPMinusException(PPMinusException.Type.PPM_Me_TokenExpired);
        }
        ppmMe = Ppm.getInstance(mode, userMe, bpListMe);

        if (at != null) {//被对比人的信息
            // 包含有@
            var b = bindDao.getUserFromQQ(at.getTarget());
            userOther = osuGetService.getPlayerInfo(b, mode);
            bpListOther = osuGetService.getBestPerformance(b, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            userOther = osuGetService.getPlayerInfo(id, mode);
            bpListOther = osuGetService.getBestPerformance(id, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_VSNotFound);
        }

        if (userOther.getStatistics().getPlayTime() < 60 || userOther.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_PlayTimeTooShort);
        }

        if (userMe.getStatistics().getPlayTime() < 60 || userMe.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Me_PlayTimeTooShort);
        }

        //你为啥不在数据库里存这些。。。
        if (userOther.getUID() == 17064371L){
            setUser(ppmOther, 999.99f);
        } else if (userOther.getUID().equals(19673275L)) {
            setUser(ppmOther, 0);
        }

        var data = imageService.getPanelB1(userMe, userOther, ppmMe, ppmOther, mode);
        QQMsgUtil.sendImage(from, data);
    }

    static void setUser(Ppm ppmOther, float value) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> PPMClass =  Class.forName("com.now.nowbot.model.PPm.Ppm");
        Field[] valueFiled = {
                PPMClass.getDeclaredField("value1"),
                PPMClass.getDeclaredField("value2"),
                PPMClass.getDeclaredField("value3"),
                PPMClass.getDeclaredField("value4"),
                PPMClass.getDeclaredField("value5"),
                PPMClass.getDeclaredField("value6"),
                PPMClass.getDeclaredField("value7"),
                PPMClass.getDeclaredField("value8"),
        };
        for (var i:valueFiled){
            i.setAccessible(true);
            i.set(ppmOther,value);
        }
    }
}
