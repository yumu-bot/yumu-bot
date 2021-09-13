package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.Ymp.Ymp;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("ymp")
public class YmpService implements MessageService{

    @Autowired
    OsuGetService osuGetService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        String name = matcher.group("name");
        JSONArray dates;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        var mode = matcher.group("mode")==null?"null":matcher.group("mode").toLowerCase();
        switch (mode){
            case"null":
            case"osu":
            case"o":
            case"0":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = osuGetService.getRecent(user,"osu",0,1);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = osuGetService.getRecent(id,"osu",0,1);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = osuGetService.getRecent(user,"osu",0,1);
                    }
                }
                mode = "osu";
            } break;
            case"taiko":
            case"t":
            case"1":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = osuGetService.getRecent(user,"taiko",0,1);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = osuGetService.getRecent(id,"taiko",0,1);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = osuGetService.getRecent(user,"taiko",0,1);
                    }
                }
                mode = "taiko";
            } break;
            case"catch":
            case"c":
            case"2":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = osuGetService.getRecent(user,"fruits",0,1);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = osuGetService.getRecent(id,"fruits",0,1);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = osuGetService.getRecent(user,"fruits",0,1);
                    }
                }
                mode = "fruits";
            } break;
            case"mania":
            case"m":
            case"3":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = osuGetService.getRecent(user,"mania",0,1);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = osuGetService.getRecent(id,"mania",0,1);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = osuGetService.getRecent(user,"mania",0,1);
                    }
                }
                mode = "fruits";
            }break;
            default:{
                throw new TipsException("未知参数");
            }
        }
        if(dates.size()==0){
            throw new TipsException("24h内无记录");
        }
        JSONObject date = dates.getJSONObject(0);
        var d = Ymp.getInstance(date);
        from.sendMessage(d.getOut());
    }
}
