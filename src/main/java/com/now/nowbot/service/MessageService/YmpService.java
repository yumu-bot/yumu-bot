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
        boolean isAll = matcher.group("isAll").toLowerCase().charAt(0) == 'r';
        from.sendMessage(isAll?"正在查询24h内的所有成绩":"正在查询24h内的pass成绩");
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
                    dates = getDates(user,"osu",isAll);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = getDates(id,"osu",isAll);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = getDates(user,"osu",isAll);
                    }
                }
                mode = "osu";
            } break;
            case"taiko":
            case"t":
            case"1":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = getDates(user,"taiko",isAll);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = getDates(id,"taiko",isAll);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = getDates(user,"taiko",isAll);
                    }
                }
                mode = "taiko";
            } break;
            case"catch":
            case"c":
            case"2":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = getDates(user,"fruits",isAll);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = getDates(id,"fruits",isAll);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = getDates(user,"fruits",isAll);
                    }
                }
                mode = "fruits";
            } break;
            case"mania":
            case"m":
            case"3":{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    dates = getDates(user,"mania",isAll);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        dates = getDates(id,"mania",isAll);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        dates = getDates(user,"mania",isAll);
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
    private JSONArray getDates(BinUser user, String mode, boolean isAll){
        if (isAll)
            return osuGetService.getAllRecent(user, mode, 0, 1);
        else
            return osuGetService.getRecent(user, mode, 0, 1);
    }
    private JSONArray getDates(int id, String mode, boolean isAll){
        if (isAll)
            return osuGetService.getAllRecent(id, mode, 0, 1);
        else
            return osuGetService.getRecent(id, mode, 0, 1);
    }
}
