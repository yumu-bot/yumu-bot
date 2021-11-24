package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.Rect;
import org.jetbrains.skija.Surface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("test")
public class TestService implements MessageService {
    private final Logger log = LoggerFactory.getLogger(TestService.class);

    OsuGetService osuGetService;
    QQMessageDao qqMessageDao;
    @Autowired
    public TestService(OsuGetService osuGetService, QQMessageDao qqMessageDao){
        this.osuGetService = osuGetService;
        this.qqMessageDao = qqMessageDao;
    }

    @Override
    @CheckPermission(isSuper = true)
    public void HandleMessage(MessageEvent event, Matcher aaa) throws Throwable {
        replay(event);
        var msg = event.getMessage();
        if (!(event instanceof GroupMessageEvent) || ((GroupMessageEvent) event).getGroup().getId() != 746671531L)
            return;
        var grp = ((GroupMessageEvent) event).getGroup();
        //这不是啥功能,这是木子取ppm 原始数据留下的接口,只允许他用,不需要单独做成功能
        var pt = Pattern.compile("!testppm([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");
        var mo = Pattern.compile("!testra(\\s+(?<id>\\d+))");
        var matcher = pt.matcher(msg.contentToString());
        var mathcer_mo = mo.matcher(msg.contentToString());

        if (matcher.find()) {
            PPmObject userinfo;
            JSONObject userdate;
            var mode = OsuMode.getMode(matcher.group("mode"));
            switch (mode) {
                case OSU: {
                    {
                        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                            int id = osuGetService.getOsuId(matcher.group("name").trim());
                            userdate = osuGetService.getPlayerOsuInfo(id);
                            var bpdate = osuGetService.getOsuBestMap(id, 0, 100);
                            userinfo = PPmObject.presOsu(userdate, bpdate);
                        } else {
                            var user = BindingUtil.readUser(event.getSender().getId());
                            userdate = osuGetService.getPlayerOsuInfo(user);
                            var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                            userinfo = PPmObject.presOsu(userdate, bpdate);
                        }
                    }
                }
                break;
                case TAIKO: {
                    {
                        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                            int id = osuGetService.getOsuId(matcher.group("name").trim());
                            userdate = osuGetService.getPlayerTaikoInfo(id);
                            var bpdate = osuGetService.getTaikoBestMap(id, 0, 100);
                            userinfo = PPmObject.presTaiko(userdate, bpdate);
                        } else {
                            var user = BindingUtil.readUser(event.getSender().getId());
                            userdate = osuGetService.getPlayerTaikoInfo(user);
                            var bpdate = osuGetService.getTaikoBestMap(user, 0, 100);
                            userinfo = PPmObject.presTaiko(userdate, bpdate);
                        }
                    }
                }
                break;
                case CATCH: {
                    {
                        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                            int id = osuGetService.getOsuId(matcher.group("name").trim());
                            userdate = osuGetService.getPlayerCatchInfo(id);
                            var bpdate = osuGetService.getCatchBestMap(id, 0, 100);
                            userinfo = PPmObject.presCatch(userdate, bpdate);
                        } else {
                            var user = BindingUtil.readUser(event.getSender().getId());
                            userdate = osuGetService.getPlayerCatchInfo(user);
                            var bpdate = osuGetService.getCatchBestMap(user, 0, 100);
                            userinfo = PPmObject.presCatch(userdate, bpdate);
                        }
                    }
                }
                break;
                case MANIA:{
                    {
                        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                            int id = osuGetService.getOsuId(matcher.group("name").trim());
                            userdate = osuGetService.getPlayerManiaInfo(id);
                            var bpdate = osuGetService.getManiaBestMap(id, 0, 100);
                            userinfo = PPmObject.presMania(userdate, bpdate);
                        } else {
                            var user = BindingUtil.readUser(event.getSender().getId());
                            userdate = osuGetService.getPlayerManiaInfo(user);
                            var bpdate = osuGetService.getManiaBestMap(user, 0, 100);
                            userinfo = PPmObject.presMania(userdate, bpdate);
                        }
                    }
                }
                break;
                default: {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(userinfo.getName()).append(' ')
                    .append(userinfo.getRank()).append(' ')
                    .append(userinfo.getPp()).append(' ')
                    .append(userinfo.getAcc()).append(' ')
                    .append(userinfo.getLevel()).append(' ')
                    .append(userinfo.getCombo()).append(' ')
                    .append(userinfo.getThit()).append(' ')
                    .append(userinfo.getPcont()).append(' ')
                    .append(userinfo.getPtime()).append(' ')
                    .append(userinfo.getNotfc()).append(' ')
                    .append(userinfo.getRawpp()).append(' ')
                    .append(userinfo.getXx()).append(' ')
                    .append(userinfo.getXs()).append(' ')
                    .append(userinfo.getXa()).append(' ')
                    .append(userinfo.getXb()).append(' ')
                    .append(userinfo.getXc()).append(' ')
                    .append(userinfo.getXd()).append(' ')
                    .append(userinfo.getPpv0()).append(' ')
                    .append(userinfo.getAccv0()).append(' ')
                    .append(userinfo.getLengv0()).append(' ')
                    .append(userinfo.getPpv45()).append(' ')
                    .append(userinfo.getAccv45()).append(' ')
                    .append(userinfo.getLengv45()).append(' ')
                    .append(userinfo.getPpv90()).append(' ')
                    .append(userinfo.getAccv90()).append(' ')
                    .append(userinfo.getLengv90()).append(' ');

            grp.sendMessage(sb.toString());
            return;
        } else if (mathcer_mo.find()) {
//                var s = osuGetService.getMatchInfo();
            List<StringBuffer> sblist = new LinkedList<>();
            mo(Integer.parseInt(mathcer_mo.group("id")), -1, sblist);

            int flag = 1;
            for (var kk : sblist) {
                grp.sendMessage(kk.toString() + (flag++));
                Thread.sleep(1000);
            }
        }

        pt = Pattern.compile("^[!！]roll(\\s+(?<num>[0-9]{1,5}))?");
        matcher = pt.matcher(msg.contentToString());
        if (matcher.find()) {
            if (matcher.group("num") != null) {
                event.getSubject().sendMessage(String.valueOf(1 + (int) (Math.random() * Integer.parseInt(matcher.group("num")))));
            } else {
                event.getSubject().sendMessage(String.valueOf(1 + (int) (Math.random() * 100)));
            }
            return;
        }


    }

    private void mo(int id, long eventid, List<StringBuffer> sbList) {
        JsonNode data;
        if (eventid > 0) {
            data = osuGetService.getMatchInfo(id, eventid);
        } else {
            data = osuGetService.getMatchInfo(id);
        }
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
    private void replay(MessageEvent event) throws IOException {
        Pattern p = Pattern.compile("bg\\s+(?<bk>\\d{1,3})?(\\s*(?<yl>ylbx))?");
        Matcher m = p.matcher(event.getMessage().contentToString());
        if (!m.find()) return;

        boolean stl = m.group("yl") != null;
        int an = m.group("bk") == null ? 0 : Integer.parseInt(m.group("bk"));
        if (an>=100) an = 99;
        Image img;
        QuoteReply reply = event.getMessage().get(QuoteReply.Key);
        if (reply == null) return;

        var msg = qqMessageDao.getReply(reply);
        img = (Image) msg.stream().filter(it -> it instanceof Image).findFirst().orElse(
                event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null)
        ) ;

        if (img == null) {
            event.getSubject().sendMessage("没有任何图片");
            return;
        }
        var skijaimg = SkiaUtil.getScaleCenterImage(SkiaUtil.lodeNetWorkImage(Image.queryUrl(img)), 1200,857);

        var surface = Surface.makeRaster(skijaimg.getImageInfo());
        var t1 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "panel05.png");
        var t2 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + (stl?"ylbx.png":"lbx.png"));

        byte[] data;
        try(skijaimg;surface;t1;t2){
            var canvas = surface.getCanvas();
            canvas.drawImage(skijaimg,0,0);
            canvas.drawRect(Rect.makeWH(surface.getWidth(),surface.getHeight()),new Paint().setARGB((int)(255f*an/100),0,0,0));
            canvas.drawImage(t1,0,0);
            canvas.drawImage(t2,0,0);
            data =
            skijaimg.encodeToData(EncodedImageFormat.PNG).getBytes();
        }

        event.getSubject().sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(data),event.getSubject()));
    }
}
