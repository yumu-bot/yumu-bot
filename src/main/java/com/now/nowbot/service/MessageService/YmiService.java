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
        //from.sendMessage("正在查询您的信息");
        String name = matcher.group("name");
        JSONObject date;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        BinUser user = null;
        int id = 0;
        if (at != null){
            user = BindingUtil.readUser(at.getTarget());
        }else {
            if (name != null && !name.trim().equals("")){
                id = osuGetService.getOsuId(matcher.group("name").trim());
            }else {
                user = BindingUtil.readUser(event.getSender().getId());
            }
        }
        var mode = matcher.group("mode");
        if (mode == null){
            mode = "osu";
        }
        {
            mode = mode.toLowerCase();
            switch (mode) {
                default:
                case "osu":
                case "o":
                case "0": {
                    if (user != null) {
                        date = osuGetService.getPlayerOsuInfo(user);
                    } else {
                        date = osuGetService.getPlayerOsuInfo(id);
                    }
                    mode = "osu";
                }
                break;
                case "taiko":
                case "t":
                case "1": {
                    if (user != null) {
                        date = osuGetService.getPlayerTaikoInfo(user);
                    } else {
                        date = osuGetService.getPlayerTaikoInfo(id);
                    }
                    mode = "taiko";
                }
                break;
                case "catch":
                case "c":
                case "2": {
                    if (user != null) {
                        date = osuGetService.getPlayerCatchInfo(user);
                    } else {
                        date = osuGetService.getPlayerCatchInfo(id);
                    }
                    mode = "catch";
                }
                break;
                case "mania":
                case "m":
                case "3": {
                    if (user != null) {
                        date = osuGetService.getPlayerManiaInfo(user);
                    } else {
                        date = osuGetService.getPlayerManiaInfo(id);
                    }
                    mode = "mania";
                }
                break;
            }
        }
        if(date.size()==0){
            throw new TipsException("没有查询到您的信息呢");
        }
        StringBuffer sb = new StringBuffer();
        var statistics = date.getJSONObject("statistics");
        // Muziyami(osu):10086PP
        sb.append(date.getString("username")).append('(').append(mode).append(')').append(':').append(statistics.getFloatValue("pp")).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(statistics.getIntValue("global_rank")).append(' ')
                .append(date.getJSONObject("country").getString("code")).append('#').append(statistics.getIntValue("country_rank")).append(' ')
                .append("(LV.").append(statistics.getJSONObject("level").getIntValue("current")).append('(').append(statistics.getJSONObject("level").getIntValue("progress")).append("%))").append('\n');
        // PC:2.01w TTH:743.52w

        from.sendMessage(sb.toString());
    }
}
