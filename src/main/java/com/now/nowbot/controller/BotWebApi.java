package com.now.nowbot.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.now.nowbot.aop.OpenResource;
import com.now.nowbot.dao.OsuUserInfoDao;
import com.now.nowbot.model.LazerMod;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.BeatMap;
import com.now.nowbot.model.json.BeatmapDifficultyAttributes;
import com.now.nowbot.model.json.LazerScore;
import com.now.nowbot.model.json.OsuUser;
import com.now.nowbot.model.mappool.old.MapPoolDto;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchRating;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.messageServiceImpl.*;
import com.now.nowbot.service.osuApiService.*;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.serviceException.*;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static com.now.nowbot.service.messageServiceImpl.LoginService.LOGIN_USER_MAP;

@RestController
@ResponseBody
@CrossOrigin({"http://localhost:5173", "https://siyuyuko.github.io", "https://a.yasunaori.be"})
@RequestMapping(value = "/pub", method = RequestMethod.GET)
public class BotWebApi {
    private static final Logger log = LoggerFactory.getLogger(BotWebApi.class);
    @Resource
    OsuUserApiService       userApiService;
    @Resource
    OsuMatchApiService      matchApiService;
    @Resource
    OsuScoreApiService      scoreApiService;
    @Resource
    OsuBeatmapApiService    beatmapApiService;
    @Resource
    OsuCalculateApiService  calculateApiService;
    @Resource
    ImageService            imageService;
    @Resource
    OsuDiscussionApiService discussionApiService;
    @Resource
    OsuUserInfoDao          infoDao;

    /**
     * SN 图片接口 (SAN)
     * 私密，仅消防栓使用
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = "san")
    @OpenResource(name = "sn", desp = "查询玩家的 SAN")
    public ResponseEntity<byte[]> getSan(
            @OpenResource(name = "name", desp = "第一个玩家的名称", required = true) @RequestParam(value = "name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode) {

        var mode = OsuMode.getMode(playMode);

        OsuUser info;
        List<LazerScore> bplist;
        PPMinus ppm;

        try {
            info = userApiService.getPlayerInfo(name.trim(), mode);
            bplist = scoreApiService.getBestScores(info.getUserID(), mode, 0, 100);
            ppm = PPMinus.getInstance(mode, info, bplist);
        } catch (Exception e) {
            info = null;
            ppm = null;
        }

        var data = imageService.getPanelGamma(info, mode, ppm);
        return new ResponseEntity<>(data, getImageHeader(STR."\{name.trim()}-sn.jpg", data.length), HttpStatus.OK);
    }

    /**
     * PM 图片接口 (PPM)
     *
     * @param name     玩家名称
     * @param name2    第二个玩家名称，不为空会转到 PV 接口
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = "ppm")
    @OpenResource(name = "pm", desp = "查询玩家的 PPM")
    public ResponseEntity<byte[]> getPPM(
            @OpenResource(name = "name", desp = "第一个玩家的名称", required = true) @RequestParam(value = "name", required = false) String name,
            @OpenResource(name = "name2", desp = "第二个玩家的名称") @Nullable @RequestParam("name2") String name2,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @RequestParam(value = "u1", required = false) String u1) {

        if (Objects.nonNull(name2)) {
            return getPPMVS(name, name2, playMode);
        }

        if (Objects.isNull(name) && Objects.isNull(u1)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else if (Objects.isNull(name)) name = u1;

        var mode = OsuMode.getMode(playMode);

        var info = userApiService.getPlayerInfo(name.trim(), mode);
        var bplist = scoreApiService.getBestScores(info.getUserID(), mode, 0, 100);
        var ppm = PPMinus.getInstance(mode, info, bplist);
        if (ppm == null) {
            throw new RuntimeException("PPM：API 异常");
        } else {
            var data = imageService.getPanelB1(info, mode, ppm);
            return new ResponseEntity<>(data, getImageHeader(STR."\{name.trim()}-pm.jpg", data.length), HttpStatus.OK);
        }
    }

    /**
     * PV 图片接口 (PPMVS)
     *
     * @param name     玩家名称
     * @param name2    第二个玩家名称，必填
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = "ppm/vs")
    @OpenResource(name = "pv", desp = "比较玩家的 PPM")
    public ResponseEntity<byte[]> getPPMVS(
            @OpenResource(name = "name", desp = "第一个玩家的名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "name2", desp = "第二个玩家的名称", required = true) @RequestParam("name2") String name2,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode) {

        var mode = OsuMode.getMode(playMode);

        var user1 = userApiService.getPlayerInfo(name.trim(), mode);
        var user2 = userApiService.getPlayerInfo(name2.trim(), mode);

        mode = OsuMode.getMode(playMode, user1.getCurrentOsuMode());

        var bplist1 = scoreApiService.getBestScores(user1.getUserID(), mode, 0, 100);
        var bplist2 = scoreApiService.getBestScores(user2.getUserID(), mode, 0, 100);

        var ppm1 = PPMinus.getInstance(mode, user1, bplist1);
        var ppm2 = PPMinus.getInstance(mode, user2, bplist2);

        if (ppm1 == null) {
            throw new RuntimeException(PPMinusException.Type.PM_Me_FetchFailed.message); //"ppm 请求失败：ppmMe/Other 不存在"
        } else if (ppm2 == null) {
            throw new RuntimeException(PPMinusException.Type.PM_Player_FetchFailed.message);
        } else {
            var data = imageService.getPanelB1(user1, user2, ppm1, ppm2, mode);
            return new ResponseEntity<>(data, getImageHeader(STR."\{name.trim()} vs \{name2.trim()}-pv.jpg", data.length), HttpStatus.OK);
        }
    }

    /**
     * 比赛结果图片接口 (MN)
     *
     * @param matchID 比赛编号
     * @param k       跳过开头对局数量，跳过热手
     * @param d       忽略结尾对局数量，忽略 TB 表演赛
     * @param f       是否删除低于 1w 分的成绩，不传默认删除
     * @param r       是否保留重复对局，不传默认保留
     * @return image 比赛结果图片
     */
    @GetMapping(value = "match/now")
    @OpenResource(name = "mn", desp = "查询比赛结果")
    public ResponseEntity<byte[]> getMatchNow(
            @OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") int matchID,
            @OpenResource(name = "easy-multiplier", desp = "Easy 模组倍率") @Nullable Double e,
            @OpenResource(name = "skip", desp = "跳过开头") @Nullable Integer k,
            @OpenResource(name = "ignore", desp = "忽略结尾") @Nullable Integer d,
            @OpenResource(name = "delete-low", desp = "删除低分成绩") @Nullable Boolean f,
            @OpenResource(name = "keep-rematch", desp = "保留重复对局") @Nullable Boolean r) throws RuntimeException {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (e == null) e = 1d;
        if (f == null) f = true;
        if (r == null) r = true;
        byte[] image;

        Match match;

        try {
            match = matchApiService.getMatchInfo(matchID, 10);
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(GeneralTipsException.Type.G_Null_MatchID.getMessage());
        }

        try {
            var data = MatchNowService.calculate(
                    new MuRatingService.MuRatingPanelParam(match, new MatchRating.RatingParam(k, d, null, e, f, r), false),
                    beatmapApiService, calculateApiService);
            image = imageService.getPanel(data, "F");
        } catch (Exception err) {
            log.error("比赛结果：API 异常", err);
            throw new RuntimeException(MatchNowException.Type.MN_Render_Error.message);
        }

        return new ResponseEntity<>(image, getImageHeader(STR."\{matchID}-match.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 比赛评分图片接口 (RA)
     *
     * @param matchID 比赛编号
     * @param k       跳过开头对局数量，跳过热手
     * @param d       忽略结尾对局数量，忽略 TB 表演赛
     * @param f       是否删除低于 1w 分的成绩，不传默认删除
     * @param r       是否保留重复对局，不传默认保留
     * @return image 评分图片
     */
    @GetMapping(value = "match/rating")
    @OpenResource(name = "ra", desp = "查询比赛评分")
    public ResponseEntity<byte[]> getRating(
            @OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") int matchID,
            @OpenResource(name = "easy-multiplier", desp = "Easy 模组倍率") @Nullable Double e,
            @OpenResource(name = "skip", desp = "跳过开头") @Nullable Integer k,
            @OpenResource(name = "ignore", desp = "忽略结尾") @Nullable Integer d,
            @OpenResource(name = "delete-low", desp = "删除低分成绩") @Nullable Boolean f,
            @OpenResource(name = "keep-rematch", desp = "保留重复对局") @Nullable Boolean r
    ) {
        if (k == null) k = 0;
        if (d == null) d = 0;
        if (e == null) e = 1d;
        if (f == null) f = true;
        if (r == null) r = true;
        byte[] image;

        Match match;

        try {
            match = matchApiService.getMatchInfo(matchID, 10);
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(GeneralTipsException.Type.G_Null_MatchID.getMessage());
        }

        try {
            var c = MuRatingService.calculate(
                    new MuRatingService.MuRatingPanelParam(match, new MatchRating.RatingParam(k, d, null, e, f, r), false),
                    beatmapApiService, calculateApiService);
            image = imageService.getPanel(c, "C");
        } catch (Exception err) {
            log.error("比赛评分：API 异常", err);
            throw new RuntimeException(MRAException.Type.RATING_Send_MRAFailed.message);
        }

        return new ResponseEntity<>(image, getImageHeader(STR."\{matchID}-mra.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 生成图池接口 (GP)
     *
     * @param name    玩家名称
     * @param dataMap 需要传进来的图池请求体。结构是 {"HD": [114514, 1919810]} 组成的 Map
     * @return image 生成图池图片
     */
    @PostMapping(value = "match/getpool")
    public ResponseEntity<byte[]> getPool(
            @RequestParam("name") @Nullable String name,
            @RequestParam("mode") @Nullable String modeStr,
            @RequestBody Map<String, List<Long>> dataMap
    ) throws RuntimeException {
        var mapPool = new MapPoolDto(name, dataMap, beatmapApiService);
        if (mapPool.getModPools().isEmpty()) throw new RuntimeException(MapPoolException.Type.GP_Map_Empty.message);

        var mode = OsuMode.getMode(modeStr);

        var image = imageService.getPanelH(mapPool, mode);
        return new ResponseEntity<>(image, getImageHeader(STR."\{mapPool.getName()}-pool.jpg", image.length), HttpStatus.OK);
    }

    public enum scoreType {
        TodayBP,
        BP,
        Pass,
        Recent,
        PassCard,
        RecentCard,
    }

    /**
     * 多组成绩接口（当然单成绩也行，我把接口改了）
     *
     * @param name     玩家名称
     * @param playMode 模式,可为空
     * @param type     scoreType
     * @param start    !bp 45-55 或 !bp 45 里的 45
     * @param end      !bp 45-55 里的 55
     * @return image 成绩图片
     */
    public ResponseEntity<byte[]> getScore(
            @RequestParam("name") String name,
            @Nullable @RequestParam("mode") String playMode,
            @Nullable @RequestParam("type") scoreType type,
            @Nullable @RequestParam("start") Integer start,
            @Nullable @RequestParam("end") Integer end
    ) {
        var mode = OsuMode.getMode(playMode);
        name = name.trim();

        var osuUser = userApiService.getPlayerInfo(name, mode);
        List<LazerScore> scores;

        int offset = DataUtil.parseRange2Offset(start, end);
        int limit = DataUtil.parseRange2Limit(start, end);

        boolean isMultipleScore = (limit > 1);

        //渲染面板
        byte[] data;
        String suffix;

        switch (type) {
            // bp
            case BP -> {
                scores = scoreApiService.getBestScores(osuUser.getUserID(), mode, offset, limit);

                ArrayList<Integer> ranks = new ArrayList<>();
                for (int i = offset; i <= (offset + limit); i++) ranks.add(i + 1);

                if (isMultipleScore) {
                    calculateApiService.applyBeatMapChanges(scores);
                    calculateApiService.applyStarToScores(scores);
                    data = imageService.getPanelA4(osuUser, scores, ranks, "BS");
                    suffix = "-bps.jpg";
                } else {
                    try {
                        var e5Param = ScorePRService.getScore4PanelE5(osuUser, scores.getFirst(), "B", beatmapApiService, calculateApiService);
                        data = imageService.getPanel(e5Param.toMap(), "E5");
                    } catch (Exception e) {
                        throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩"));
                    }
                    suffix = "-bp.jpg";
                }
            }
            // pass
            case Pass -> {
                scores = scoreApiService.getPassedScore(osuUser.getUserID(), mode, offset, limit);

                calculateApiService.applyBeatMapChanges(scores);
                calculateApiService.applyStarToScores(scores);

                if (isMultipleScore) {
                    data = imageService.getPanelA5(osuUser, scores, "PS");
                    suffix = "-passes.jpg";
                } else {
                    try {
                        var e5Param = ScorePRService.getScore4PanelE5(osuUser, scores.getFirst(), "P", beatmapApiService, calculateApiService);
                        data = imageService.getPanel(e5Param.toMap(), "E5");
                    } catch (Exception e) {
                        throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "通过成绩"));
                    }
                    suffix = "-pass.jpg";
                }
            }

            //recent
            case Recent -> {
                scores = scoreApiService.getPassedScore(osuUser.getUserID(), mode, offset, limit);

                calculateApiService.applyBeatMapChanges(scores);
                calculateApiService.applyStarToScores(scores);

                if (isMultipleScore) {
                    data = imageService.getPanelA5(osuUser, scores, "RS");
                    suffix = "-recents.jpg";
                } else {
                    try {
                        var e5Param = ScorePRService.getScore4PanelE5(osuUser, scores.getFirst(), "R", beatmapApiService, calculateApiService);
                        data = imageService.getPanel(e5Param.toMap(), "E5");
                    } catch (Exception e) {
                        throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最近成绩"));
                    }
                    suffix = "-recent.jpg";
                }
            }

            //passCard
            case PassCard -> {
                scores = scoreApiService.getScore(osuUser.getUserID(), mode, offset, 1, true);
                var score = scores.getFirst();

                calculateApiService.applyBeatMapChanges(scores);
                calculateApiService.applyStarToScores(scores);

                data = imageService.getPanelGamma(score);
                suffix = "-pass_card.jpg";
            }

            //recentCard
            case RecentCard -> {
                scores = scoreApiService.getScore(osuUser.getUserID(), mode, offset, 1, false);
                var score = scores.getFirst();

                calculateApiService.applyBeatMapChanges(scores);
                calculateApiService.applyStarToScores(scores);

                data = imageService.getPanelGamma(score);
                suffix = "-recent_card.jpg";
            }

            // todaybp
            case null, default -> {
                // 时间计算
                var day = Math.max(Math.min(Optional.ofNullable(start).orElse(1), 999), 1);
                var bps = scoreApiService.getBestScores(osuUser.getUserID(), mode, 0, 100);
                ArrayList<Integer> ranks = new ArrayList<>();

                java.time.OffsetDateTime dayBefore = java.time.OffsetDateTime.now().minusDays(day);

                //scoreList = BPList.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();
                scores = new ArrayList<>();
                for (int i = 0; i < bps.size(); i++) {
                    var s = bps.get(i);
                    if (dayBefore.isBefore(s.getEndedTime())) {
                        scores.add(s);
                        ranks.add(i + 1);
                    }
                }

                calculateApiService.applyBeatMapChanges(scores);
                calculateApiService.applyStarToScores(scores);

                data = imageService.getPanelA4(osuUser, scores, ranks, "T");
                suffix = "-todaybp.jpg";
            }
        }

        return new ResponseEntity<>(data, getImageHeader(name + suffix, data.length), HttpStatus.OK);
    }

    /**
     * 今日最好成绩接口 (T)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param day      天数，不传默认一天内
     * @return image 今日最好成绩图片
     */
    @GetMapping(value = "bp/today")
    @OpenResource(name = "t", desp = "查询今日最好成绩")
    public ResponseEntity<byte[]> getTodayBP(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "day", desp = "天数") @Nullable @RequestParam("day") Integer day
    ) {
        return getScore(name, playMode, scoreType.TodayBP, day, null);
    }

    @GetMapping(value = "bp/scores")
    @OpenResource(name = "bs", desp = "查询多个最好成绩")
    public ResponseEntity<byte[]> getBPScores(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") Integer start,
            @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") Integer end
    ) {
        return getScore(name, playMode, scoreType.BP, start, end);
    }

    /**
     * 多个最近通过成绩接口 (PS) 不计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @param end      结束位置，不传默认等于开始位置
     * @return image 多个最近通过成绩图片
     */
    @GetMapping(value = "score/passes")
    @OpenResource(name = "ps", desp = "查询多个最近通过成绩")
    public ResponseEntity<byte[]> getPassedScores(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") Integer start,
            @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") Integer end
    ) {
        return getScore(name, playMode, scoreType.Pass, start, end);
    }

    /**
     * 多个最近成绩接口 (RS) 计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @param end      结束位置，不传默认等于开始位置
     * @return image 多个最近成绩图片
     */
    @GetMapping(value = "score/recents")
    @OpenResource(name = "rs", desp = "查询多个最近成绩")
    public ResponseEntity<byte[]> getRecentScores(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") Integer start,
            @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") Integer end
    ) {
        return getScore(name, playMode, scoreType.Recent, start, end);
    }

    /**
     * 最好成绩接口 (B)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最好成绩图片
     */
    @GetMapping(value = "bp")
    @OpenResource(name = "b", desp = "查询最好成绩")
    public ResponseEntity<byte[]> getBPScore(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") Integer start
    ) {
        return getScore(name, playMode, scoreType.BP, start, null);
    }

    /**
     * 最近通过成绩接口 (P) 不计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最近通过成绩图片
     */
    @GetMapping(value = "score/pass")
    @OpenResource(name = "p", desp = "查询最近通过成绩")
    public ResponseEntity<byte[]> getPassedScore(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") Integer start
    ) {
        return getScore(name, playMode, scoreType.Pass, start, null);
    }

    /**
     * 最近成绩接口 (R) 计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最近成绩图片
     */
    @GetMapping(value = "score/recent")
    @OpenResource(name = "r", desp = "查询最近成绩")
    public ResponseEntity<byte[]> getRecentScore(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") Integer start
    ) {
        return getScore(name, playMode, scoreType.Recent, start, null);
    }

    /**
     * 谱面成绩接口 (S)
     *
     * @param name     玩家名称
     * @param bid      谱面编号
     * @param playMode 模式，可为空
     * @param mods     模组字符串，可为空
     * @return image 谱面成绩图片
     */
    @GetMapping(value = "score")
    @OpenResource(name = "s", desp = "查询玩家谱面成绩")
    public ResponseEntity<byte[]> getBeatMapScore(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") String name,
            @OpenResource(name = "bid", desp = "谱面编号", required = true) @RequestParam("bid") Long bid,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode,
            @OpenResource(name = "mods", desp = "模组") @Nullable @RequestParam("mods") String mods
    ) {
        OsuUser osuUser;

        OsuMode mode = OsuMode.getMode(playMode);
        long uid;
        int modInt = 0;
        if (Objects.nonNull(mods)) modInt = OsuMod.getModsValue(mods);

        List<LazerScore> scoreList;
        LazerScore score = null;

        try {
            osuUser = userApiService.getPlayerInfo(name);
            uid = osuUser.getUserID();
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name));
        }

        if (Objects.isNull(mods)) {
            score = Objects.requireNonNull(scoreApiService.getBeatMapScore(bid, uid, mode)).score;
        } else {
            try {
                scoreList = scoreApiService.getBeatMapScores(bid, uid, mode);
                for (var s : scoreList) {
                    if (LazerMod.getModsValue(s.getMods()) == modInt) {
                        score = s;
                        break;
                    }
                }
            } catch (WebClientResponseException.NotFound e) {
                throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "成绩列表"));
            }
        }

        if (Objects.isNull(score)) {
            throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Null_Score, bid.toString()));
        }

        byte[] image;

        try {
            var e5Param = ScorePRService.getScore4PanelE5(osuUser, score, "S", beatmapApiService, calculateApiService);
            image = imageService.getPanel(e5Param.toMap(), "E5");
        } catch (Exception e) {
            throw new RuntimeException(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩列表"));
        }
        return new ResponseEntity<>(image, getImageHeader(STR."\{name}@\{bid}-score.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 最好成绩分析接口 (BA)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @return image 最好成绩分析图片
     */
    @GetMapping(value = "bp/analysis")
    @OpenResource(name = "ba", desp = "最好成绩分析")
    public ResponseEntity<byte[]> getBPAnalysis(
            @OpenResource(name = "name", desp = "玩家名称", required = true) @NonNull @RequestParam("name") String name,
            @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") String playMode
    ) throws RuntimeException {
        List<LazerScore> scores;
        OsuUser osuUser;

        try {
            var mode = OsuMode.getMode(playMode);
            name = name.trim();
            long uid = userApiService.getOsuId(name);
            osuUser = userApiService.getPlayerInfo(uid, mode);
            if (mode != OsuMode.DEFAULT) osuUser.setMode(mode.getName());
            scores = scoreApiService.getBestScores(uid, mode, 0, 100);
        } catch (Exception e) {
            throw new RuntimeException(GeneralTipsException.Type.G_Fetch_List.getMessage());
        }

        Map<String, Object> data;
        try {
            data = BPAnalysisService.parseData(osuUser, scores, userApiService, 2);
        } catch (Exception e) {
            throw new RuntimeException(GeneralTipsException.Type.G_Fetch_BeatMapAttr.getMessage());
        }

        var image = imageService.getPanel(data, "J2");
        return new ResponseEntity<>(image, getImageHeader(STR."\{name}-ba.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 扔骰子接口 (D)
     *
     * @param range   范围，支持 0 ~ 2147483647
     * @param compare 需要比较的文本，也可以把范围输入到这里来
     * @return String 扔骰子结果
     */
    @GetMapping(value = "dice")
    @OpenResource(name = "d", desp = "扔骰子")
    public ResponseEntity<byte[]> getDice(
            @OpenResource(name = "range", desp = "范围") @RequestParam("range") @Nullable Integer range,
            @OpenResource(name = "compare", desp = "需要比较的文本") @RequestParam("compare") @Nullable String compare
    ) throws RuntimeException {
        String message;

        try {
            if (Objects.isNull(range)) {
                if (Objects.isNull(compare)) {
                    message = String.format("%.0f", DiceService.getRandom(100));
                } else {
                    var isOnlyNumbers = Pattern.matches("^[0-9.]+$", compare);

                    if (isOnlyNumbers) {
                        try {
                            var r = DiceService.getRandom(Integer.parseInt(compare));

                            if (r <= 1) {
                                message = String.format("%.2f", r);
                            } else {
                                message = String.format("%.0f", r);
                            }
                        } catch (NumberFormatException e) {
                            message = DiceService.compare(compare);
                        }
                    } else {
                        message = DiceService.compare(compare);
                    }
                }
            } else {
                var r = DiceService.getRandom(range);

                if (r <= 1) {
                    message = String.format("%.2f", r);
                } else {
                    message = String.format("%.0f", r);
                }
            }

            return new ResponseEntity<>(message.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        } catch (DiceException e) {
            return new ResponseEntity<>(
                    String.format("%.0f", DiceService.getRandom(100)).getBytes(StandardCharsets.UTF_8)
                    , HttpStatus.OK);
        } catch (Exception e) {
            log.error("扔骰子：API 异常", e);
            throw new RuntimeException("扔骰子：API 异常");
        }
    }

    /***
     * 获取谱面信息 (M)
     * @param bid 谱面编号
     * @param modeStr 谱面模式
     * @param accuracy acc, 0-1的浮点数，或1-100，或101-10000
     * @param combo 最大连击
     * @param miss 失误数量
     * @param modStr 模组的字符串, 比如 HDHR 等
     * @return 谱面信息图片
     * @throws RuntimeException API 出错
     */
    @GetMapping(value = "map")
    @OpenResource(name = "m", desp = "获取谱面信息")
    public ResponseEntity<byte[]> getMapInfo(
            @OpenResource(name = "bid", desp = "谱面编号") @RequestParam("bid") Long bid,
            @OpenResource(name = "mode", desp = "游戏模式") @RequestParam("mode") @Nullable String modeStr,
            @OpenResource(name = "accuracy", desp = "准确率，允许输入 0-10000") @RequestParam("accuracy") @Nullable Double accuracy,
            @OpenResource(name = "combo", desp = "连击数") @RequestParam("combo") @Nullable Integer combo,
            @OpenResource(name = "miss", desp = "失误数") @RequestParam("miss") @Nullable Integer miss,
            @OpenResource(name = "mods", desp = "模组，允许按成对的双字母输入") @RequestParam("mods") @Nullable String modStr
    ) throws RuntimeException {
        if (Objects.isNull(accuracy)) accuracy = 1D;
        if (Objects.isNull(combo)) combo = 0;
        if (Objects.isNull(miss)) miss = 0;
        if (Objects.isNull(modStr)) modStr = "";

        try {
            var mode = OsuMode.getMode(modeStr, OsuMode.OSU);
            var beatMap = beatmapApiService.getBeatMap(bid);

            var expected = new MapStatisticsService.Expected(
                    mode,
                    accuracy,
                    combo,
                    miss,
                    OsuMod.splitModAcronyms(modStr),
                    false
            );

            var image = MapStatisticsService.getPanelE6Image(null, beatMap, expected, beatmapApiService, calculateApiService, imageService);

            return new ResponseEntity<>(image, getImageHeader(STR."\{bid}-mapinfo.jpg", image.length), HttpStatus.OK);
        } catch (Exception e) {
            log.error("谱面信息：API 异常", e);
            throw new RuntimeException("谱面信息：API 异常");
        }
    }

    /**
     * 获取玩家信息 (I)
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 游戏模式
     * @return 玩家信息图片
     */
    @GetMapping(value = "info")
    @OpenResource(name = "i", desp = "获取玩家信息")
    public ResponseEntity<byte[]> getPlayerInfo(
            @OpenResource(name = "uid", desp = "玩家编号") @RequestParam("uid") @Nullable Long uid,
            @OpenResource(name = "name", desp = "玩家名称") @RequestParam("name") @Nullable String name,
            @OpenResource(name = "mode", desp = "游戏模式") @RequestParam("mode") @Nullable String modeStr,
            @OpenResource(name = "day", desp = "回溯天数") @RequestParam("day") @Nullable Integer day
    ) {
        if (Objects.isNull(day)) day = 1;
        var user = getPlayerInfoJson(uid, name, modeStr);
        var mode = OsuMode.getMode(modeStr);
        if (OsuMode.isDefaultOrNull(mode)) mode = user.getCurrentOsuMode();

        var BPs = scoreApiService.getBestScores(user);
        //var recents = scoreApiService.getRecentIncludingFail(osuUser);
        var historyUser = infoDao.getLastFrom(user.getUserID(),
                        mode,
                        LocalDate.now().minusDays(day))
                .map(OsuUserInfoDao::fromArchive).orElse(null);

        var param = new InfoService.PanelDParam(user, historyUser, BPs, user.getCurrentOsuMode());

        var image = imageService.getPanel(param.toMap(), "D");

                /*
        var image = imageService.getPanelD(user, historyUser, BPs, user.getCurrentOsuMode());

                 */

        return new ResponseEntity<>(image, getImageHeader(STR."\{user.getUserID()}-info.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 获取谱师信息 (IM)
     *
     * @param uid  玩家编号
     * @param name 玩家名称
     * @return 谱师信息图片
     */
    @GetMapping(value = "info/mapper")
    @OpenResource(name = "im", desp = "获取谱师信息")
    public ResponseEntity<byte[]> getMapperInfo(
            @OpenResource(name = "uid", desp = "玩家编号") @RequestParam("uid") @Nullable Long uid,
            @OpenResource(name = "name", desp = "玩家名称") @RequestParam("name") @Nullable String name
    ) {
        var osuUser = getPlayerInfoJson(uid, name, null);
        var data = IMapperService.Companion.parseData(osuUser, userApiService, beatmapApiService);
        var image = imageService.getPanel(data, "M");

        return new ResponseEntity<>(image, getImageHeader(STR."\{osuUser.getUserID()}-mapper.jpg", image.length), HttpStatus.OK);
    }

    /**
     * 获取提名信息 (N)
     *
     * @param sid 谱面集编号
     * @param bid 谱面编号
     * @return 提名信息图片
     */

    @GetMapping(value = "map/nomination")
    @OpenResource(name = "n", desp = "获取提名信息")
    public ResponseEntity<byte[]> getNomination(
            @OpenResource(name = "sid", desp = "谱面集编号") @RequestParam("sid") @Nullable Integer sid,
            @OpenResource(name = "bid", desp = "谱面编号") @RequestParam("bid") @Nullable Integer bid
    ) throws RuntimeException {
        Map<String, Object> data;

        try {
            if (Objects.isNull(sid)) {
                if (Objects.isNull(bid)) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Map);
                } else {
                    data = NominationService.parseData(bid, false, beatmapApiService, discussionApiService, userApiService);
                }
            } else {
                data = NominationService.parseData(sid, true, beatmapApiService, discussionApiService, userApiService);
            }
        } catch (GeneralTipsException e) {
            throw new RuntimeException(e.getMessage());
        }

        try {
            byte[] image = imageService.getPanel(data, "N");

            return new ResponseEntity<>(image, getImageHeader(STR."\{Optional.ofNullable(sid).orElse(bid)}-nomination.jpg", image.length), HttpStatus.OK);
        } catch (Exception e) {
            log.error("提名信息：API 异常", e);
            throw new RuntimeException("提名信息：API 异常");
        }

    }

    /**
     * 获取比赛结果文件 (CA)
     *
     * @return 比赛结果文件
     */

    @GetMapping(value = "match/rating/csv")
    @OpenResource(name = "ca", desp = "获取比赛结果")
    public ResponseEntity<byte[]> getCsvRating(
            @OpenResource(name = "match", desp = "比赛编号（逗号分隔）") @RequestParam("match") @Nullable String matchIDs
    ) throws RuntimeException {
        try {
            var sb = new StringBuilder();
            var ids = CsvMatchService.Companion.parseDataString(matchIDs);

            if (ids == null) {
                throw new RuntimeException("请输入对局！");
            } else if (ids.size() == 1) {
                CsvMatchService.Companion.parseCRA(sb, ids.getFirst(), matchApiService, beatmapApiService, calculateApiService);
            } else {
                CsvMatchService.Companion.parseCRAs(sb, ids, matchApiService, beatmapApiService, calculateApiService);
            }

            var data = sb.toString().getBytes();

            return new ResponseEntity<>(data, getByteHeader(ids.getFirst() + "-csvrating.csv", data.length), HttpStatus.OK);
        } catch (Exception e) {
            log.error("比赛结果：API 异常", e);
            throw new RuntimeException("比赛结果：API 异常");
        }

    }


    //======================  以下是跨域调用 osu!API 的部分，可获取到各种原始 JSON  ===================================

    /**
     * 获取玩家信息的 OsuUser JSON，即原 uui
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 玩家模组
     * @return OsuUser JSON
     */
    @GetMapping(value = "info/json")
    public OsuUser getPlayerInfoJson(
            @RequestParam("uid") @Nullable Long uid,
            @RequestParam("name") @Nullable String name,
            @RequestParam("mode") @Nullable String modeStr
    ) {
        var mode = OsuMode.getMode(modeStr);
        if (Objects.nonNull(uid)) {
            return userApiService.getPlayerInfo(uid, mode);
        } else if (Objects.nonNull(name)) {
            return userApiService.getPlayerInfo(name, mode);
        } else {
            return userApiService.getPlayerInfo(17064371L, mode);
        }
    }

    /**
     * 获取谱面信息的 BeatMap JSON
     *
     * @param bid 谱面编号
     * @return BeatMap JSON
     */
    @GetMapping(value = "map/json")
    public BeatMap getBeatMapInfoJson(
            @RequestParam("bid") @Nullable Long bid
    ) {
        if (Objects.nonNull(bid)) {
            return beatmapApiService.getBeatMap(bid);
        } else {
            return new BeatMap();
        }
    }

    /**
     * 获取谱面的附加信息 Attr JSON
     *
     * @param bid  谱面编号
     * @param mods 模组字符串，通过逗号分隔
     * @param mode 游戏模式，默认为谱面自己的
     * @return BeatMap JSON
     */
    @GetMapping(value = "attr/json")
    public BeatmapDifficultyAttributes getDifficultyAttributes(
            @RequestParam("bid") @Nullable Long bid,
            @RequestParam("mods") @Nullable String mods,
            @RequestParam("mode") @Nullable String mode
    ) {
        if (Objects.nonNull(bid)) {
            return beatmapApiService.getAttributes(bid, OsuMode.getMode(mode), OsuMod.getModsValue(mods));
        } else {
            return new BeatmapDifficultyAttributes();
        }
    }

    /**
     * 获取 BP 信息的 JSON
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 玩家模组
     * @param start   !bp 45-55 或 !bp 45 里的 45
     * @param end     !bp 45-55 里的 55
     * @return OsuUser JSON
     */
    @GetMapping(value = "bp/json")
    public List<LazerScore> getBPJson(
            @RequestParam("uid") @Nullable Long uid,
            @RequestParam("name") @Nullable String name,
            @RequestParam("mode") @Nullable String modeStr,
            @RequestParam("start") @Nullable Integer start,
            @RequestParam("end") @Nullable Integer end
    ) {
        var mode = OsuMode.getMode(modeStr);

        int offset = DataUtil.parseRange2Offset(start, end);
        int limit = DataUtil.parseRange2Limit(start, end);

        if (Objects.nonNull(uid)) {
            return scoreApiService.getBestScores(uid, mode, offset, limit);
        } else if (Objects.nonNull(name)) {
            var user = userApiService.getPlayerInfo(name, mode);
            return scoreApiService.getBestScores(user, offset, limit);
        } else {
            return scoreApiService.getBestScores(7003013L, OsuMode.DEFAULT, offset, limit);
        }
    }

    /**
     * 获取成绩信息的 JSON
     *
     * @param uid      玩家编号
     * @param name     玩家名称
     * @param modeStr  游戏模式
     * @param start    !bp 45-55 或 !bp 45 里的 45
     * @param end      !bp 45-55 里的 55
     * @param isPass 是否通过，默认通过（真）
     * @return List<Score> JSON
     */
    @GetMapping(value = "score/json")
    public List<LazerScore> getScoreJson(
            @RequestParam("uid") @Nullable Long uid,
            @RequestParam("name") @Nullable String name,
            @RequestParam("mode") @Nullable String modeStr,
            @RequestParam("start") @Nullable Integer start,
            @RequestParam("end") @Nullable Integer end,
            @RequestParam("isPassed") @Nullable Boolean isPass
    ) {
        var mode = OsuMode.getMode(modeStr);
        if (Objects.isNull(isPass)) {
            isPass = true;
        }

        int offset = DataUtil.parseRange2Offset(start, end);
        int limit = DataUtil.parseRange2Limit(start, end);

        if (Objects.nonNull(uid)) {
            return scoreApiService.getScore(uid, mode, offset, limit, isPass);
        } else if (Objects.nonNull(name)) {
            var user = userApiService.getPlayerInfo(name, mode);
            return scoreApiService.getScore(user.getUserID(), mode, offset, limit, isPass);
        } else {
            return scoreApiService.getScore(7003013L, OsuMode.DEFAULT, offset, limit, isPass);
        }
    }

    /**
     * 登录, 向 bot 发送 !login 获得验证码, 验证码不区分大小写, 1分钟过期
     *
     * @param code 验证码
     */
    @GetMapping(value = "login")
    public OsuUser doLogin(@RequestParam("code") @NonNull String code) {
        var u = LOGIN_USER_MAP.getOrDefault(code.toUpperCase(), null);
        if (Objects.nonNull(u)) {
            LOGIN_USER_MAP.remove(code.toUpperCase());
            return userApiService.getPlayerInfo(u.uid());
        }
        throw new RuntimeException("已过期或者不存在");
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
    public String setLoggerLever(@RequestParam("l") String level, @RequestParam("package") @Nullable String package$) {
        var l = Level.toLevel(level, Level.INFO);
        if (Objects.isNull(package$)) {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("com.now.nowbot").setLevel(l);
        } else {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(package$).setLevel(l);
        }
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

    private static HttpHeaders getByteHeader(String name, long length) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(name).build());
        headers.setContentLength(length);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return headers;
    }

    @Resource
    Over6KUserService over6KUserService;

    @GetMapping("alumni")
    public Object getAlumni(@RequestParam(name = "start", defaultValue = "0") int start,
                            @RequestParam(name = "size", defaultValue = "30") int size) {
        try {
            return over6KUserService.getResultJson(start, size);
        } catch (IOException e) {
            log.error("alumni 文件异常", e);
            return "[]";
        }
    }
}

