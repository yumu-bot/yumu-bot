package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.PPMinusException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;

@Service("PP_MINUS")
public class PPMinusService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(PPMinusService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.PP_MINUS.matcher(messageText);
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
        PPMinus ppMinus;
        OsuUser user;
        List<Score> bps;
        var mode = OsuMode.getMode(matcher.group("mode"));
        // 在新人群管理群里查询则固定为 osu模式
        if (event.getSubject().getId() == 695600319L && OsuMode.DEFAULT.equals(mode)) {
            mode = OsuMode.OSU;
        }
            // 不包含@ 分为查自身/查他人
        if (StringUtils.hasText(matcher.group("name"))) {
            // 查他人
            try {
                var id = userApiService.getOsuId(matcher.group("name").trim());
                user = userApiService.getPlayerInfo(id, mode);
                bps = scoreApiService.getBestPerformance(id, mode, 0, 100);
            } catch (WebClientResponseException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_NotFound);
            } catch (BindException e) {
                throw e;
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Player_FetchFailed);
            }

            //默认无主模式
            if (mode == OsuMode.DEFAULT && user.getPlayMode() != null) mode = user.getOsuMode();

        } else if (at != null) {
            try {
                var binUser = bindDao.getUserFromQQ(at.getTarget());//处理默认mode
                if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
                user = userApiService.getPlayerInfo(binUser, mode);
                bps = scoreApiService.getBestPerformance(binUser, mode, 0, 100);

            } catch (WebClientResponseException.Unauthorized e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_TokenExpired);
            } catch (WebClientResponseException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_NotFound);
            } catch (BindException e) {
                throw e;
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Player_FetchFailed);
            }

        } else {
            try {
                var binUser = bindDao.getUserFromQQ(event.getSender().getId());//处理默认mode
                if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
                user = userApiService.getPlayerInfo(binUser, mode);
                bps = scoreApiService.getBestPerformance(binUser, mode, 0, 100);
            } catch (WebClientResponseException.Unauthorized e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Me_TokenExpired);
            } catch (WebClientResponseException.NotFound e) {
                throw new PPMinusException(PPMinusException.Type.PPM_Me_NotFound);
            } catch (BindException e) {
                throw e;
            } catch (Exception e) {
                log.error("PPM 获取失败：", e);
                throw new PPMinusException(PPMinusException.Type.PPM_Me_FetchFailed);
            }
        }

        if (user.getStatistics().getPlayTime() < 60 || user.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_PlayTimeTooShort);
        }

        try {
            ppMinus = PPMinus.getInstance(mode, user, bps);
        } catch (Exception e) {
            log.error("PPM 数据计算失败", e);
            throw new PPMinusException(PPMinusException.Type.PPM_Calculate_Error);
        }


        try {
            var image = imageService.getPanelB1(user, mode, ppMinus);
            from.sendImage(image);
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
        var binUser = bindDao.getUserFromQQ(event.getSender().getId());

        List<Score> bpListMe;
        OsuUser userOther;
        List<Score> bpListOther;
        PPMinus PPMinusMe;
        PPMinus PPMinusOther;

        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
        //自己的信息
        try {
            userMe = userApiService.getPlayerInfo(binUser, mode);
            bpListMe = scoreApiService.getBestPerformance(binUser, mode, 0, 100);
        } catch (BindException e) {
            throw new PPMinusException(PPMinusException.Type.PPM_Me_TokenExpired);
        }
        try {
            if (at != null) {//被对比人的信息
                // 包含有@
                var b = bindDao.getUserFromQQ(at.getTarget());
                userOther = userApiService.getPlayerInfo(b, mode);
                bpListOther = scoreApiService.getBestPerformance(b, mode, 0, 100);
            } else if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
                var id = userApiService.getOsuId(matcher.group("name").trim());
                userOther = userApiService.getPlayerInfo(id, mode);
                bpListOther = scoreApiService.getBestPerformance(id, mode, 0, 100);
            } else {
                throw new PPMinusException(PPMinusException.Type.PPM_Player_VSNotFound);
            }
        } catch (PPMinusException e) {
            throw e;
        } catch (WebClientResponseException.Unauthorized e) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_TokenExpired);
        } catch (WebClientResponseException.NotFound e) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_NotFound);
        } catch (Exception e) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_FetchFailed);
        }

        try {
            PPMinusMe = PPMinus.getInstance(mode, userMe, bpListMe);
            PPMinusOther = PPMinus.getInstance(mode, userOther, bpListOther);
        } catch (Exception e) {
            log.error("PPM 数据计算失败", e);
            throw new PPMinusException(PPMinusException.Type.PPM_Calculate_Error);
        }

        if (userOther.getStatistics().getPlayTime() < 60 || userOther.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_PlayTimeTooShort);
        }

        if (userMe.getStatistics().getPlayTime() < 60 || userMe.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Me_PlayTimeTooShort);
        }

        //你为啥不在数据库里存这些。。。
        // 就两个
        if (userOther.getUID() == 17064371L){
            setUser(PPMinusOther, 999.99f);
        } else if (userOther.getUID().equals(19673275L)) {
            setUser(PPMinusOther, 0);
        }

        var image = imageService.getPanelB1(userMe, userOther, PPMinusMe, PPMinusOther, mode);
        from.sendImage(image);
    }

    static void setUser(PPMinus PPMinusOther, float value) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> PPMClass =  Class.forName("com.now.nowbot.model.ppminus.PPMinus");
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
            i.set(PPMinusOther,value);
        }
    }
}
