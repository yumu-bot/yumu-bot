package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("start")
public class startService implements MessageService{
    @Autowired
    StarService starService;
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        //获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        Contact from = event.getSubject();
        BinUser user;
        if (at != null){
            user = BindingUtil.readUser(at.getTarget());
        }else {
            user = BindingUtil.readUser(event.getSender().getId());
        }
        StringBuffer sb = new StringBuffer();
        StarService.Score sc = starService.getScore(user);

        if (starService.isRefouse(sc)){
            JSONObject date = osuGetService.getPlayerOsuInfo(user);
            float adsstar = date.getJSONObject("statistics").getFloatValue("pp")/100;
            starService.refouseStar(sc,adsstar);
            sb.append("今日刷新").append(adsstar).append("点积分\n").append("24小时后再次刷新\n");
        }
        var date = osuGetService.getOsuRecent(user,0,1);
        if (date.size()>0) {
            sb.append(starService.ScoreToStar(user, date.getJSONObject(0)));
        }
        sb.append("您有积分").append(sc.getStar()).append("点");
        from.sendMessage(new At(event.getSender().getId()).plus(sb.toString()));
    }
}
