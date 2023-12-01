package com.now.nowbot.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.now.nowbot.aop.OpenResource;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageServiceImpl.BPAnalysisService;
import com.now.nowbot.service.MessageServiceImpl.MonitorNowService;
import com.now.nowbot.service.MessageServiceImpl.MuRatingService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.throwable.ServiceException.MonitorNowException;
import com.now.nowbot.throwable.ServiceException.PPMinusException;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/pub", method = RequestMethod.GET)
@CrossOrigin("http://localhost:5173")
public class BotWebApi {
    private static final Logger log = LoggerFactory.getLogger(BotWebApi.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    MuRatingService mraService;
    @Resource
    MonitorNowService monitorNowService;
    @Resource
    ImageService imageService;
    @Resource
    BPAnalysisService bpAnalysisService;


    /**
     * 如果包含 u2 则响应为 ppmvs
     *
     * @return image
     */

    @GetMapping(value = "ppm")
    @OpenResource(name = "ppm", desp = "查询玩家的 PP- !ymppminus (!ppm)")
    public ResponseEntity<byte[]> getPPM(@OpenResource(name = "user1", desp = "第一个用户的用户名", required = true) @RequestParam("u1") String user1,
                                         @OpenResource(name = "user2", desp = "第二个用户的用户名") @Nullable @RequestParam("u2") String user2,
                                         @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode) {
        if (user2 != null) {
            return getPPMVS(user1, user2, playMode);
        }
        var mode = OsuMode.getMode(playMode);
        var info = userApiService.getPlayerInfo(user1.trim(), mode);
        var bplist = scoreApiService.getBestPerformance(info.getUID(), mode, 0, 100);
        var ppm = PPMinus.getInstance(mode, info, bplist);
        if (ppm == null) {
            throw new RuntimeException("ppm 请求失败：ppmMe 不存在");
        } else {
            var data = imageService.getPanelB1(info, mode, ppm);
            return new ResponseEntity<>(data, getImageHeader(user1.trim() + "-ppm.jpg", data.length), HttpStatus.OK);
        }
    }

    @GetMapping(value = "ppmvs")
    public ResponseEntity<byte[]> getPPMVS(@RequestParam("u1") String user1, @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        var mode = OsuMode.getMode(playMode);
        var info1 = userApiService.getPlayerInfo(user1.trim(), mode);
        var info2 = userApiService.getPlayerInfo(user2.trim(), mode);
        if (OsuMode.isDefault(mode)) mode = info1.getPlayMode();
        var bplist1 = scoreApiService.getBestPerformance(info1.getUID(), mode, 0, 100);
        var bplist2 = scoreApiService.getBestPerformance(info2.getUID(), mode, 0, 100);
        var ppm1 = PPMinus.getInstance(mode, info1, bplist1);
        var ppm2 = PPMinus.getInstance(mode, info2, bplist2);
        if (ppm1 == null) {
            throw new RuntimeException(PPMinusException.Type.PPM_Me_FetchFailed.message); //"ppm 请求失败：ppmMe/Other 不存在"
        } else if (ppm2 == null) {
            throw new RuntimeException(PPMinusException.Type.PPM_Player_FetchFailed.message);
        } else {
            var data = imageService.getPanelB1(info1, info2, ppm1, ppm2, mode);
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
    @OpenResource(name = "mn", desp = "查看比赛房间信息 !ymmonitornow (!mn)")
    public ResponseEntity<byte[]> getMatchNow(@OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") int mid,
                                              @OpenResource(name = "skip", desp = "跳过开头") @Nullable Integer k,
                                              @OpenResource(name = "skip-end", desp = "忽略结尾") @Nullable Integer d,
                                              @OpenResource(name = "keep-low", desp = "保留低分成绩") @Nullable Boolean f,
                                              @OpenResource(name = "ignore-repeat", desp = "忽略重复对局") @Nullable Boolean r) throws MonitorNowException {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (f == null) f = true;
        if (r == null) r = true;
        var data = monitorNowService.getImage(mid, k, d, f, r);
        return new ResponseEntity<>(data, getImageHeader(mid + "-match.jpg", data.length), HttpStatus.OK);
    }

    /***
     *
     * @param matchId match id
     * @param k skip round
     * @param d delete end
     * @param f include failed
     * @param r include rematch
     * @return img
     */
    @GetMapping(value = "rating")
    @OpenResource(name = "ra", desp = "查看比赛评价 !ymrating (!ra)")
    public ResponseEntity<byte[]> getRating(
            @OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") int matchId,
            @OpenResource(name = "skip", desp = "跳过开头") @Nullable Integer k,
            @OpenResource(name = "skip-end", desp = "忽略结尾") @Nullable Integer d,
            @OpenResource(name = "keep-failed", desp = "保留低分成绩") @Nullable Boolean f,
            @OpenResource(name = "remove-repeat", desp = "忽略重复对局") @Nullable Boolean r
    ) throws MRAException {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (f == null) f = true;
        if (r == null) r = true;

        byte[] data = imageService.getPanelC(mraService.calculate(matchId, k, d, f, r));
        return new ResponseEntity<>(data, getImageHeader(matchId + "-mra.jpg", data.length), HttpStatus.OK);
    }

    /**
     * 多组成绩接口（当然单成绩也行，我把接口改了）
     *
     * @param userName 用户
     * @param playMode 模式,可为空
     * @param type     0，null: todaybp，此时只需要输入 value1，介于 1-999
     *                 1: bp，
     *                 2: pass，
     *                 3: recent，
     * @param value1   不传默认为 1,具体含义取决于 type,范围在 1-100 之间
     * @param value2   可以不传，具体含义取决于 type,范围在 1-100 之间
     * @return image
     */
    public ResponseEntity<byte[]> getScore(@RequestParam("u1") String userName,
                                           @Nullable @RequestParam("mode") String playMode,
                                           @Nullable @RequestParam("type") Integer type,
                                           @Nullable @RequestParam("value1") Integer value1,
                                           @Nullable @RequestParam("value2") Integer value2
    ) {
        var mode = OsuMode.getMode(playMode);
        userName = userName.trim();

        var osuUser = userApiService.getPlayerInfo(userName, mode);
        List<Score> scoreList;

        int offset;
        int limit;
        boolean isMultipleScore;

        if (Objects.isNull(value1) || value1 < 1 || value1 > 100) value1 = 1;

        if (Objects.isNull(value2) || value2 < 1 || value2 > 100) {
            offset = value1 - 1;
            limit = 1;
        } else {
            //分流：正常，相等，相反
            if (value2 > value1) {
                offset = value1 - 1;
                limit = value2 - value1 + 1;
            } else if (Objects.equals(value1, value2)) {
                offset = value1 - 1;
                limit = 1;
            } else {
                offset = value2 - 1;
                limit = value1 - value2 + 1;
            }
        }

        isMultipleScore = (limit > 1);

        //渲染面板
        byte[] data;
        String suffix;

        switch (type) {
            // bp
            case 1 -> {
                scoreList = scoreApiService.getBestPerformance(osuUser.getUID(), mode, offset, limit);

                ArrayList<Integer> rankList = new ArrayList<>();
                for (int i = offset; i <= (offset + limit); i++) rankList.add(i + 1);

                if (isMultipleScore) {
                    data = imageService.getPanelA4(osuUser, scoreList, rankList);
                    suffix = "-bps.jpg";
                } else {
                    data = imageService.getPanelE(osuUser, scoreList.get(0), beatmapApiService);
                    suffix = "-bp.jpg";
                }
            }
            // pass
            case 2 -> {
                scoreList = scoreApiService.getRecent(osuUser.getUID(), mode, offset, limit);

                if (isMultipleScore) {
                    data = imageService.getPanelA5(osuUser, scoreList);
                    suffix = "-passes.jpg";
                } else {
                    data = imageService.getPanelE(osuUser, scoreList.get(0), beatmapApiService);
                    suffix = "-pass.jpg";
                }
            }

            //recent
            case 3 -> {
                scoreList = scoreApiService.getRecent(osuUser.getUID(), mode, offset, limit);

                if (isMultipleScore) {
                    data = imageService.getPanelA5(osuUser, scoreList);
                    suffix = "-recents.jpg";
                } else {
                    data = imageService.getPanelE(osuUser, scoreList.get(0), beatmapApiService);
                    suffix = "-recent.jpg";
                }
            }

            // todaybp
            case null, default -> {
                // 时间计算
                var BPList = scoreApiService.getBestPerformance(osuUser.getUID(), mode, 0, 100);
                ArrayList<Integer> rankList = new ArrayList<>();

                LocalDateTime dayBefore = LocalDateTime.now().minusDays(value1);

                //scoreList = BPList.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();
                scoreList = new ArrayList<>();
                for (int i = 0; i < BPList.size(); i++) {
                    var s = BPList.get(i);
                    if (dayBefore.isBefore(s.getCreateTime())) {
                        scoreList.add(s);
                        rankList.add(i + 1);
                    }
                }

                data = imageService.getPanelA4(osuUser, scoreList, rankList);
                suffix = "-tbp.jpg";
            }
        }

        return new ResponseEntity<>(data, getImageHeader(userName + suffix, data.length), HttpStatus.OK);
    }

    @GetMapping(value = "scores/bp-days")
    @OpenResource(name = "ppm", desp = "查询今日最好成绩 !ymtodaybp (!t)")
    public ResponseEntity<byte[]> getTodayBP(
            @OpenResource(name = "username", desp = "玩家名称", required = true) @RequestParam("u1") String userName,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "days", desp = "天数") @Nullable @RequestParam("n") Integer value
    ) {
        if (value == null) value = 1;
        if (value <= 0) value = 1;
        else if (value > 999) value = 999;
        return getScore(userName, playMode, 0, value, null);
    }

    @GetMapping(value = "scores/bp-range")
    public ResponseEntity<byte[]> getBPScores(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value1,
            @Nullable @RequestParam("m") Integer value2
    ) {
        return getScore(userName, playMode, 1, value1, value2);
    }

    /**
     * 不计入 fail 成绩
     */
    @GetMapping(value = "scores/pr")
    public ResponseEntity<byte[]> getPassedScores(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value1,
            @Nullable @RequestParam("m") Integer value2
    ) {
        return getScore(userName, playMode, 2, value1, value2);
    }

    /**
     * 计入 fail 成绩
     */
    @GetMapping(value = "scores/re")
    public ResponseEntity<byte[]> getRecentScores(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value1,
            @Nullable @RequestParam("m") Integer value2
    ) {
        return getScore(userName, playMode, 3, value1, value2);
    }

    /**
     * n 从0开始, 不传默认为0
     */
    @GetMapping(value = "score/bp")
    public ResponseEntity<byte[]> getBPScore(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value
    ) {
        if (value == null) value = 0;
        return getScore(userName, playMode, 1, value, null);
    }

    /**
     * n 从0开始, 不传默认为0
     */
    @GetMapping(value = "score/pr")
    public ResponseEntity<byte[]> getPassedScore(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value
    ) {
        return getScore(userName, playMode, 2, value, null);
    }

    /**
     * n 从0开始, 不传默认为0
     */
    @GetMapping(value = "score/re")
    public ResponseEntity<byte[]> getRecentScore(
            @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("n") Integer value
    ) {
        return getScore(userName, playMode, 3, value, null);
    }

    @GetMapping(value = "score")
    public ResponseEntity<byte[]> getBeatMapScore(
            @NotNull @RequestParam("u1") String userName,
            @Nullable @RequestParam("mode") String playMode,
            @NotNull @RequestParam("bid") Integer value,
            @Nullable @RequestParam("mods") String mods
    ) {
        OsuUser osuUser;

        OsuMode mode = OsuMode.getMode(playMode);
        long UID;
        int modInt = 0;
        if (mods != null) modInt = Mod.getModsValue(mods);

        List<Score> scoreList;
        Score score = null;

        try {
            osuUser = userApiService.getPlayerInfo(userName);
            UID = osuUser.getUID();
        } catch (Exception e) {
            throw new RuntimeException(ScoreException.Type.SCORE_Player_NotFound.message);
        }

        try {
            scoreList = scoreApiService.getScoreAll(value, UID, mode);
        } catch (Exception e) {
            throw new RuntimeException(ScoreException.Type.SCORE_Score_FetchFailed.message);
        }

        for (var s : scoreList) {
            if ((s.getMods() == null || s.getMods().isEmpty()) && Mod.None.check(modInt)) {
                score = s;
                break;
            } else if (Mod.getModsValueFromStr(s.getMods()) == modInt) {
                score = s;
                break;
            }
        }

        if (score == null) {
            throw new RuntimeException(ScoreException.Type.SCORE_Mod_NotFound.message);
        } else {
            var beatMap = new BeatMap();
            beatMap.setBID(Long.valueOf(value));
            score.setBeatMap(beatMap);
        }

        var data = imageService.getPanelE(osuUser, score, beatmapApiService);
        return new ResponseEntity<>(data, getImageHeader(userName + "-score.jpg", data.length), HttpStatus.OK);
    }

    @GetMapping(value = "bpa")
    public ResponseEntity<byte[]> getBPAnalysis(@RequestParam("u1") String userName,
                                                @Nullable @RequestParam("mode") String playMode
    ) {
        userName = userName.trim();
        var mode = OsuMode.getMode(playMode);
        long uid = userApiService.getOsuId(userName);
        var osuUser = userApiService.getPlayerInfo(uid, mode);
        if (mode != OsuMode.DEFAULT) osuUser.setPlayMode(mode.getName());
        var scores = scoreApiService.getBestPerformance(uid, mode, 0, 100);

        var d = bpAnalysisService.parseData(osuUser, scores, userApiService);
        var data = imageService.getPanelJ(d);
        return new ResponseEntity<>(data, getImageHeader(userName + "-bp.jpg", data.length), HttpStatus.OK);
    }

    /**
     * 用于使用 go-cqhttp 发送文件的下载接口
     *
     * @param key file key
     * @return file
     */
    @GetMapping("file/{key}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("key") String key) {
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

    private static HttpHeaders getImageHeader(String name, long length) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(name).build());
        headers.setContentLength(length);
        headers.setContentType(MediaType.IMAGE_JPEG);
        return headers;
    }
}

