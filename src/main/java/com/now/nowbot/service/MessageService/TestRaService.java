package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;

@Service("t-ra")
public class TestRaService implements MessageService {
    static DateTimeFormatter f1 = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    static DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yy-MM-dd hh:mm:ss");
    OsuGetService osuGetService;
    @Autowired
    TestRaService(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }

    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        StringBuffer sb = new StringBuffer();
        from.sendMessage("正在处理"+matcher.group("id"));
        mo(Integer.parseInt(matcher.group("id")), -1, sb);

        if (from instanceof Group group){
            try {
                var f = group.getFiles().uploadNewFile(System.currentTimeMillis()+".txt", ExternalResource.create(sb.toString().getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                from.sendMessage(e.getMessage());
            }
        } else {
            from.sendMessage("私聊不行");
        }
//        AbsoluteFileFolder
    }

    void mo(int id, long eventid, StringBuffer strData) {
        Match match =osuGetService.getMatchInfo(id);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuGetService.getMatchInfo(id, match.getEvents().get(0).getId()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var events = match.getEvents();
        for (var node : events) {
            if (node.getGame() != null) {
                var game = node.getGame();
                try {
                    strData.append(LocalDateTime.from(f1.parse(game.getScoringType())).format(f2)).append(' ')
                            .append(game.getMode()).append(' ')
                            .append(game.getScoringType()).append(' ')
                            .append(game.getTeamType()).append(' ')
                            .append((game.getBeatmap().getDifficultyRating())).append(' ')
                            .append(game.getBeatmap().getTotalLength()).append(' ')
                            .append(Arrays.toString(game.getMods())).append(' ')
                            .append(game.getBeatmap().getId()).append(' ')
                            .append(osuGetService.getMapInfo(game.getBeatmap().getId()).getMaxCombo()).append(' ')
                            .append('\n');
                } catch (Exception e) {
                    strData.append(e.getMessage()).append('\n');//.append("  error---->")
                }
                for (var score : game.getScoreInfos()) {
                    try {
                        strData.append(score.getUserId()).append(' ')
                                .append((score.getAccuracy() + "     "), 0, 6).append(' ')
                                .append(Arrays.toString(score.getMods())).append(' ')
                                .append(score.getScore()).append(' ')
                                .append(score.getMaxCombo()).append(' ')
                                .append(score.getPassed()).append(' ')
                                .append(score.getPerfect() != 0).append(' ')
                                .append(score.getMatch().get("slot").asText()).append(' ')
                                .append(score.getMatch().get("team").asText()).append(' ')
                                .append(score.getMatch().get("pass").asText()).append(' ')
                                .append("\n");
                    } catch (Exception e) {
                        strData.append("  error---->").append(e.getMessage()).append('\n');
                    }
                }
            }
        }
    }
}
