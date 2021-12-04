package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("t-ppm")
public class TestPPMService implements MessageService {
    @Autowired
    public TestPPMService(OsuGetService osuGetService) {
        this.osuGetService = osuGetService;
    }

    private OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        PPmObject userinfo;
        JSONObject userdate;
        var mode = OsuMode.getMode(matcher.group("mode"));
        switch (mode) {
            case OSU: {
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
            }
            break;
            case TAIKO: {
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
            }
            break;
            case CATCH: {
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
            }
            break;
            case MANIA: {
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
                .append(userinfo.getHC0()).append(' ')
                .append(userinfo.getPpv45()).append(' ')
                .append(userinfo.getAccv45()).append(' ')
                .append(userinfo.getLengv45()).append(' ')
                .append(userinfo.getHC45()).append(' ')
                .append(userinfo.getPpv90()).append(' ')
                .append(userinfo.getAccv90()).append(' ')
                .append(userinfo.getLengv90()).append(' ')
                .append(userinfo.getHC90()).append(' ');

        event.getSubject().sendMessage(sb.toString());
    }
}
