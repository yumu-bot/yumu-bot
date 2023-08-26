package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MonitorNowException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service("MonitorNow")
public class MonitorNowService implements MessageService {
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService imageService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchID;
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        int deletEndRounds = matcher.group("deletendrounds") == null ? 0 : Integer.parseInt(matcher.group("deletendrounds"));
        boolean includingRematch = matcher.group("excludingrematch") == null || !matcher.group("excludingrematch").equalsIgnoreCase("r");
        boolean includingFail = matcher.group("excludingfail") == null || !matcher.group("excludingfail").equalsIgnoreCase("f");

        try {
            matchID = Integer.parseInt(matcher.group("matchid"));
        } catch (NullPointerException e) {
            throw new MonitorNowException(MonitorNowException.Type.MN_MatchId_Error);
        }

        var from = event.getSubject();
        try {
            var f = getImage(matchID, skipedRounds, deletEndRounds, includingFail, includingRematch);
            QQMsgUtil.sendImage(from, f);
        } catch (Exception e) {
            NowbotApplication.log.error("MonitorNow:", e);
            throw new MonitorNowException(MonitorNowException.Type.MN_Send_Error);
            //log.error("MonitorNow 数据请求失败。", e);
            //from.sendMessage("MonitorNow 渲染图片超时，请重试，或者将问题反馈给开发者。");
        }
    }

    public byte[] getImage(int matchID, int skipRounds, int deleteEnd, boolean includingFail, boolean includingRematch) throws MonitorNowException {
        Match match = null;
        try {
            match = osuGetService.getMatchInfo(matchID);
        } catch (Exception e) {
            NowbotApplication.log.error("mn 请求异常", e);
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_NotFound);
        }
        int gameTime = 0;
        var m = match.getEvents().stream()
                .collect(Collectors.groupingBy(e -> e.getGame() == null, Collectors.counting()))
                .get(Boolean.FALSE);
        if (m != null) {
            gameTime = m.intValue();
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID()) && gameTime < 40) {
            var next = osuGetService.getMatchInfo(matchID, match.getEvents().get(0).getID());
            m = next.getEvents().stream()
                    .collect(Collectors.groupingBy(e -> e.getGame() == null, Collectors.counting()))
                    .get(Boolean.FALSE);
            if (m != null) {
                gameTime += m.intValue();
            }
            match.addEventList(next);
        }

        if (gameTime <= 0) {
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_Empty);
        }
        if (gameTime - deleteEnd - skipRounds <= 0) {
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_OutOfBoundsError);
        }

        return imageService.getPanelF(match, osuGetService, skipRounds, deleteEnd, includingFail, includingRematch);
    }
}