package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.PpmException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;

@Service("PPMinus")
public class PPMinusService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(PPMinusService.class);
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;
    @Autowired
    ImageService imageService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }

        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        Ppm ppm;
        OsuUser user;
        List<Score> bps;
        var mode = OsuMode.getMode(matcher.group("mode"));

        if (at != null) {
            // 包含有@
            var userBin = bindDao.getUser(at.getTarget());
            //处理默认mode
            if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
            user = osuGetService.getPlayerInfo(userBin, mode);
            bps = osuGetService.getBestPerformance(userBin, mode, 0, 100);
            ppm = Ppm.getInstance(mode, user, bps);
        } else {
            // 不包含@ 分为查自身/查他人
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                // 查他人
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = osuGetService.getPlayerInfo(id, mode);
                bps = osuGetService.getBestPerformance(id, mode, 0, 100);
                //默认无主模式
                if (mode == OsuMode.DEFAULT && user.getPlayMode() != null) mode = user.getPlayMode();
                ppm = Ppm.getInstance(mode, user, bps);
            } else {
                var userBin = bindDao.getUser(event.getSender().getId());//处理默认mode
                if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
                user = osuGetService.getPlayerInfo(userBin, mode);
                bps = osuGetService.getBestPerformance(userBin, mode, 0, 100);
                ppm = Ppm.getInstance(mode, user, bps);
            }
        }

        try {
            long now = System.currentTimeMillis();
            var img = imageService.getPanelB(user, mode, ppm);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("PPM 数据请求失败", e);
            from.sendMessage("PPM 渲染图片超时，请重试。\n或尝试旧版渲染 !pmv2。");
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws BindException, PpmException, NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        OsuUser userMe;
        var userBin = bindDao.getUser(event.getSender().getId());
        List<Score> bpListMe;
        OsuUser userOther;
        List<Score> bpListOther;
        Ppm ppmMe;
        Ppm ppmOther;

        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
        me://自己的信息
        {

            userMe = osuGetService.getPlayerInfo(userBin, mode);
            bpListMe = osuGetService.getBestPerformance(userBin, mode,0,100);
            ppmMe = Ppm.getInstance(mode, userMe, bpListMe);
            if (userMe.getStatistics().getPlayTime() < 60 || userMe.getStatistics().getPlayCount() < 30) {
                throw new PpmException(PpmException.Type.PPM_Me_PlayTimeTooShort);
            }
        }
        //生成panel名
        String panelName = "VS" + switch (mode) {
            case OSU -> ":O";
            case MANIA -> ":M";
            case CATCH -> ":C";
            case TAIKO -> ":T";
            default -> ":?";
        };
        if (at != null) {//被对比人的信息
            // 包含有@
            var OtherBin = bindDao.getUser(at.getTarget());
            userOther = osuGetService.getPlayerInfo(OtherBin, mode);
            bpListOther = osuGetService.getBestPerformance(OtherBin, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            userOther = osuGetService.getPlayerInfo(id, mode);
            bpListOther = osuGetService.getBestPerformance(id, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else {
            throw new PpmException(PpmException.Type.PPM_Player_VSNotFound);
        }
        if (userOther.getStatistics().getPlayTime() < 60 || userOther.getStatistics().getPlayCount() < 30) {
            throw new PpmException(PpmException.Type.PPM_Player_PlayTimeTooShort);
        }
        if (userOther.getId() == 17064371L){
            Class clz =  Class.forName("com.now.nowbot.model.PPm.Ppm");
            Field[] valueFiled = {
                    clz.getDeclaredField("value1"),
                    clz.getDeclaredField("value2"),
                    clz.getDeclaredField("value3"),
                    clz.getDeclaredField("value4"),
                    clz.getDeclaredField("value5"),
                    clz.getDeclaredField("value6"),
                    clz.getDeclaredField("value7"),
                    clz.getDeclaredField("value8"),
            };
            for (var i:valueFiled){
                i.setAccessible(true);
                i.set(ppmOther,999.99);
            }
        }

        var data = imageService.getPanelB(userMe, userOther, ppmMe, ppmOther, mode);
        QQMsgUtil.sendImage(from, data);
    }
}
