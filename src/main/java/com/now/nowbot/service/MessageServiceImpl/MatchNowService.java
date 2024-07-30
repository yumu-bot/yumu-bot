package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.model.multiplayer.MatchCalculate;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchNowException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
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
    OsuBeatmapApiService beatmapApiService;
    @Resource
    OsuMatchApiService matchApiService;
    @Resource
    ImageService imageService;
    @Resource
    MuRatingService muRatingService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.MATCH_NOW.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var param = muRatingService.parseParam(matcher);
        var data = calculate(param, matchApiService, beatmapApiService);

        byte[] image;
        try {
            image = imageService.getPanelF(data);
        } catch (Exception e) {
            log.error("比赛结果：渲染图片失败");
            throw new MatchNowException(MatchNowException.Type.MN_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("比赛结果：发送失败", e);
            throw new MatchNowException(MatchNowException.Type.MN_Send_Error);
        }
    }

    public static MatchCalculate calculate(MuRatingService.MRAParam param, OsuMatchApiService matchApiService, OsuBeatmapApiService beatmapApiService) throws MatchNowException {

        Match match;
        try {
            match = matchApiService.getMatchInfo(param.matchID(), 10);
        } catch (Exception e) {
            throw new MatchNowException(MatchNowException.Type.MN_Match_NotFound);
        }

        while (!match.getFirstEventID().equals(match.getEvents().getFirst().getEventID())) {
            var events = matchApiService.getMatchInfo(param.matchID(), 10).getEvents();
            if (events.isEmpty()) throw new MatchNowException(MatchNowException.Type.MN_Match_Empty);
            match.getEvents().addAll(0, events);
        }

        if (match.getEvents().size() - param.calParam().ignore() - param.calParam().skip() <= 0) {
            throw new MatchNowException(MatchNowException.Type.MN_Match_OutOfBoundsError);
        }

        MatchCalculate c;
        try {
            c = new MatchCalculate(match, param.calParam(), beatmapApiService);

            //如果只有一两个人，则不排序（slot 从小到大）
            boolean isSize2p = ! c.getRounds().stream().filter(s -> s.getScores().size() > 2).toList().isEmpty();

            for (Match.MatchRound r : c.getRounds()) {
                var scoreList = r.getScores();

                if (isSize2p) {
                    r.setScores(scoreList.stream().sorted(
                                    Comparator.comparingInt(Match.MatchScore::getScore).reversed()).toList());
                } else {
                    r.setScores(scoreList.stream().sorted(
                            Comparator.comparingInt(s -> s.getPlayerStat().slot())).toList());
                }
            }

        } catch (Exception e) {
            log.error("比赛结果：获取失败", e);
            throw new MatchNowException(MatchNowException.Type.MN_Match_ParseError);
        }
        return c;
    }
}