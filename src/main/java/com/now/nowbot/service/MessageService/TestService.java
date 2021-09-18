package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("test")
public class TestService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (event instanceof GroupMessageEvent && ((GroupMessageEvent) event).getGroup().getId() == 746671531L) {
            var msg = event.getMessage();
            var grp = ((GroupMessageEvent) event).getGroup();

            var pt = Pattern.compile("!info([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");
            var match = pt.matcher(msg.contentToString());
            if (match.find()) {
                PPmObject userinfo = null;
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
                                userinfo = PPmObject.presOsu(userdate, bpdate);
                            } else {
                                var user = BindingUtil.readUser(event.getSender().getId());
                                userdate = osuGetService.getPlayerManiaInfo(user);
                                var bpdate = osuGetService.getManiaBestMap(user, 0, 100);
                                userinfo = PPmObject.presCatch(userdate, bpdate);
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
                sb.append("name").append(' ').append(userinfo.getName()).append('\n');
                sb.append("rank").append(' ').append(userinfo.getRank()).append('\n');
                sb.append("PP").append(' ').append(userinfo.getPp()).append('\n');
                sb.append("acc").append(' ').append(userinfo.getAcc()).append('\n');
                sb.append("level").append(' ').append(userinfo.getLevel()).append('\n');
                sb.append("max_cb").append(' ').append(userinfo.getCombo()).append('\n');
                sb.append("tth").append(' ').append(userinfo.getThit()).append('\n');
                sb.append("pc").append(' ').append(userinfo.getPcont()).append('\n');
                sb.append("time").append(' ').append(userinfo.getPtime()).append('\n');
                sb.append("no s rank:").append(' ').append(userinfo.getNotfc()).append('\n');
                sb.append("rowPP:").append(' ').append(userinfo.getRawpp()).append('\n');
                sb.append("ss数:").append(' ').append(userinfo.getXx()).append('\n');
                sb.append("s数:").append(' ').append(userinfo.getXs()).append('\n');
                sb.append("a数:").append(' ').append(userinfo.getXa()).append('\n');
                sb.append("b数:").append(' ').append(userinfo.getXb()).append('\n');
                sb.append("c数:").append(' ').append(userinfo.getXc()).append('\n');
                sb.append("前10:\n");
                sb.append("pp").append(' ').append(userinfo.getPpv0()).append('\n');
                sb.append("acc").append(' ').append(userinfo.getAccv0()).append('\n');
                sb.append("length").append(' ').append(userinfo.getLengv0()).append('\n');
                sb.append("45:\n");
                sb.append("pp").append(' ').append(userinfo.getPpv45()).append('\n');
                sb.append("acc").append(' ').append(userinfo.getAccv45()).append('\n');
                sb.append("length").append(' ').append(userinfo.getLengv45()).append('\n');
                sb.append("90:\n");
                sb.append("pp").append(' ').append(userinfo.getPpv90()).append('\n');
                sb.append("acc").append(' ').append(userinfo.getAccv90()).append('\n');
                sb.append("length").append(' ').append(userinfo.getLengv90()).append('\n');

                grp.sendMessage(sb.toString());
            }
        }

    }
}
