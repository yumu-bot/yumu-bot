package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.Ymp;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("score")
public class ScoreService implements MessageService{
    @Autowired
    OsuGetService osuGetService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var user = BindingUtil.readUser(event.getSender().getId());
        int bid = Integer.parseInt(matcher.group("bid"));
        JSONObject score = osuGetService.getScore(bid, user);
        var d = Ymp.getInstance(score);
        from.sendMessage(d.getOut());
    }
}
