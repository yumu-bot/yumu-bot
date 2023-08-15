package com.now.nowbot.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService.BphtService;
import com.now.nowbot.service.MessageService.MRAService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.channels.Channels;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pub", method = RequestMethod.GET)
@CrossOrigin("http://localhost:5173")
public class BotWebApi {
    private static final Logger log = LoggerFactory.getLogger(BotWebApi.class);
    @Resource
    OsuGetService osuGetService;
    @Resource
    BphtService bphtService;
    @Resource
    MRAService mraService;
    @Resource
    ImageService imageService;

    @GetMapping(value = "ppm", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPM(@RequestParam("u1") String user1, @Nullable @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        if (user2 != null) {
            return getPPMVS(user1, user2, playMode);
        }
        var mode = OsuMode.getMode(playMode);
        var info = osuGetService.getPlayerInfo(user1.trim(), mode);
        var bplist = osuGetService.getBestPerformance(info.getId(), mode, 0, 100);
        var ppm = Ppm.getInstance(mode, info, bplist);
        if (ppm == null) {
            throw new RuntimeException("ppm 请求失败：ppmMe 不存在");
        } else {
            return imageService.getPanelB(info, mode, ppm);
        }
    }

    @GetMapping(value = "ppmvs", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPMVS(@RequestParam("u1") String user1, @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        var mode = OsuMode.getMode(playMode);
        var info1 = osuGetService.getPlayerInfo(user1.trim());
        var info2 = osuGetService.getPlayerInfo(user2.trim());
        if (OsuMode.isDefault(mode)) mode = info1.getPlayMode();
        var bplist1 = osuGetService.getBestPerformance(info1.getId(), mode, 0, 100);
        var bplist2 = osuGetService.getBestPerformance(info2.getId(), mode, 0, 100);
        var ppm1 = Ppm.getInstance(mode, info1, bplist1);
        var ppm2 = Ppm.getInstance(mode, info2, bplist2);
        if (ppm1 == null || ppm2 == null) {
            throw new RuntimeException("ppm 请求失败：ppmMe/Other 不存在");
        } else {
            return imageService.getPanelB(info1, info2, ppm1, ppm2, mode);
        }
    }

    /***
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @param r include rematch
     * @return img
     */
    @GetMapping(value = "match", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getMatch(@RequestParam("id") int mid, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f, @Nullable Boolean r) {
        Match match = osuGetService.getMatchInfo(mid);
        int gameTime = 0;
        var m = match.getEvents().stream()
                .collect(Collectors.groupingBy(e -> e.getGame() == null, Collectors.counting()))
                .get(Boolean.FALSE);
        if (m != null) {
            gameTime = m.intValue();
        }
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId()) && gameTime < 40) {
            var next = osuGetService.getMatchInfo(mid, match.getEvents().get(0).getId());
            m = next.getEvents().stream()
                    .collect(Collectors.groupingBy(e -> e.getGame() == null, Collectors.counting()))
                    .get(Boolean.FALSE);
            if (m != null) {
                gameTime += m.intValue();
            }
            match.addEventList(next);
        }
        if (k == null) k = 0;
        if (d == null) d = 0;
        f = f != null;
        r = r != null;
        long t = System.currentTimeMillis();
        var b = imageService.getPanelF(match, osuGetService, k, d, f, r);
        System.out.println(System.currentTimeMillis() - t);
        return b;
    }

    /***
     *
     * @param matchId
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @param r include rematch
     * @return img
     */
    @GetMapping(value = "rating", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getRa(@RequestParam("id") int matchId, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f, @Nullable Boolean r) {
        if (k == null) k = 0;
        if (d == null) d = 0;
        f = f != null;
        r = r != null;
        return mraService.getDataImage(matchId, k, d, f, r);
    }

    @GetMapping(value = "bphti", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String getBPHTI(@RequestParam("u1") String userName, @Nullable @RequestParam("mode") String playMode) {
        BinUser nu = new BinUser();
        userName = userName.trim();
        long id = osuGetService.getOsuId(userName);
        nu.setOsuID(id);
        nu.setOsuName(userName);
        var mode = OsuMode.getMode(playMode);
        var Bps = osuGetService.getBestPerformance(nu, mode, 0, 100);
        var msg = bphtService.getAllMsg(Bps, userName, mode);
        StringBuilder sb = new StringBuilder();
        for (var s : msg) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @GetMapping(value = "bpht", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String getBPHT(@RequestParam("u1") String userName, @Nullable @RequestParam("mode") String playMode) {
        BinUser nu = new BinUser();
        userName = userName.trim();
        long id = osuGetService.getOsuId(userName);
        nu.setOsuID(id);
        nu.setOsuName(userName);
        var mode = OsuMode.getMode(playMode);
        var Bps = osuGetService.getBestPerformance(nu, mode, 0, 100);
        var msg = bphtService.getAllMsg(Bps, userName, mode.getName());
        StringBuilder sb = new StringBuilder();
        for (var s : msg) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @GetMapping(value = "bp", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPR(@RequestParam("u1") String userName,
                        @Nullable @RequestParam("mode") String playMode,
                        @Nullable @RequestParam("days") Integer days,
                        @Nullable @RequestParam("range") Integer range,
                        @Nullable @RequestParam("re") Boolean re,
                        @Nullable @RequestParam("pr") Boolean pr
    ) {
        var mode = OsuMode.getMode(playMode);
        userName = userName.trim();
        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(userName);
        List<Score> bps;
        if (pr != null && pr) {
            bps = osuGetService.getRecentN(infoMe.getId(), mode, 0, 100);
        } else if (re != null && re) {
            bps = osuGetService.getAllRecentN(infoMe.getId(), mode, 0, 100);
        } else if (range != null) {
            range = Math.max(5, Math.min(100, range + 1));
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0, range);
        } else if (days != null) {
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0, 100);
            // 时间计算
            int dat = -Math.max(1, Math.min(999, days));
            LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);
            bps = bps.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();
        } else {
            int dat = -1;
            LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0, 100);
            bps = bps.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();
        }
        var lines = new ArrayList<Image>(bps.size());
        try {
            var card = CardBuilder.getUserCard(infoMe);
            for (int i = 0; i < bps.size(); i++) {
                lines.add(new HCardBuilder(bps.get(i), i + 1).build());
            }
            var panel = new TBPPanelBuilder(lines.size());
            panel.drawBanner(PanelUtil.getBanner(null)).mainCrawCard(card.build()).drawBp(lines);
            return panel.build(mode == OsuMode.DEFAULT ? infoMe.getPlayMode() : mode)
                    .encodeToData(EncodedImageFormat.JPEG, 80)
                    .getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "score", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getScore(@RequestParam("u1") String userName,
                           @Nullable @RequestParam("mode") String playMode,
                           @Nullable @RequestParam("bp") Integer bps,
                           @Nullable @RequestParam("bid") Integer bid,
                           @Nullable @RequestParam("f") Boolean includeF
    ) {
        Score score;
        userName = userName.trim();
        var mode = OsuMode.getMode(playMode);
        long uid = osuGetService.getOsuId(userName);
        var userInfo = osuGetService.getPlayerInfo(uid, mode);
        if (bps != null) {
            bps = Math.min(99, bps - 1);
            bps = Math.max(0, bps);
            var scores = osuGetService.getBestPerformance(uid, mode, bps, 1);
            if (scores.size() == 0) throw new RuntimeException("bp不够");
            score = scores.get(0);
        } else if (bid != null) {
            try {
                score = osuGetService.getScore(bid, uid, mode).getScore();
            } catch (Exception e) {
                throw new RuntimeException("没打过");
            }
        } else if (Boolean.TRUE.equals(includeF)) {
            var scores = osuGetService.getAllRecentN(uid, mode, 0, 1);
            if (scores.size() == 0) throw new RuntimeException("最近没玩过");
            score = scores.get(0);
        } else {
            var scores = osuGetService.getRecentN(uid, mode, 0, 1);
            if (scores.size() == 0) throw new RuntimeException("最近没玩过");
            score = scores.get(0);
        }

        return imageService.getPanelE(userInfo, score, osuGetService);
    }

    @GetMapping(value = "bpa", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getBpa(@RequestParam("u1") String userName,
                         @Nullable @RequestParam("mode") String playMode
    ) {
        userName = userName.trim();
        var mode = OsuMode.getMode(playMode);
        long uid = osuGetService.getOsuId(userName);
        var userInfo = osuGetService.getPlayerInfo(uid, mode);
        if (mode != OsuMode.DEFAULT) userInfo.setPlayMode(mode.getName());
        var scores = osuGetService.getBestPerformance(uid, mode, 0, 100);
        return imageService.getPanelJ(userInfo, scores, osuGetService);
    }

    @GetMapping(value = "friend", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getFriend(@RequestParam("u1") String userName,
                            @Nullable @RequestParam("r1") Integer range1,
                            @Nullable @RequestParam("r2") Integer range2
    ) {
        BinUser nu = new BinUser();
        userName = userName.trim();
        long id = osuGetService.getOsuId(userName);
        nu.setOsuID(id);
        nu.setOsuName(userName);

        var me = osuGetService.getPlayerInfo(id);
        var allFriends = osuGetService.getFriendList(nu);
        List<MicroUser> friend = null;

        // 计算范围
        var n = 0;
        var m = 0;

        boolean doRandom = false;

        if (range1 == null) {
            m = 11;
            doRandom = true; //12个人
        } else if (range2 == null) {
            m = range1 - 1;
            doRandom = true;
        } else {
            n = Math.min(range1 - 1, range2 - 1);
            m = Math.max(range1 - 1, range2 - 1);
        }

        if (m - n < 0 || m - n > 100) throw new RuntimeException("输入范围错误！");

        //构造随机数组
        int[] index = null;
        if (doRandom) {
            index = new int[allFriends.size()];
            for (int i = 0; i < index.length; i++) {
                index[i] = i;
            }
            for (int i = 0; i < index.length; i++) {
                int rand = rand(i, index.length);
                if (rand != 1) {
                    int temp = index[rand];
                    index[rand] = index[i];
                    index[i] = temp;
                }
            }
        }

        //好友数据打包
        for (int i = n; i <= m && i < allFriends.size(); i++) {
            try {
                MicroUser infoO;
                if (doRandom) {
                    infoO = allFriends.get(index[i]);
                } else {
                    infoO = allFriends.get(i);
                }

                friend.add(infoO);
            } catch (Exception e) {
                throw new RuntimeException("卡片加载失败，报错信息为：\n{}", e);
            }
        }

        return imageService.getPanelA1(me, friend);
    }

    @GetMapping("file/{key}")
    public void downloadFile(@PathVariable("key") String key, HttpServletResponse response) throws IOException {
        var data = QQMsgUtil.getFileData(key);
        if (data == null) throw new RuntimeException("文件不存在");
        response.reset();
        response.setContentType("application/octet-stream");
        response.setContentLength(data.bytes().capacity());
        response.setHeader("Content-Disposition", "attachment;filename=" + data.name());
        var w = Channels.newChannel(response.getOutputStream());
        w.write(data.bytes());
        w.close();
    }

    @GetMapping("log-level")
    public String setLoggerLever(@RequestParam("l") String level) {
        var l = Level.toLevel(level, Level.INFO);
        ((LoggerContext)LoggerFactory.getILoggerFactory()).getLogger("com.now.nowbot").setLevel(l);
        log.trace("trace");
        log.debug("debug");
        log.info("info");
        log.warn("warn");
        log.error("error");

        return "ok - " + l.levelStr;
    }


    static final Random random = new Random();

    static int rand(int min, int max) {
        return min + random.nextInt(max - min);
    }
}

