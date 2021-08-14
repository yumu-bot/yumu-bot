package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StarServiceImpl extends MessageService{
    @Autowired
    StarService starService;
    @Autowired
    OsuGetService osuGetService;
    StarServiceImpl(){
        super("查看积分");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if(user == null){
            from.sendMessage(new At(event.getSender().getId()).plus("您尚未绑定，请发送bind绑定"));
            return;
        }
        StringBuffer sb = new StringBuffer();
        StarService.score sc = starService.getScore(user);
        if (starService.isRefouse(sc)){
            JSONObject date = osuGetService.getPlayerInfo(user);
            float adsstar = date.getJSONObject("statistics").getFloatValue("pp")/100;
            starService.refouseStar(sc,adsstar);
            sb.append("今日刷新").append(adsstar).append("点积分\n").append("24小时后再次刷新\n");
        }

        var date = osuGetService.getRecent(user,0,1);
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

                starService.addStart(sc, pp);
            }
        }
        sb.append("您有积分").append(sc.getStar()).append("点");
        from.sendMessage(new At(event.getSender().getId()).plus(sb.toString()));
    }
}
