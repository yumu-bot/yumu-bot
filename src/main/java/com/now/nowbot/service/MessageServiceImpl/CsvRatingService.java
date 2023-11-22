package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.model.multiplayer.MatchScore;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("CRA")
public class CsvRatingService implements MessageService<Matcher> {
    static DateTimeFormatter Date1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    static DateTimeFormatter Date2 = DateTimeFormatter.ofPattern("hh:mm:ss");

    OsuMatchApiService osuMatchApiService;

    @Autowired
    CsvRatingService(OsuMatchApiService osuMatchApiService) {
        this.osuMatchApiService = osuMatchApiService;
    }

    Pattern pattern = Pattern.compile("[!！]\\s*(?i)((ym)?(csvrating|cr(?![a-wy-zA-WY-Z_])|cra(?![a-wy-zA-WY-Z_])))+\\s*(?<x>[xX])?\\s*(?<id>\\d+)?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isGroupAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var isLite = matcher.group("x") != null;
        int id;

        try {
            id = Integer.parseInt(matcher.group("id"));
        } catch (NullPointerException e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        StringBuilder sb = new StringBuilder();
        from.sendMessage("正在处理" + id);
        CRACalculate(sb, id, isLite);

        //必须群聊
        if (from instanceof Group group) {
            try {
                group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), matcher.group("id") + ".csv");
            } catch (Exception e) {
                NowbotApplication.log.error("CRA:", e);
                throw new MRAException(MRAException.Type.RATING_Send_CRAFailed);
                //from.sendMessage(e.getMessage());
            }
        } else {
            throw new MRAException(MRAException.Type.RATING_Send_NotGroup);
        }
    }

    public void CRACalculate(StringBuilder sb, int matchID, boolean isLite) throws MRAException {
        Match match;

        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            match.getEvents().addAll(0, events);
        }

        var events = match.getEvents();
        for (var e : events) {
            if (Objects.equals(e.getDetail().type(), "other") && e.getRound() != null && e.getRound().getScoreInfoList() != null) {
                var round = e.getRound();
                getMatchStrings(sb, round);
                for (var score : round.getScoreInfoList()) {
                    if (isLite) {
                        getScoreStringsLite(sb, score);
                    } else {
                        getScoreStrings(sb, score);
                    }
                }
            }
        }
    }

    private void getMatchStrings(StringBuilder sb, MatchRound round){
        try {
            sb.append(round.getStartTime().format(Date1)).append(',')
                    .append(round.getStartTime().format(Date2)).append(',')
                    .append(round.getMode()).append(',')
                    .append(round.getScoringType()).append(',')
                    .append(round.getTeamType()).append(',')
                    .append((round.getBeatmap().getDifficultyRating())).append(',')
                    .append(round.getBeatmap().getTotalLength()).append(',')
                    .append(round.getMods().toString().replaceAll(", ", "|")).append(',')
                    .append(round.getBeatmap().getId()).append(',')
                    .append(round.getBeatmap().getMaxCombo())
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }

    private void getScoreStrings(StringBuilder sb, MatchScore score){
        try {
            sb.append(score.getUserId()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getScore()).append(',')
                    .append(score.getMaxCombo()).append(',')
                    .append(score.isPassed()).append(',')
                    .append(score.isPerfect()).append(',')
                    .append(score.getSlot()).append(',')
                    .append(score.getTeam()).append(',')
                    .append(score.isPass())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private void getScoreStringsLite(StringBuilder sb, MatchScore score){

        try {
            sb.append(score.getTeam()).append(',')
                    .append(score.getUserId()).append(',')
                    .append(score.getUserName()).append(',')
                    .append(score.getScore()).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getMaxCombo()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }
}
