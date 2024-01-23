package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.multiplayer.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchNowException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.regex.Matcher;

@Service("MATCH_NOW")
public class MatchNowService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MatchNowService.class);
    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MATCH_NOW.matcher(messageText);
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
        boolean failed = matcher.group("failed") == null || !matcher.group("failed").equalsIgnoreCase("f");

        try {
            matchID = Integer.parseInt(matcher.group("matchid"));
        } catch (NullPointerException e) {
            throw new MatchNowException(MatchNowException.Type.MN_MatchId_Error);
        }

        var from = event.getSubject();

        var data = parseData(matchID, skip, skipEnd, failed, rematch);

        byte[] img;
        try {
            img = imageService.getPanelF(data);
        } catch (Exception e) {
            throw new MatchNowException(MatchNowException.Type.MN_Render_Error);
        }

        try {
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("MN:", e);
            throw new MatchNowException(MatchNowException.Type.MN_Send_Error);
        }
    }

    public MatchData parseData(int matchID, int skip, int skipEnd, boolean failed, boolean rematch) throws MatchNowException {
        Match match;
        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MatchNowException(MatchNowException.Type.MN_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().getFirst().getId())) {
            var events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            if (events.isEmpty()) throw new MatchNowException(MatchNowException.Type.MN_Match_Empty);
            match.getEvents().addAll(0, events);
        }

        if (match.getEvents().size() - skipEnd - skip <= 0) {
            throw new MatchNowException(MatchNowException.Type.MN_Match_OutOfBoundsError);
        }

        MatchData matchData;
        try {
            matchData = new MatchData(match, skip, skipEnd, failed, rematch);
            matchData.calculate();

            //如果只有一两个人，则不排序（slot 从小到大）
            boolean isSize2p = !matchData.getRoundList().stream().filter(s -> s.getScoreInfoList().size() > 2).toList().isEmpty();

            for (MatchRound r : matchData.getRoundList()) {
                var scoreList = r.getScoreInfoList();

                if (isSize2p) {
                    r.setScoreInfoList(scoreList.stream().sorted(
                                    Comparator.comparingInt(MatchScore::getScore).reversed()).toList());
                } else {
                    r.setScoreInfoList(scoreList.stream().sorted(
                            Comparator.comparingInt(MatchScore::getSlot)).toList());
                }
            }

        } catch (Exception e) {
            log.error("MN Parse:", e);
            throw new MatchNowException(MatchNowException.Type.MN_Match_ParseError);
        }
        return matchData;
    }
}