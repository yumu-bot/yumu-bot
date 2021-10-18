package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.OsuMode;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("ymi")
public class YmiService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(YmiService.class);
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;

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
        var mode = OsuMode.getMode(matcher.group("mode"));
        {
            switch (mode) {
                default: //todo 获取账号默认模式
                    mode = OsuMode.OSU;
                case OSU: {
                    if (user != null) {
                        date = osuGetService.getPlayerOsuInfo(user);
                    } else {
                        date = osuGetService.getPlayerOsuInfo(id);
                    }
                }
                break;
                case TAIKO: {
                    if (user != null) {
                        date = osuGetService.getPlayerTaikoInfo(user);
                    } else {
                        date = osuGetService.getPlayerTaikoInfo(id);
                    }
                }
                break;
                case CATCH: {
                    if (user != null) {
                        date = osuGetService.getPlayerCatchInfo(user);
                    } else {
                        date = osuGetService.getPlayerCatchInfo(id);
                    }
                }
                break;
                case MANIA: {
                    if (user != null) {
                        date = osuGetService.getPlayerManiaInfo(user);
                    } else {
                        date = osuGetService.getPlayerManiaInfo(id);
                    }
                }
                break;
            }
        }
        if(date.size()==0){
            throw new TipsException("没有查询到您的信息呢");
        }
        StringBuilder sb = new StringBuilder();
        var statistics = date.getJSONObject("statistics");
        // Muziyami(osu):10086PP
        sb.append(date.getString("username")).append('(').append(mode).append(')').append(':').append(statistics.getFloatValue("pp")).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(statistics.getIntValue("global_rank")).append(' ')
                .append(date.getJSONObject("country").getString("code")).append('#').append(statistics.getIntValue("country_rank")).append(' ')
                .append("(LV.").append(statistics.getJSONObject("level").getIntValue("current")).append('(').append(statistics.getJSONObject("level").getIntValue("progress")).append("%))").append('\n');
        // PC:2.01w TTH:743.52w
        sb.append("PC:");
        int PC = statistics.getIntValue("play_count");
        if (PC>10_000) {
            sb.append((PC / 100) / 100D).append('w');
        }else {
            sb.append(PC);
        }
        sb.append(" TTH:");
        int TTH = statistics.getIntValue("total_hits");
        if (TTH>10_000) {
            sb.append((TTH / 100) / 100D).append('w');
        }else {
            sb.append(TTH);
        }
        sb.append('\n');
        // PT:24d2h7m ACC:98.16%
        sb.append("PT:");
        int PT = statistics.getIntValue("play_time");
        if(PT>86400){
            sb.append(PT/86400).append('d');
        }
        if(PT>3600){
            sb.append((PT%86400)/3600).append('h');
        }
        if(PT>60){
            sb.append((PT%3600)/60).append('m');
        }
        sb.append(" ACC:").append(statistics.getFloatValue("hit_accuracy")).append('%').append('\n');
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ").append(date.getIntValue("follower_count"))
                .append(" kds:").append(date.getJSONObject("kudosu").getIntValue("total")).append('\n');
        // SS:26(107) S:157(844) A:1083
        sb.append("SS:").append(statistics.getJSONObject("grade_counts").getIntValue("ss"))
                .append('(').append(statistics.getJSONObject("grade_counts").getIntValue("ssh")).append(')')
                .append(" S:").append(statistics.getJSONObject("grade_counts").getIntValue("s"))
                .append('(').append(statistics.getJSONObject("grade_counts").getIntValue("sh")).append(')')
                .append(" A:").append(statistics.getJSONObject("grade_counts").getIntValue("a")).append('\n');
        // uid:7003013
        sb.append("UID:").append(date.getIntValue("id")).append('\n');

        String occupation = (String) date.getOrDefault("occupation","");
        String discord = (String) date.getOrDefault("discord","");
        String interests = (String) date.getOrDefault("interests","");
        if ((occupation != null && !occupation.trim().equals("")) || (discord != null && !discord.trim().equals("")) || (interests != null && !interests.trim().equals(""))){
            sb.append('\n');
            if (occupation != null && !occupation.trim().equals("")) {
                sb.append("occupation: ").append(occupation.trim()).append('\n');
            }if (discord != null && !discord.trim().equals("")) {
                sb.append("discord: ").append(discord.trim()).append('\n');
            }if (interests != null && !interests.trim().equals("")) {
                sb.append("interests: ").append(interests.trim()).append('\n');
            }
        }
//        Image img = from.uploadImage(ExternalResource.create());
        from.sendMessage(sb.toString());
    }
}
