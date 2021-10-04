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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
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
            var mo = Pattern.compile("!testmo(\\s+(?<id>\\d+))");
            var matcher = pt.matcher(msg.contentToString());
            var mathcer_mo = mo.matcher(msg.contentToString());
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
            }else if(mathcer_mo.find()){
                var s = osuGetService.getMatchInfo(Integer.parseInt(matcher.group("id")));
                var events = s.get("events");
                List<StringBuffer> sblist = new LinkedList<>();
                StringBuffer sb = new StringBuffer();
                var f1 = DateTimeFormatter.ISO_ZONED_DATE_TIME;
                var f2 = DateTimeFormatter.ofPattern("yy-MM-dd hh:mm:ss");
                int flag = 0;
                for (var node : events){
                    if (node.get("detail").get("type").asText("no").equals("other")){
                        var game = node.get("game");
                        sb.append(LocalDateTime.from(f1.parse(game.get("start_time").asText())).format(f2)).append(' ')
                                .append(LocalDateTime.from(f1.parse(game.get("end_time").asText())).format(f2)).append(' ')
                                .append(game.get("mode").asText()).append(' ')
                                .append(game.get("scoring_type").asText()).append(' ')
                                .append(game.get("team_type").asText()).append(' ')
                                .append(game.get("beatmap").get("difficulty_rating").asText().substring(0,4))
                                .append(game.get("beatmap").get("total_length").asText())
                                .append(game.get("mods").toString()).append('\n');
                        flag++;
                        for (var score: game.get("scores")){
                            sb.append(score.get("user_id").asText()).append(' ')
                                    .append(score.get("accuracy").asText().substring(0,6)).append(' ')
                                    .append(score.get("mods").toString()).append(' ')
                                    .append(score.get("score").asText()).append(' ')
                                    .append(score.get("max_combo").asText()).append(' ')
                                    .append(score.get("passed").asText()).append(' ')
                                    .append(score.get("perfect").asText()).append(' ')
                                    .append(score.get("match").get("slot").asText()).append(' ')
                                    .append(score.get("match").get("team").asText()).append(' ')
                                    .append(score.get("match").get("pass").asText()).append(' ');
                            sb.append("\n");
                            flag++;
                        }
                        if (flag >= 25){
                            sblist.add(sb);
                            sb = new StringBuffer();
                        }
                    }
                }
                flag = 1;
                for (var kk : sblist){
                    grp.sendMessage((flag++) +kk.toString());
                    Thread.sleep(1000);
                }
            }
        }
        var pt = Pattern.compile("^[!！]roll(\\s+(?<num>[0-9]{1,5}))?");
        var matcher = pt.matcher(msg.contentToString());
        if (matcher.find()){
            if (matcher.group("num") != null){
                event.getSubject().sendMessage(String.valueOf(1 + (int)(Math.random() * Integer.parseInt(matcher.group("num")))));
            }else {
                event.getSubject().sendMessage(String.valueOf(1 +(int)(Math.random() * 100)));
            }
        }
    }
}
