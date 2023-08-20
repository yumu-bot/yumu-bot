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
import com.now.nowbot.service.MessageServiceImpl.BphtService;
import com.now.nowbot.service.MessageServiceImpl.MRAService;
import com.now.nowbot.service.MessageServiceImpl.MonitorNowService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    BphtService   bphtService;
    @Resource
    MRAService    mraService;
    @Resource
    MonitorNowService monitorNowService;
    @Resource
    ImageService  imageService;

    /**
     * 如果包含 u2 则响应为 ppmvs
     *
     * @return
     */

    @GetMapping(value = "ppm")
    public ResponseEntity<byte[]> getPPM(@RequestParam("u1") String user1, @Nullable @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
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
            var data = imageService.getPanelB(info, mode, ppm);
            return new ResponseEntity<>(data, getImageHeader(user1.trim() + "-ppm.jpg", data.length), HttpStatus.OK);
        }
    }

    @GetMapping(value = "ppmvs")
    public ResponseEntity<byte[]> getPPMVS(@RequestParam("u1") String user1, @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
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
            var data = imageService.getPanelB(info1, info2, ppm1, ppm2, mode);
            return new ResponseEntity<>(data, getImageHeader(user1.trim() + " vs " + user2.trim() + "-ppm.jpg", data.length), HttpStatus.OK);
        }
    }

    /***
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @param r include rematch
     * @return img
     */
    @GetMapping(value = "match")
    public ResponseEntity<byte[]> getMatch(@RequestParam("id") int mid, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f, @Nullable Boolean r) {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (f == null) f = true;
        if (r == null) r = true;
        var data = monitorNowService.getImage(mid, k, d, f, r);
        return new ResponseEntity<>(data, getImageHeader(mid + "-match.jpg", data.length), HttpStatus.OK);
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
    @GetMapping(value = "rating")
    public ResponseEntity<byte[]> getRa(@RequestParam("id") int matchId, @Nullable Integer k, @Nullable Integer d, @Nullable Boolean f, @Nullable Boolean r) {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (f == null) f = true;
        if (r == null) r = true;

        var data = mraService.getDataImage(matchId, k, d, f, r);
        return new ResponseEntity<>(data, getImageHeader(matchId + "-mra.jpg", data.length), HttpStatus.OK);
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

    /**
     * 多组成绩接口
     *
     * @param userName 用户
     * @param playMode 模式,可为空
     * @param type     不传 或 0: 近N天的bp,此时 value 参数为天数,且范围是0-999
     *                 1: 前N个bp
     *                 2: 最近N次游玩,不包含fail
     *                 3: 最近N次游玩,包含fail
     * @param value    不传默认为 1,具体含义取决于 type,范围在 1-100 之间
     * @return
     */
    @GetMapping(value = "scores")
    public ResponseEntity<byte[]> getPR(@RequestParam("u1") String userName,
                                        @Nullable @RequestParam("mode") String playMode,
                                        @Nullable @RequestParam("type") Integer type,
                                        @Nullable @RequestParam("value") Integer value
    ) {
        var mode = OsuMode.getMode(playMode);
        userName = userName.trim();
        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(userName);
        List<Score> bps;
        if (value == null) value = 1;
        value = Math.max(1, value);
        if (type == null || type == 0) {
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0, 100);
            // 时间计算
            int dat = -Math.min(999, value);
            LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);
            bps = bps.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();
        } else if (type == 1) {
            if (value > 100) value = 100;
            bps = osuGetService.getBestPerformance(infoMe.getId(), mode, 0, value);
        } else if (type == 2) {
            if (value > 100) value = 100;
            bps = osuGetService.getRecentN(infoMe.getId(), mode, 0, value);
        } else if (type == 3) {
            if (value > 100) value = 100;
            bps = osuGetService.getAllRecentN(infoMe.getId(), mode, 0, value);
        } else {
            throw new RuntimeException("type 参数错误");
        }
        var lines = new ArrayList<Image>(bps.size());
        try {
            var card = CardBuilder.getUserCard(infoMe);
            for (int i = 0; i < bps.size(); i++) {
                lines.add(new HCardBuilder(bps.get(i), i + 1).build());
            }
            var panel = new TBPPanelBuilder(lines.size());
            panel.drawBanner(PanelUtil.getBanner(null)).mainCrawCard(card.build()).drawBp(lines);
            var data = panel.build(mode == OsuMode.DEFAULT ? infoMe.getPlayMode() : mode)
                    .encodeToData(EncodedImageFormat.JPEG, 80)
                    .getBytes();
            return new ResponseEntity<>(data, getImageHeader(userName + "-bp.jpg", data.length), HttpStatus.OK);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 单个成绩接口
     *
     * @param userName 用户
     * @param playMode 模式,可为空
     * @param type     请求类型,使用整数; 不传或者 0: 查询pr; 1: 查询bp; 2:查询谱面成绩;
     * @param value    查询pr: 前第N个成绩, 不传默认为最近一次,从 0 开始
     *                 查询bp: bp,从0开始, 不传默认为bp1(value == 0)
     *                 查询谱面成绩: 谱面id, 不传报错
     * @param param    查询pr: 0为不包含失败,1为包含失败,不传默认为0
     *                 查询bp: 无需此值
     *                 查询谱面成绩: 指定 mod_int, 不传默认谱面最高成绩
     */
    @GetMapping(value = "score")
    public ResponseEntity<byte[]> getScore(@RequestParam("u1") String userName,
                                           @Nullable @RequestParam("mode") String playMode,
                                           @Nullable @RequestParam("type") Integer type,
                                           @Nullable @RequestParam("value") Integer value,
                                           @Nullable @RequestParam("param") Integer param
                                           ) {
        Score score;
        userName = userName.trim();
        var mode = OsuMode.getMode(playMode);
        long uid = osuGetService.getOsuId(userName);
        var userInfo = osuGetService.getPlayerInfo(uid, mode);
        if (type == null || type == 0) {
            if (value == null) value = 0;
            value = Math.min(99, Math.max(0, value));
            List<Score> scores;
            if (param == null || param == 0) {
                scores = osuGetService.getRecentN(uid, mode, value, 1);
            } else {
                scores = osuGetService.getAllRecentN(uid, mode, value, 1);
            }
            if (scores.size() == 0) throw new RuntimeException("最近没玩过");
            score = scores.get(0);

        } else if (type == 1) {
            if (value == null) value = 0;
            value = Math.min(99, value - 1);
            value = Math.max(0, value);
            var scores = osuGetService.getBestPerformance(uid, mode, value, 1);
            if (scores.size() == 0) throw new RuntimeException("bp不够");
            score = scores.get(0);
        } else if (type == 2) {
            if (value == null) throw new RuntimeException("value 参数错误");
            try {
                score = osuGetService.getScore(value, uid, mode).getScore();
            } catch (Exception e) {
                throw new RuntimeException("没打过");
            }
        } else {
            throw new RuntimeException("type 参数错误");
        }

        var data = imageService.getPanelE(userInfo, score, osuGetService);
        return new ResponseEntity<>(data, getImageHeader(userName + "-bp.jpg", data.length), HttpStatus.OK);
    }

    @GetMapping(value = "bpa")
    public ResponseEntity<byte[]> getBpa(@RequestParam("u1") String userName,
                                         @Nullable @RequestParam("mode") String playMode
    ) {
        userName = userName.trim();
        var mode = OsuMode.getMode(playMode);
        long uid = osuGetService.getOsuId(userName);
        var userInfo = osuGetService.getPlayerInfo(uid, mode);
        if (mode != OsuMode.DEFAULT) userInfo.setPlayMode(mode.getName());
        var scores = osuGetService.getBestPerformance(uid, mode, 0, 100);
        var data = imageService.getPanelJ(userInfo, scores, osuGetService);
        return new ResponseEntity<>(data, getImageHeader(userName + "-bp.jpg", data.length), HttpStatus.OK);
    }


    @GetMapping("file/{key}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("key") String key) throws IOException {
        var data = QQMsgUtil.getFileData(key);
        if (data == null) throw new RuntimeException("文件不存在");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(data.name()).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(data.bytes().capacity());
        return new ResponseEntity<>(data.bytes().array(), headers, HttpStatus.OK);
    }

    @GetMapping("log-level")
    public String setLoggerLever(@RequestParam("l") String level) {
        var l = Level.toLevel(level, Level.INFO);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("com.now.nowbot").setLevel(l);
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

    HttpHeaders getImageHeader(String name, long length) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(name).build());
        headers.setContentLength(length);
        headers.setContentType(MediaType.IMAGE_JPEG);
        return headers;
    }
}

