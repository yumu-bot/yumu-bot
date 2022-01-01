package com.now.nowbot.service.MessageService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        JsonNode data;
        if (eventid > 0) {
            data = osuGetService.getMatchInfo(id, eventid);
        } else {
            data = osuGetService.getMatchInfo(id);
        }
        var events = data.get("events");
        //因为ppy对大于100条event采用的是分页查询,先递归至房间创建的页,然后顺页执行
        if (!events.get(0).get("detail").get("type").asText().equals("match-created")) {
            mo(id, events.get(0).get("id").asLong(), strData);
        }


        for (var node : events) {
            if (node.get("detail").get("type").asText("no").equals("other")) {
                var game = node.get("game");
                try {
                    strData.append(LocalDateTime.from(f1.parse(game.get("start_time").asText())).format(f2)).append(' ')
                            .append(LocalDateTime.from(f1.parse(game.get("end_time").asText(""))).format(f2)).append(' ')
                            .append(game.get("mode").asText()).append(' ')
                            .append(game.get("scoring_type").asText()).append(' ')
                            .append(game.get("team_type").asText()).append(' ')
                            .append((game.get("beatmap").get("difficulty_rating").asDouble())).append(' ')
                            .append(game.get("beatmap").get("total_length").asText()).append(' ')
                            .append(game.get("mods").toString()).append(' ')
                            .append(game.get("beatmap").get("id").asInt()).append(' ')
                            .append('\n');
                } catch (Exception e) {
                    strData.append(e.getMessage()).append('\n');//.append("  error---->")
                }
                for (var score : game.get("scores")) {
                    try {
                        strData.append(score.get("user_id").asText()).append(' ')
                                .append((score.get("accuracy").asText() + "     "), 0, 6).append(' ')
                                .append(score.get("mods").toString()).append(' ')
                                .append(score.get("score").asText()).append(' ')
                                .append(score.get("max_combo").asText()).append(' ')
                                .append(score.get("passed").asText()).append(' ')
                                .append(score.get("perfect").asInt() != 0).append(' ')
                                .append(score.get("match").get("slot").asText()).append(' ')
                                .append(score.get("match").get("team").asText()).append(' ')
                                .append(score.get("match").get("pass").asText()).append(' ')
                                .append("\n");
                    } catch (Exception e) {
                        strData.append("  error---->").append(e.getMessage()).append('\n');
                    }
                }
            }
        }
    }
}
