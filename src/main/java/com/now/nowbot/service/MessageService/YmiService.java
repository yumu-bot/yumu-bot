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
        boolean isAll = matcher.group("isAll").toLowerCase().charAt(0) == 'r';
        //from.sendMessage(isAll?"正在查询您的信息":"正在查询您的信息");
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
                    dates = getDates(user,"osu",isAll);
                }else {
                    dates = getDates(id,"osu",isAll);
                }
                mode = "osu";
            } break;
            case"taiko":
            case"t":
            case"1":{
                if (user != null){
                    dates = getDates(user,"taiko",isAll);
                }else {
                    dates = getDates(id,"taiko",isAll);
                }
                mode = "taiko";
            } break;
            case"catch":
            case"c":
            case"2":{
                if (user != null){
                    dates = getDates(user,"fruits",isAll);
                }else {
                    dates = getDates(id,"fruits",isAll);
                }
                mode = "fruits";
            } break;
            case"mania":
            case"m":
            case"3":{
                if (user != null){
                    dates = getDates(user,"mania",isAll);
                }else {
                    dates = getDates(id,"mania",isAll);
                }
                mode = "fruits";
            }break;
            default:{
                throw new TipsException("如果您...传了这些完全没法用的参数...这么说...您也有泽任吧...！");
            }
        }
        if(dates.size()==0){
            throw new TipsException("没有查询到您的信息呢");
        }
        JSONObject date = dates.getJSONObject(0);
        var d = Ymi.getInstance(date);
        from.sendMessage(d.getOut());
        if (user != null){
            log.info(starService.ScoreToStar(user, date));
        }
    }
    private JSONArray getDates(BinUser user, String mode, boolean isAll){
        if (isAll)
            return osuGetService.getPlayerInfo(user, mode, 0, 1);
        else
            return osuGetService.getPlayerInfo(user, mode, 0, 1);
    }
    private JSONArray getDates(int id, String mode, boolean isAll){
        if (isAll)
            return osuGetService.getPlayerInfo(id, mode, 0, 1);
        else
            return osuGetService.getPlayerInfo(id, mode, 0, 1);
    }
}
