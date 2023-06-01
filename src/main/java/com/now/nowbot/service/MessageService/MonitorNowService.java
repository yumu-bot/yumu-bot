package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.match.Match;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Matcher;

@Service("MonitorNow")
public class MonitorNowService implements MessageService{
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService imageService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        int deletEndRounds = matcher.group("deletendrounds") == null ? 0 : Integer.parseInt(matcher.group("deletendrounds"));
        boolean includingFail = matcher.group("includingfail") == null || !matcher.group("includingfail").equals("0");
        Match match = osuGetService.getMatchInfo(matchId);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var next = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId());
            match.addEventList(next);
        }
        var f = imageService.getPanelF(match, osuGetService, skipedRounds, deletEndRounds, includingFail);

        QQMsgUtil.sendImage(event.getSubject(), f);
    }
}