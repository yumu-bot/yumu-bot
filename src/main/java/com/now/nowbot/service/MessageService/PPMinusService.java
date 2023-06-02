package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("PPMinus")
public class PPMinusService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(PPmService.class);
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
            //doVs(event, matcher);
            //return;
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
            var userBin = bindDao.getUser(at.getTarget());
            var img = imageService.getPanelB(userBin, mode, osuGetService, ppm);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("PPM 数据请求失败", e);
            from.sendMessage("PPM 渲染图片超时，请重试。");
        }
    }
}
