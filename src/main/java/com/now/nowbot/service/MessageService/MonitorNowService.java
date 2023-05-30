package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
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
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        Match match = osuGetService.getMatchInfo(matchId);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var next = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId());
            match.addEventList(next);
        }
        var f = imageService.getPanelF(match, osuGetService);

        QQMsgUtil.sendImage(event.getSubject(), f);
    }
}
