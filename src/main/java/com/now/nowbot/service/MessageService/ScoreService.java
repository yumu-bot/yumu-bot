package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;

@Service("score")
public class ScoreService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    @Autowired
    public ScoreService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        BinUser user = null;

        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if (at != null) {
            try {
                user = bindDao.getUser(at.getTarget());
            } catch (Exception e) {
                throw new TipsException("该玩家没有绑定");
            }
        }
        else {
            user = bindDao.getUser(event.getSender().getId());
        }
        var bid = Long.parseLong(matcher.group("bid"));
        Score score = null;
        try {
            score = osuGetService.getScore(bid, user, user.getMode()).getScore();
        } catch (Exception e) {
            from.sendMessage("你没打过这张图");
            return;
        }
        var userInfo = osuGetService.getPlayerInfo(user);

        try {
            var data = YmpService.postImage(userInfo, score, osuGetService, template);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
