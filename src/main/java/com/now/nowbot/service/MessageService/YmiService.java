package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.Ymi;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

/**
 * 这是个复制品，直接复制的ymp代码
 * 如果有bug请慢慢修吧，我真的不会写
 */

@Service("ymi")
public class YmiService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(YmiService.class);

    @Autowired
    OsuGetService osuGetService;

    @Autowired
    StarService starService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage(isAll?"正在查询您的信息...");
        String name = matcher.group("name");
        JSONArray dates;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        BinUser user = null;
        int id = 0;
        if (at != null){
            user = BindingUtil.readUser(at.getTarget());
        }else {
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")){
                id = osuGetService.getOsuId(matcher.group("name").trim());
            }else {
                user = BindingUtil.readUser(event.getSender().getId());
            }
        }
        var mode = matcher.group("mode")==null?"null":matcher.group("mode").toLowerCase();
        switch (mode){
            case"null":
            case"osu":
            case"o":
            case"0":{
                if (user != null){
                    dates = getDates(user,"osu");
                }else {
                    dates = getDates(id,"osu");
                }
                mode = "osu";
            } break;
            case"taiko":
            case"t":
            case"1":{
                if (user != null){
                    dates = getDates(user,"taiko");
                }else {
                    dates = getDates(id,"taiko");
                }
                mode = "taiko";
            } break;
            case"catch":
            case"c":
            case"2":{
                if (user != null){
                    dates = getDates(user,"fruits");
                }else {
                    dates = getDates(id,"fruits");
                }
                mode = "fruits";
            } break;
            case"mania":
            case"m":
            case"3":{
                if (user != null){
                    dates = getDates(user,"mania");
                }else {
                    dates = getDates(id,"mania");
                }
                mode = "fruits";
            }break;
            default:{
                throw new TipsException("如果您...传了这些完全没法用的参数...这么说...您也有泽任吧...！");
            }
        }
        if(dates.size()==0){
            throw new TipsException("未查询到您的信息！");
        }
        JSONObject date = dates.getJSONObject(0);
        var d = Ymi.getInstance(date);
        from.sendMessage(d.getOut());
        if (user != null){
            log.info(starService.ScoreToStar(user, date));
        }
    }
    private JSONArray getDates(BinUser user, String mode){
        return osuGetService.getPlayerOsuInfo(user, mode);

    }
    private JSONArray getDates(int id, String mode){
        return osuGetService.getPlayerOsuInfo(id, mode);

    }
}
