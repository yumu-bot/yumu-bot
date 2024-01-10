package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("TEST")
public class TestService implements MessageService<Matcher> {
    private final Logger log = LoggerFactory.getLogger(TestService.class);
    OsuUserApiService userApiService;
    QQMessageDao qqMessageDao;
    @Resource
    ImageService imageService;
    @Resource
    RestTemplate restTemplate;

    private static final Pattern pattern =  Pattern.compile("!testname (?<ids>[0-9a-zA-Z\\[\\]\\-_ ,]+)");
    @Autowired
    public TestService(OsuUserApiService userApiService, QQMessageDao qqMessageDao) {
        this.userApiService = userApiService;
        this.qqMessageDao = qqMessageDao;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(test = true)
    public void HandleMessage(MessageEvent event, Matcher aaa) throws Throwable {
        var msg = event.getRawMessage();

        /*
        if (msg.startsWith("!testmd")){
            QQMsgUtil.sendImage(event.getSubject(), imageService.getMarkdownImage(removeFirstLine(msg),1080));
        } else if (msg.startsWith("!testwebmd")){
            var p = Pattern.compile("(?<url>(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])");
            var m = p.matcher(msg);
            if (m.find()) {
                var s = restTemplate.getForObject(m.group("url"), String.class);
                QQMsgUtil.sendImage(event.getSubject(), imageService.getMarkdownImage(removeFirstLine(s), 1080));
            }
        } else if (msg.startsWith("!testname")){

         */
        if (msg.startsWith("!testname")){
           var m = pattern.matcher(msg);
           if (m.find()){
               var names = m.group("ids").split(",");
               var nameList = new ArrayList<String>();

               for (var name:names){
                   var nt = name.trim();
                   if (!nt.isEmpty()) nameList.add(nt);
               }

               StringBuilder sb = new StringBuilder();
               for (var name:nameList){
                   Long id = 0L;
                   try {
                       id = userApiService.getOsuId(name);

                   } catch (Exception e) {
                       // do nothing
                   }
                   sb.append(id).append(',');
               }
               event.getSubject().sendMessage(sb.toString());
           }
           return;
        } else
        if (!(event instanceof GroupMessageEvent) || ((GroupMessageEvent) event).getGroup().getId() != 746671531L)
            return;
        var grp = ((GroupMessageEvent) event).getGroup();
        //这不是啥功能,这是木子取ppm 原始数据留下的接口,只允许他用,不需要单独做成功能
        //?
        var ppmPat = Pattern.compile("");
//        var mo = Pattern.compile("!testra(\\s+(?<id>\\d+))");
//        var mathcer_mo = mo.matcher(msg.contentToString());
//
//         if (mathcer_mo.find()) {
////                var s = osuGetService.getMatchInfo();
//            List<StringBuffer> sblist = new LinkedList<>();
//            mo(Integer.parseInt(mathcer_mo.group("id")), -1, sblist);
//
//            int flag = 1;
//            for (var kk : sblist) {
//                grp.sendMessage(kk.toString() + (flag++)).recallIn(100*1000);
//                Thread.sleep(1000);
//            }
//        }

        var ppmMch = ppmPat.matcher(msg);
        ppmPat = Pattern.compile("^[!！]roll(\\s+(?<num>[0-9]{1,5}))?");
        ppmMch = ppmPat.matcher(msg);
        if (ppmMch.find()) {
            if (ppmMch.group("num") != null) {
                event.getSubject().sendMessage(String.valueOf(1 + (int) (Math.random() * Integer.parseInt(ppmMch.group("num")))));
            } else {
                event.getSubject().sendMessage(String.valueOf(1 + (int) (Math.random() * 100)));
            }
        }


    }

    private void mo(int id, long eventid, List<StringBuffer> sbList) {
        JsonNode data = null;
//        if (eventid > 0) {
//            data = osuGetService.getMatchInfo(id, eventid);
//        } else {
//            data = osuGetService.getMatchInfo(id);
//        }
        var events = data.get("events");
        //因为ppy对大于100条event采用的是分页查询,先递归至房间创建的页,然后顺页执行
        if (!events.get(0).get("detail").get("type").asText().equals("match-created")) {
            mo(id, events.get(0).get("id").asLong(), sbList);
        }
        StringBuffer sb = new StringBuffer();
        sbList.add(sb);
        var f1 = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        var f2 = DateTimeFormatter.ofPattern("yy-MM-dd hh:mm:ss");
        int flag = 0;
        for (var node : events) {
            if (node.get("detail").get("type").asText("no").equals("other")) {
                var game = node.get("game");
                try {
                    sb.append(LocalDateTime.from(f1.parse(game.get("start_time").asText())).format(f2)).append(' ')
                            .append(LocalDateTime.from(f1.parse(game.get("end_time").asText(""))).format(f2)).append(' ')
                            .append(game.get("mode").asText()).append(' ')
                            .append(game.get("scoring_type").asText()).append(' ')
                            .append(game.get("team_type").asText()).append(' ')
                            .append((game.get("beatmap").get("difficulty_rating").asDouble())).append(' ')
                            .append(game.get("beatmap").get("total_length").asText()).append(' ')
                            .append(game.get("mods").toString()).append('\n');
                } catch (Exception e) {
                    sb.append("  error---->").append(e.getMessage()).append('\n');
                }
                flag++;
                for (var score : game.get("scores")) {
                    try {
                        sb.append(score.get("user_id").asText()).append(' ')
                                .append((score.get("accuracy").asText() + "     "), 0, 6).append(' ')
                                .append(score.get("mods").toString()).append(' ')
                                .append(score.get("score").asText()).append(' ')
                                .append(score.get("max_combo").asText()).append(' ')
                                .append(score.get("passed").asText()).append(' ')
                                .append(score.get("perfect").asInt() != 0).append(' ')
                                .append(score.get("match").get("slot").asText()).append(' ')
                                .append(score.get("match").get("team").asText()).append(' ')
                                .append(score.get("match").get("pass").asText()).append(' ');
                        sb.append("\n");
                    } catch (Exception e) {
                        sb.append("  error---->").append(e.getMessage()).append('\n');
                    }
                    flag++;
                }
                if (flag >= 25) {
                    sb = new StringBuffer();
                    sbList.add(sb);
                    flag = 0;
                }
            }
        }
    }

    public static String removeFirstLine(String str) {
        int firstNewLineIndex = str.indexOf('\n');
        if (firstNewLineIndex != -1) {
            return str.substring(firstNewLineIndex + 1);
        } else {
            return "";
        }
    }
}
