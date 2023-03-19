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
            var data = postImage(userInfo, score);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            from.sendMessage("出错了出错了,问问管理员");
        }
    }

    public byte[] postImage(OsuUser user, Score score) {
        var map = osuGetService.getMapInfo(score.getBeatMap().getId());
        score.setBeatMap(map);
        score.setBeatMapSet(map.getBeatMapSet());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of("user", user,
                "score", score
        );
        HttpEntity httpEntity = new HttpEntity(body, headers);
        ResponseEntity<byte[]> s = template.exchange(URI.create("http://127.0.0.1:1611/panel_E"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }
}
