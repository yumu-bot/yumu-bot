package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static com.now.nowbot.service.MessageServiceImpl.PPMinusService.PPMinusStatus.*;

@Service("PP_MINUS")
public class PPMinusService implements MessageService<PPMinusService.PPMinusParam> {
    private static final Logger log = LoggerFactory.getLogger(PPMinusService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

    public record PPMinusParam(boolean isVs, BinUser me, BinUser other, OsuMode mode) {}

    public enum PPMinusStatus {
        USER,
        USER_VS,
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PPMinusParam> data) {
        var matcher = Instructions.PP_MINUS.matcher(messageText);
        if (!matcher.find()) return false;

        var status = switch (Optional.ofNullable(
                matcher.group("function")
        ).orElse("pm").trim().toLowerCase()) {
            case "pm", "ppm", "pp-", "p-", "ppminus", "minus" -> USER;
            case "pv", "ppmv", "pmv", "pmvs", "ppmvs", "ppminusvs", "minusvs" -> USER_VS;
            default -> throw new RuntimeException("PP-：未知的类型");
        };

        var area1 = matcher.group("area1");
        var area2 = matcher.group("area2");

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        BinUser me = new BinUser();
        BinUser other = new BinUser();

        // 艾特
        if (at != null) {
            switch (status) {
                case USER -> //pm @
                        me = bindDao.getUserFromQQ(at.getTarget());

                case USER_VS -> {
                    //pv 0v@
                    me = bindDao.getUserFromQQ(event.getSender().getId());
                    other = bindDao.getUserFromQQ(at.getTarget());
                }
            }
        }

        if (StringUtils.hasText(area1) || StringUtils.hasText(area2)) {
            if (StringUtils.hasText(area1) && StringUtils.hasText(area2)) {
                //pv 1v2
                me.setOsuName(area1);
                other.setOsuName(area2);
            } else {
                var area = StringUtils.hasText(area1) ? area1 : area2;

                switch (status) {
                    case USER -> //pm 1 or 2
                            me.setOsuName(area);

                    case USER_VS -> {
                        //pv 0v1 or 0v2
                        me = bindDao.getUserFromQQ(event.getSender().getId());
                        other.setOsuName(area);
                    }
                }
            }
        } else {
            // pm 0
            me = bindDao.getUserFromQQ(event.getSender().getId());
        }

        // 处理默认模式
        var mode = OsuMode.getMode(matcher.group("mode"), me.getMode());

        // 在新人群管理群里查询则主动认为是 osu 模式
        if (event.getSubject().getId() == 695600319L && OsuMode.DEFAULT.equals(mode)) {
            mode = OsuMode.OSU;
        }

        boolean isVs = (other.getOsuName() != null);
        data.setValue(
                new PPMinusParam(isVs, me, other, mode)
        );

        return true;
    }

    /**
     * 获取 PPM 信息重写
     * @param user 玩家信息
     * @return PPM 实例
     */
    private PPMinus getPPMinus(OsuUser user) throws PPMinusException {
        List<Score> BPList;

        try {
            BPList = scoreApiService.getBestPerformance(user);
        } catch (WebClientResponseException e) {
            log.error("PP-：最好成绩获取失败", e);
            throw new PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed);
        }


        if (user.getStatistics().getPlayTime() < 60 || user.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PM_Player_PlayTimeTooShort,  OsuMode.getName(user.getOsuMode()).orElse("Default"));
        }

        try {
            return PPMinus.getInstance(user.getOsuMode(), user, BPList);
        } catch (Exception e) {
            log.error("PP-：数据计算失败", e);
            throw new PPMinusException(PPMinusException.Type.PM_Calculate_Error);
        }
    }


    // 感觉可以写到 OsuUser 或 BinUser 的公用方法里
    private OsuUser getOsuUser(BinUser user, OsuMode mode) throws Throwable {
        try {
            if (user == null) return null;
            else if (user.getOsuName() != null) return userApiService.getPlayerInfo(user.getOsuName(), mode);
            else if (user.isAuthorized()) return userApiService.getPlayerInfo(user, mode);
            else return null;
        } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
            throw new PPMinusException(PPMinusException.Type.PM_Player_NotFound);
        } catch (BindException e) {
            throw e;
        } catch (Exception e) {
            log.error("PP-：玩家信息获取失败：", e);
            throw new PPMinusException(PPMinusException.Type.PM_Player_FetchFailed);
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, PPMinusParam param) throws Throwable {
        var from = event.getSubject();

        OsuUser me = getOsuUser(param.me, param.mode);
        PPMinus my = getPPMinus(me);

        OsuUser other = null;
        PPMinus others = null;

        if (param.isVs) {
            other = getOsuUser(param.other, param.mode);
            others = getPPMinus(other);
        }

        //你为啥不在数据库里存这些。。。
        // 就两个
        if (other != null) {
            if (other.getId() == 17064371L) {
                customizePerformanceMinus(others, 999.99f);
            } else if (other.getId() == 19673275L) {
                customizePerformanceMinus(others, 0);
            }
        }

        /*
        笑点解析：《就不写一堆了》
        if (param.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, param);
            return;
        }
         */

        byte[] image;

        try {
            image = imageService.getPanelB1(me, other, my, others, param.mode);
        } catch (WebClientResponseException e) {
            log.error("PP-：渲染失败：", e);
            throw new PPMinusException(PPMinusException.Type.PM_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("PP-：发送失败：", e);
            throw new PPMinusException(PPMinusException.Type.PM_Send_Error);
        }
    }

    private static void customizePerformanceMinus(PPMinus minus, float value) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if (minus == null) return;

        Class<?> PPMClass =  Class.forName("com.now.nowbot.model.ppminus.PPMinus");
        Field[] valieFields = {
                PPMClass.getDeclaredField("value1"),
                PPMClass.getDeclaredField("value2"),
                PPMClass.getDeclaredField("value3"),
                PPMClass.getDeclaredField("value4"),
                PPMClass.getDeclaredField("value5"),
                PPMClass.getDeclaredField("value6"),
                PPMClass.getDeclaredField("value7"),
                PPMClass.getDeclaredField("value8"),
        };
        for (var i : valieFields){
            i.setAccessible(true);
            i.set(minus, value);
        }
    }
}
