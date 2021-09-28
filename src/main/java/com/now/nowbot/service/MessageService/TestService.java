package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("test")
public class TestService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher aaa) throws Throwable {
        var msg = event.getMessage();
        if (event instanceof GroupMessageEvent && ((GroupMessageEvent) event).getGroup().getId() == 746671531L) {
            var grp = ((GroupMessageEvent) event).getGroup();
            var pt = Pattern.compile("!testppm([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");
            var matcher = pt.matcher(msg.contentToString());
            if (matcher.find()) {
                PPmObject userinfo;
                JSONObject userdate;
                var mode = matcher.group("mode") == null ? "null" : matcher.group("mode").toLowerCase();
                switch (mode) {
                    case "null":
                    case "osu":
                    case "o":
                    case "0": {
                        {
                            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                                int id = osuGetService.getOsuId(matcher.group("name").trim());
                                userdate = osuGetService.getPlayerOsuInfo(id);
                                var bpdate = osuGetService.getOsuBestMap(id, 0, 100);
                                userinfo = PPmObject.presOsu(userdate, bpdate);
                            } else {
                                var user = BindingUtil.readUser(event.getSender().getId());
                                userdate = osuGetService.getPlayerOsuInfo(user);
                                var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                                userinfo = PPmObject.presOsu(userdate, bpdate);
                            }
                        }
                        mode = "osu";
                    }
                    break;
                    case "taiko":
                    case "t":
                    case "1": {
                        {
                            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                                int id = osuGetService.getOsuId(matcher.group("name").trim());
                                userdate = osuGetService.getPlayerTaikoInfo(id);
                                var bpdate = osuGetService.getTaikoBestMap(id, 0, 100);
                                userinfo = PPmObject.presTaiko(userdate, bpdate);
                            } else {
                                var user = BindingUtil.readUser(event.getSender().getId());
                                userdate = osuGetService.getPlayerTaikoInfo(user);
                                var bpdate = osuGetService.getTaikoBestMap(user, 0, 100);
                                userinfo = PPmObject.presTaiko(userdate, bpdate);
                            }
                        }
                        mode = "taiko";
                    }
                    break;
                    case "catch":
                    case "c":
                    case "2": {
                        {
                            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                                int id = osuGetService.getOsuId(matcher.group("name").trim());
                                userdate = osuGetService.getPlayerCatchInfo(id);
                                var bpdate = osuGetService.getCatchBestMap(id, 0, 100);
                                userinfo = PPmObject.presCatch(userdate, bpdate);
                            } else {
                                var user = BindingUtil.readUser(event.getSender().getId());
                                userdate = osuGetService.getPlayerCatchInfo(user);
                                var bpdate = osuGetService.getCatchBestMap(user, 0, 100);
                                userinfo = PPmObject.presCatch(userdate, bpdate);
                            }
                        }
                        mode = "fruits";
                    }
                    break;
                    case "mania":
                    case "m":
                    case "3": {
                        {
                            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                                int id = osuGetService.getOsuId(matcher.group("name").trim());
                                userdate = osuGetService.getPlayerManiaInfo(id);
                                var bpdate = osuGetService.getManiaBestMap(id, 0, 100);
                                userinfo = PPmObject.presMania(userdate, bpdate);
                            } else {
                                var user = BindingUtil.readUser(event.getSender().getId());
                                userdate = osuGetService.getPlayerManiaInfo(user);
                                var bpdate = osuGetService.getManiaBestMap(user, 0, 100);
                                userinfo = PPmObject.presMania(userdate, bpdate);
                            }
                        }
                        mode = "mania";
                    }
                    break;
                    default: {
                        return;
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append(userinfo.getName()).append(' ')
                        .append(userinfo.getRank()).append(' ')
                        .append(userinfo.getPp()).append(' ')
                        .append(userinfo.getAcc()).append(' ')
                        .append(userinfo.getLevel()).append(' ')
                        .append(userinfo.getCombo()).append(' ')
                        .append(userinfo.getThit()).append(' ')
                        .append(userinfo.getPcont()).append(' ')
                        .append(userinfo.getPtime()).append(' ')
                        .append(userinfo.getNotfc()).append(' ')
                        .append(userinfo.getRawpp()).append(' ')
                        .append(userinfo.getXx()).append(' ')
                        .append(userinfo.getXs()).append(' ')
                        .append(userinfo.getXa()).append(' ')
                        .append(userinfo.getXb()).append(' ')
                        .append(userinfo.getXc()).append(' ')
                        .append(userinfo.getXd()).append(' ')
                        .append(userinfo.getPpv0()).append(' ')
                        .append(userinfo.getAccv0()).append(' ')
                        .append(userinfo.getLengv0()).append(' ')
                        .append(userinfo.getPpv45()).append(' ')
                        .append(userinfo.getAccv45()).append(' ')
                        .append(userinfo.getLengv45()).append(' ')
                        .append(userinfo.getPpv90()).append(' ')
                        .append(userinfo.getAccv90()).append(' ')
                        .append(userinfo.getLengv90()).append(' ');

                grp.sendMessage(sb.toString());
                return;
            }
        }
        var pt = Pattern.compile("^[!！]roll(\\s+(?<num>[0-9]{1,5}))?");
        var matcher = pt.matcher(msg.contentToString());
        if (matcher.find()){
            if (matcher.group("num") != null){
                event.getSubject().sendMessage(String.valueOf(1 + (System.currentTimeMillis() % Integer.parseInt(matcher.group("num")))));
            }else {
                event.getSubject().sendMessage(String.valueOf(1 + (System.currentTimeMillis() % 100)));
            }
        }
    }
}
