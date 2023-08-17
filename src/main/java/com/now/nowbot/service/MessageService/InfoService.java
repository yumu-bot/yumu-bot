package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
        log.error("-------------------ymi--------------------");
        log.error("from: {}", event.getSender().getName());
        log.error("at is null ? [{}]", at == null);
        BinUser user;
        if (at != null) {
            user = bindDao.getUser(at.getTarget());
            log.error("at: {}", user.getOsuName());
        } else {
            if (name != null && !name.trim().equals("")) {
                log.error("not at, name: {}", matcher.group("name").trim());
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = new BinUser();
                user.setOsuID(id);
                user.setMode(OsuMode.DEFAULT);
            } else {
                user = bindDao.getUser(event.getSender().getId());
                log.error("not at, is me: {}", user.getOsuName());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

//        Image img = from.uploadImage(ExternalResource.create());
        try {
            var img = imageService.getPanelD(user, mode, osuGetService);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("INFO 数据请求失败", e);
            from.sendMessage("Info 渲染图片超时，请重试。");
        }
    }
}
