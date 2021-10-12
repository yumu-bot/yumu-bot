package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.Ymp;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("score")
public class ScoreService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        BinUser user = null;

        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if (at != null) {
            try {
                user = BindingUtil.readUser(at.getTarget());
            } catch (Exception e) {
                throw new TipsException("该玩家没有绑定");
            }
        }
        else {
            user = BindingUtil.readUser(event.getSender().getId());
        }
        int bid = Integer.parseInt(matcher.group("bid"));
        JSONObject score = osuGetService.getScore(bid, user);
    }
}
