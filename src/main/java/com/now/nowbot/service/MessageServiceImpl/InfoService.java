package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;

@Service("Info")
public class InfoService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao       bindDao;
    @Autowired
    ImageService  imageService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage("正在查询您的信息");
        String name = matcher.group("name");
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser user;
        if (at != null) {
            user = bindDao.getUser(at.getTarget());
        } else {
            if (name != null && !name.trim().isEmpty()) {
                long id;
                try {
                    id = osuGetService.getOsuId(matcher.group("name").trim());
                } catch (Exception e) {
                    throw new InfoException(InfoException.Type.INFO_Player_NotFound);
                }
                user = new BinUser();
                user.setOsuID(id);
                user.setMode(OsuMode.DEFAULT);
            } else {
                try {
                    user = bindDao.getUser(event.getSender().getId());
                } catch (Exception e) {
                    throw new InfoException(InfoException.Type.INFO_Me_NotFound);
                }
            }
        }

        //处理默认mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        OsuUser osuUser;
        List<Score> BPs;
        List<Score> Recents;

        try {
            osuUser = osuGetService.getPlayerInfo(user, mode);
            BPs = osuGetService.getBestPerformance(user, mode, 0, 100);
        } catch (Exception e) {
            throw new InfoException(InfoException.Type.INFO_Player_NoBP);
        }

        Recents = osuGetService.getRecentN(user, mode, 0, 3);

        try {
            var img = imageService.getPanelD(osuUser, BPs, Recents, mode, osuGetService);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("INFO 数据请求失败", e);
            from.sendMessage("Info 渲染图片超时，请重试。");
        }
    }
}
