package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;

@Service("t-ra")
public class TestRAService implements MessageService {
    static DateTimeFormatter Date1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    static DateTimeFormatter Date2 = DateTimeFormatter.ofPattern("hh:mm:ss");
    OsuGetService osuGetService;

    @Autowired
    TestRAService(OsuGetService osuGetService) {
        this.osuGetService = osuGetService;
    }

    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        int id;

        try {
            id = Integer.parseInt(matcher.group("id"));
        } catch (NullPointerException e) {
            throw new MRAException(MRAException.Type.RATING_TRA_None);
        }

        StringBuilder sb = new StringBuilder();
        from.sendMessage("正在处理" + id);
        testRaCalculate(id, sb);

        if (from instanceof Group group) {
            try {
                group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), matcher.group("id") + ".csv");
            } catch (Exception e) {
                from.sendMessage(e.getMessage());
            }
        } else {
            throw new MRAException(MRAException.Type.RATING_TRA_NotGroup);
            //from.sendMessage("私聊不行");
        }
    }

    public void testRaCalculate(int id, StringBuilder strData) {
        Match match = osuGetService.getMatchInfo(id);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID())) {
            var events = osuGetService.getMatchInfo(id, match.getEvents().get(0).getID()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var events = match.getEvents();
        for (var node : events) {
            if (node.getGame() != null) {
                var game = node.getGame();
                try {
                    strData.append(game.getStartTime().format(Date1)).append(',')
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
                    strData.append(e.getMessage()).append('\n');//.append("  error---->")
                }
                for (var score : game.getScoreInfos()) {
                    try {
                        strData.append(score.getUserId()).append(',')
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
                        strData.append("  error---->").append(e.getMessage()).append('\n');
                    }
                }
            }
        }
    }
}
