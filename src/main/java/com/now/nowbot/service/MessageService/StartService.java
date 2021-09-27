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
public class StartService implements MessageService{
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
            sb.append("今日刷新").append(adsstar).append("点金币\n").append("24小时后再次刷新\n");
        }
        var date = osuGetService.getOsuRecent(user,0,1);
        if (date.size()>0) {
            sb.append(starService.ScoreToStar(user, date.getJSONObject(0)));
        }
        sb.append("您有金币").append(sc.getStar()).append("点");
        from.sendMessage(new At(event.getSender().getId()).plus(sb.toString()));
    }
    static double refreshPP(float pp, float acc){
        //修正系数  单图94acc为基准
        double s = acc + 0.06;
        return pp/100*s;
    }
    static double scorePP(float pp, float acc){
        //修正系数  单图96acc为基准
        double s = acc + 0.04;
        return (pp/20 * s);
    }
    static double scoreTth(int n, float acc){
        //修正系数  单图96acc为基准
        double s = acc + 0.04;
        //物件tth 500;
        if(n < 500) return 0;
        n -= 800;
        return (10 + n/100D) * s;
    }
    static double newBp(int indexBP){
        return 5-indexBP*0.005;
    }
}
