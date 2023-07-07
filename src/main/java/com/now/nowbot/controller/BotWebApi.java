package com.now.nowbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.now.nowbot.model.BinUser;
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
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pub", method = RequestMethod.GET)
public class BotWebApi {
    @Resource
    OsuGetService osuGetService;
    @Resource
    BphtService   bphtService;
    @Resource
    MRAService mraService;
    @Resource
    ImageService  imageService;

    @GetMapping(value = "ppm", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPM(@RequestParam("u1") String user1, @Nullable @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        if (user2 != null) {
            return getPPMVS(user1, user2, playMode);
        }
        var mode = OsuMode.getMode(playMode);
        var info = osuGetService.getPlayerInfo(user1);
        if (mode == OsuMode.DEFAULT) mode = info.getPlayMode();
        var bplist = osuGetService.getBestPerformance(info.getId(), mode, 0, 100);
        var ppm = Ppm.getInstance(mode, info, bplist);
        return imageService.getPanelB(info, mode, ppm);
    }

    @GetMapping(value = "ppmvs", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPMVS(@RequestParam("u1") String user1, @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        var mode = OsuMode.getMode(playMode);
        var info1 = osuGetService.getPlayerInfo(user1);
        var info2 = osuGetService.getPlayerInfo(user2);
        if (mode == OsuMode.DEFAULT) mode = info1.getPlayMode();
        var bplist1 = osuGetService.getBestPerformance(info1.getId(), mode, 0, 100);
        var bplist2 = osuGetService.getBestPerformance(info2.getId(), mode, 0, 100);
        var ppm1 = Ppm.getInstance(mode, info1, bplist1);
        var ppm2 = Ppm.getInstance(mode, info2, bplist2);
        return imageService.getPanelB(info1, info2, ppm1, ppm2, mode);
    }

    /***
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @return img
     */
    @GetMapping(value = "match", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getMatch(@RequestParam("id") int mid, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f) {
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
        if (f == null) f = false;
        long t = System.currentTimeMillis();
        var b = imageService.getPanelF(match, osuGetService, k, d, false);
        System.out.println(System.currentTimeMillis() - t);
        return b;
    }

    /***
     *
     * @param matchId
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @return img
     */
    @GetMapping(value = "rating", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getRa(@RequestParam("id") int matchId, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f) {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (f == null) f = false;
        return mraService.getDataImage(matchId, k, d, f);
    }

    @GetMapping(value = "bphti", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String getBPHTI(@RequestParam("u1") String userName, @Nullable @RequestParam("mode") String playMode) {
        BinUser nu = new BinUser();
        long id = osuGetService.getOsuId(userName);
        nu.setOsuID(id);
        nu.setOsuName(userName);
        var mode = OsuMode.getMode(playMode);
        var Bps = osuGetService.getBestPerformance(nu, mode, 0, 100);
        var msg = bphtService.getAllMsg(Bps, userName, mode, nu);
        StringBuilder sb = new StringBuilder();
        for (var s : msg) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @GetMapping(value = "bpht", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String getBPHT(@RequestParam("u1") String userName, @Nullable @RequestParam("mode") String playMode) {
        BinUser nu = new BinUser();
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
                        @Nullable @RequestParam("limit") Integer limit,
                        @Nullable @RequestParam("re") Boolean re,
                        @Nullable @RequestParam("pr") Boolean pr
    ) {
        var mode = OsuMode.getMode(playMode);
        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(userName);
        List<Score> bps;
        if (pr != null && pr) {
            bps = osuGetService.getRecentN(infoMe.getId(), mode, 0, 100);
        } else if (re != null && re) {
            bps = osuGetService.getAllRecentN(infoMe.getId(), mode, 0, 100);
        } else  if (limit != null) {
            limit = Math.max(5, Math.min(100, limit + 1));
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0,limit);
        } else if (days != null) {
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0,100);
            // 时间计算
            int dat = -Math.max(1, Math.min(999, days));
            LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);
            bps = bps.stream().filter(s->dayBefore.isBefore(s.getCreateTime())).toList();
        } else {
            int dat = -1;
            LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0,100);
            bps = bps.stream().filter(s->dayBefore.isBefore(s.getCreateTime())).toList();
        }
        var lines = new ArrayList<Image>(bps.size());
        try {
            var card = CardBuilder.getUserCard(infoMe);
            for (int i = 0; i < bps.size(); i++) {
                lines.add(new HCardBuilder(bps.get(i), i+1).build());
            }
            var panel = new TBPPanelBuilder(lines.size());
            panel.drawBanner(PanelUtil.getBanner(null)).mainCrawCard(card.build()).drawBp(lines);
            return panel.build(mode==OsuMode.DEFAULT ? infoMe.getPlayMode() : mode)
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
        var mode = OsuMode.getMode(playMode);
        long uid = osuGetService.getOsuId(userName);
        var userInfo = osuGetService.getPlayerInfo(uid, mode);
        if (bps != null) {
            bps = Math.min(99, bps-1);
            bps = Math.max(0, bps);
            var scores = osuGetService.getBestPerformance(uid, mode, bps,1);
            if (scores.size() == 0) throw new RuntimeException("bp不够");
            score = scores.get(0);
        } else if (bid != null) {
            try {
                score = osuGetService.getScore(bid, uid, mode).getScore();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("没打过");
            }
        } else if (Boolean.TRUE.equals(includeF)){
            var scores =osuGetService.getAllRecentN(uid, mode, 0,1);
            if (scores.size() == 0) throw new RuntimeException("最近没玩过");
            score = scores.get(0);
        } else {
            var scores = osuGetService.getRecentN(uid, mode, 0,1);
            if (scores.size() == 0) throw new RuntimeException("最近没玩过");
            score = scores.get(0);
        }

        return imageService.drawScore(userInfo, score, osuGetService);
    }
}
