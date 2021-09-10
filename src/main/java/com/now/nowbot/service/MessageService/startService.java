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
        Contact from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());
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
            var re = date.getJSONObject(0);
            long bid = re.getLongValue("best_id");
            if (bid != sc.getBest_id() && re.getJSONObject("beatmap").getString("status").equals("ranked")) {
                sc.setBest_id(bid);
                float pp = re.getFloatValue("pp");
                sb.append("您此次成绩为")
                        .append(re.getFloatValue("pp"))
                        .append("pp").append('\n');

                    if (re.getJSONArray("mods").contains("HD")) {
                        pp /= 25;
                        sb.append("由于使用了hd,您获得了").append(pp).append("积分").append('\n');
                    } else {
                        pp /= 20;
                        sb.append("您获得了").append(pp).append("积分").append('\n');
                    }
                if (System.currentTimeMillis() % 312 == 15) {
                    sb.append("恭喜！非常幸运的触发了积分暴击，您本次获得积分为10倍！");
                    pp *= 10;
                }
                starService.addStart(sc, pp);
            }
        }
        sb.append("您有积分").append(sc.getStar()).append("点");
        from.sendMessage(new At(event.getSender().getId()).plus(sb.toString()));
    }
}
