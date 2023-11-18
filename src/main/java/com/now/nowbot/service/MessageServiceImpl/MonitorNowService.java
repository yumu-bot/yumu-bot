package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.multiplayer.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MonitorNowException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("MONITORNOW")
public class MonitorNowService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MonitorNowService.class);
    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;
    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(monitornow|monow|mn(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skip>\\d+))?(\\s*(?<skipend>\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchID;

        int skip = matcher.group("skip") == null ? 0 : Integer.parseInt(matcher.group("skip"));
        int skipEnd = matcher.group("skipend") == null ? 0 : Integer.parseInt(matcher.group("skipend"));
        boolean rematch = matcher.group("rematch") == null || !matcher.group("rematch").equalsIgnoreCase("r");
        boolean keep = matcher.group("keep") == null || !matcher.group("keep").equalsIgnoreCase("f");

        try {
            matchID = Integer.parseInt(matcher.group("matchid"));
        } catch (NullPointerException e) {
            throw new MonitorNowException(MonitorNowException.Type.MN_MatchId_Error);
        }

        var from = event.getSubject();

        var f = getImage(matchID, skip, skipEnd, !keep, rematch);

        try {
            QQMsgUtil.sendImage(from, f);
        } catch (Exception e) {
            NowbotApplication.log.error("MonitorNow:", e);
            throw new MonitorNowException(MonitorNowException.Type.MN_Send_Error);
        }
    }

    public byte[] getImage(int matchID, int skip, int skipEnd, boolean remove, boolean rematch) throws MonitorNowException {
        Match match;
        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            if (events.isEmpty()) throw new MonitorNowException(MonitorNowException.Type.MN_Match_Empty);
            match.getEvents().addAll(0, events);
        }

        if (match.getEvents().size() - skipEnd - skip <= 0) {
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_OutOfBoundsError);
        }

        MatchCal cal;
        try {
            cal = new MatchCal(match, skip, skipEnd, remove, rematch);
            cal.addMicroUser4MatchScore();
            cal.addRanking4MatchScore();

            for (MatchRound r : cal.getRoundList()) {
                var scoreList = r.getScoreInfoList();

                //如果只有一两个人，则不排序（slot 从小到大）
                if (scoreList.size() > 2) {
                    r.setScoreInfoList(scoreList.stream().sorted(
                                    Comparator.comparingInt(MatchScore::getScore).reversed()).toList());
                } else {
                    r.setScoreInfoList(scoreList.stream().sorted(
                            Comparator.comparingInt(MatchScore::getSlot)).toList());
                }
            }

        } catch (Exception e) {
            log.error("MN Parse:", e);
            throw new MonitorNowException(MonitorNowException.Type.MN_Match_ParseError);
        }

        byte[] img;
        try {
            img = imageService.getPanelF(match.getMatchStat(), cal.getRoundList());
        } catch (Exception e) {
            throw new MonitorNowException(MonitorNowException.Type.MN_Render_Error);
        }

        return img;
    }
}