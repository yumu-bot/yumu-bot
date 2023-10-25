package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.match.GameInfo;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.model.score.MPScore;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("CRA")
public class CRAService implements MessageService<Matcher> {
    static DateTimeFormatter Date1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    static DateTimeFormatter Date2 = DateTimeFormatter.ofPattern("hh:mm:ss");
    OsuGetService osuGetService;

    @Autowired
    CRAService(OsuGetService osuGetService) {
        this.osuGetService = osuGetService;
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
            throw new MRAException(MRAException.Type.RATING_CRA_MatchIDNotFound);
        }

        StringBuilder sb = new StringBuilder();
        from.sendMessage("正在处理" + id);
        CRaCalculate(sb, id, isLite);

        //必须群聊
        if (from instanceof Group group) {
            try {
                group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), matcher.group("id") + ".csv");
            } catch (Exception e) {
                NowbotApplication.log.error("CRA:", e);
                throw new MRAException(MRAException.Type.RATING_CRA_Error);
                //from.sendMessage(e.getMessage());
            }
        } else {
            throw new MRAException(MRAException.Type.RATING_CRA_NotGroup);
        }
    }

    public void CRaCalculate(StringBuilder sb, int id, boolean isLite) throws MRAException {
        Match match;

        try {
            match = osuGetService.getMatchInfo(id);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_CRA_MatchIDNotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID())) {
            var events = osuGetService.getMatchInfo(id, match.getEvents().get(0).getID()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var events = match.getEvents();
        for (var e : events) {
            if (e.getGame() != null) {
                var game = e.getGame();
                getMatchStrings(sb, game);
                for (var score : game.getScoreInfoList()) {
                    if (isLite) {
                        getScoreStringsLite(sb, score);
                    } else {
                        getScoreStrings(sb, score);
                    }
                }
            }
        }
    }

    private void getMatchStrings(StringBuilder sb, GameInfo game){
        try {
            sb.append(game.getStartTime().format(Date1)).append(',')
                    .append(game.getStartTime().format(Date2)).append(',')
                    .append(game.getMode()).append(',')
                    .append(game.getScoringType()).append(',')
                    .append(game.getTeamType()).append(',')
                    .append((game.getBeatmap().getDifficultyRating())).append(',')
                    .append(game.getBeatmap().getTotalLength()).append(',')
                    .append(Arrays.toString(game.getMods()).replaceAll(", ", "|")).append(',')
                    .append(game.getBeatmap().getId()).append(',')
                    .append(osuGetService.getBeatMapInfo(game.getBeatmap().getId()).getMaxCombo())
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }

    private void getScoreStrings(StringBuilder sb, MPScore score){
        try {
            sb.append(score.getUserID()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getScore()).append(',')
                    .append(score.getMaxCombo()).append(',')
                    .append(score.getPassed()).append(',')
                    .append(score.getPerfect() != 0).append(',')
                    .append(score.getMatch().get("slot").asText()).append(',')
                    .append(score.getMatch().get("team").asText()).append(',')
                    .append(score.getMatch().get("pass").asText())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private void getScoreStringsLite(StringBuilder sb, MPScore score){

        try {
            String userName;

            try {
                userName = osuGetService.getPlayerInfo((long) score.getUserID()).getUsername();
            } catch (HttpClientErrorException e) {
                userName = score.getUserID().toString();
            }

            sb.append(score.getMatch().get("team").asText()).append(',')
                    .append(score.getUserID()).append(',')
                    .append(userName).append(',')
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
