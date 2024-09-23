package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.DataUtil;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import rosu.Rosu;
import rosu.parameter.JniScore;
import rosu.result.JniResult;
import rosu.result.ManiaResult;
import rosu.result.OsuResult;
import rosu.result.TaikoResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public interface OsuBeatmapApiService {
    /**
     * 下载beatmap(.osu)文件
     *
     * @param bid 谱面id
     * @return osu文件字符串
     */
    String getBeatMapFileStr(long bid) throws Exception;

    boolean hasBeatMapFile(long bid);

    boolean refreshBeatMapFile(long bid) throws IOException;

    String downloadBeatMapFileStrForce(long bid);

    /**
     * 查一下文件是否跟 checksum 是否对起来
     *
     * @return 是否对得上
     */
    boolean checkBeatMap(BeatMap beatMap) throws IOException;

    boolean checkBeatMap(long bid, String checkStr) throws IOException;

    // 尽量用 FromDataBase，这样可以节省 API 开支
    BeatMap getBeatMap(long bid);

    default BeatMap getBeatMap(int bid) {
        return getBeatMap((long) bid);
    }

    BeatMapSet getBeatMapSet(long sid);

    default BeatMapSet getBeatMapSet(int sid) {
        return getBeatMapSet((long) sid);
    }

    default BeatMap getBeatMapFromDataBase(int bid) {
        return getBeatMapFromDataBase((long) bid);
    }

    BeatMap getBeatMapFromDataBase(long bid);

    boolean isNewbieMap(long bid);

    int[] getBeatmapObjectGrouping26(BeatMap map) throws Exception;

    int getFailTime(long bid, int passObj);

    double getPlayPercentage(Score score);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue);

    default BeatmapDifficultyAttributes getAttributes(Long id) {
        return getAttributes(id, OsuMode.DEFAULT);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, int modsValue) {
        return getAttributes(id, OsuMode.DEFAULT, modsValue);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, OsuMod... osuMods) {
        int value = Arrays.stream(osuMods).mapToInt(m -> m.value).reduce(0, Integer::sum);
        return getAttributes(id, value);
    }

    /**
     * 应该单独写一个返回类型
     */
    JsonNode lookupBeatmap(String checksum, String filename, Long id);

    BeatMapSetSearch searchBeatMapSet(Map<String, Object> query);

    default JniResult getMaxPP(long bid, int modInt) throws Exception {
        var b = getBeatMapFileStr(bid).getBytes(StandardCharsets.UTF_8);
        JniScore js = new JniScore();
        js.setAccuracy(100);
        js.setMods(modInt);
        return Rosu.calculate(b, js);
    }

    default JniResult getMaxPP(Score s) throws Exception {
        return getMaxPP(s.getBeatMap().getBeatMapID(), s.getMode(), OsuMod.getModsValueFromAbbrList(s.getMods()));
    }

    default JniResult getMaxPP(long bid, OsuMode mode, int modInt) throws Exception {
        var b = getBeatMapFileStr(bid).getBytes(StandardCharsets.UTF_8);
        JniScore js = new JniScore();

        js.setMode(mode.toRosuMode());
        js.setAccuracy(100D);
        js.setMisses(0);
        js.setMods(modInt);
        js.setSpeed(OsuMod.getModsClockRate(modInt));

        return Rosu.calculate(b, js);
    }

    default JniResult getPP(long bid, int modInt, Statistics s, int combo) throws Exception {
        var b = getBeatMapFileStr(bid).getBytes(StandardCharsets.UTF_8);
        JniScore js = new JniScore();
        js.setCombo(combo);
        return getJniResult(modInt, s, b, js);
    }

    default JniResult getPP(long bid, OsuMode mode, int modInt, Statistics s, int combo) throws Exception {
        var b = getBeatMapFileStr(bid).getBytes(StandardCharsets.UTF_8);
        JniScore js = new JniScore();
        js.setCombo(combo);
        js.setMode(mode.toRosuMode());
        return getJniResult(modInt, s, b, js);
    }

    default JniResult getPP(BeatMap beatMap, MapStatisticsService.Expected e) throws Exception {
        var b = getBeatMapFileStr(beatMap.getBeatMapID()).getBytes(StandardCharsets.UTF_8);
        var m = OsuMod.getModsValueFromAbbrList(e.mods);

        JniScore score = new JniScore();
        score.setCombo(e.combo);
        score.setMode(e.mode.toRosuMode());
        score.setMisses(e.misses);
        score.setAccuracy(e.accuracy);
        score.setMods(m);

        return Rosu.calculate(b, score);
    }

    default JniResult getPP(Score s) throws Exception {
        var b = getBeatMapFileStr(s.getBeatMap().getBeatMapID()).getBytes(StandardCharsets.UTF_8);
        var m = OsuMod.getModsValueFromAbbrList(s.getMods());
        var t = s.getStatistics();

        JniScore js = new JniScore();
        js.setCombo(s.getMaxCombo());
        js.setMode(s.getMode().toRosuMode());
        return getJniResult(m, t, b, js);
    }

    default JniResult getFcPP(Score s) throws Exception {
        var b = getBeatMapFileStr(s.getBeatMap().getBeatMapID()).getBytes(StandardCharsets.UTF_8);
        var m = OsuMod.getModsValueFromAbbrList(s.getMods());
        var t = s.getStatistics().clone();

        if (t.getCountAll(s.getMode()) > 0 && t.getCountMiss() > 0) {
            t.setCountMiss(0);
            t.setCount300(t.getCount300() + t.getCountMiss());
        }

        if (s.getBeatMap() == null || s.getBeatMap().getMaxCombo() == null) {
            applyBeatMapExtend(s);
        }

        //s.setMaxCombo(s.getBeatMap().getMaxCombo());

        JniScore js = new JniScore();
        js.setMode(s.getMode().toRosuMode());
        return getJniResult(m, t, b, js);
    }

    default Map<String, Object> getFullStatistics(Score s) throws Exception {
        var jniResult = getMaxPP(s);

        return getStats(jniResult);
    }

    default Map<String, Object> getStatistics(Score s) throws Exception {
        var jniResult = getPP(s);

        return getStats(jniResult);
    }

    @NonNull
    private Map<String, Object> getStats(@NonNull JniResult jniResult) {
        var result = new HashMap<String, Object>(6);
        switch (jniResult) {
            case OsuResult o -> {
                result.put("aim_pp", o.getPpAim());
                result.put("spd_pp", o.getPpSpeed());
                result.put("acc_pp", o.getPpAcc());
                result.put("fl_pp", o.getPpFlashlight());
            }
            case TaikoResult t -> {
                result.put("acc_pp", t.getPpAcc());
                result.put("diff_pp", t.getPpDifficulty());
            }
            case ManiaResult m -> result.put("diff_pp", m.getPpDifficulty());
            default -> {
                return result;
            }
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("all")
    private JniResult getJniResult(int modInt, Statistics s, byte[] b, JniScore score) {
        score.setMods(modInt);
        // 这个要留着, 因为是调用了 native 方法
        // 那边如果有 null 会直接导致虚拟机炸掉退出, 注解不会在运行时检查是不是 null
        if (
                Objects.nonNull(s.getCountGeki()) &&
                        Objects.nonNull(s.getCountKatu()) &&
                        Objects.nonNull(s.getCount300()) &&
                        Objects.nonNull(s.getCount100()) &&
                        Objects.nonNull(s.getCount50()) &&
                        Objects.nonNull(s.getCountMiss())
        ) {
            score.setGeki(s.getCountGeki());
            score.setKatu(s.getCountKatu());
            score.setN300(s.getCount300());
            score.setN100(s.getCount100());
            score.setN50(s.getCount50());
            score.setMisses(s.getCountMiss());
        } else {
            score.setAccuracy(s.getAccuracy());
        }
        return Rosu.calculate(b, score);
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtend(List<Score> scoreList) {
        for (var score : scoreList) {
            applyBeatMapExtend(score);
        }
    }

    // 给标准谱面添加完整的谱面
    default void applyBeatMapExtend(Match.MatchRound round) {
        var b = Objects.requireNonNullElse(round.getBeatMap(), new BeatMap(round.getBeatMapID()));
        round.setBeatMap(getBeatMap(b.getBeatMapID()));
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtend(Score score) {
        var extended = getBeatMap(score.getBeatMap().getBeatMapID());
        var lite = score.getBeatMap();

        score.setBeatMap(BeatMap.extend(lite, extended));
        score.setBeatMapSet(extended.getBeatMapSet());
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtendFromDataBase(Score score) {
        var extended = getBeatMapFromDataBase(score.getBeatMap().getBeatMapID());
        var lite = score.getBeatMap();

        score.setBeatMap(BeatMap.extend(lite, extended));
        score.setBeatMapSet(extended.getBeatMapSet());
    }

    default void applySRAndPP(Score score) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        var modsInt = OsuMod.getModsValueFromAbbrList(score.getMods());

        // 没有变星数，并且有 PP，略过
        if (!OsuMod.hasChangeRating(modsInt) && score.getPP() > 0f) return;

        var beatMap = score.getBeatMap();
        JniResult r;
        try {
            r = getPP(score);

            if (r.getPp() <= 1) try {
                refreshBeatMapFile(beatMap.getBeatMapID());
                r = getPP(score);
            } catch (IOException ignored) {

            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        if (r.getPp() > 0) {
            score.setPP((float) r.getPp());
            beatMap.setStarRating((float) r.getStar());
        } else {
            NowbotApplication.log.info("无法获取{}的 PP！", beatMap.getBeatMapID());
        }

        DataUtil.applyBeatMapChanges(beatMap, modsInt);
    }

    default void applySRAndPP(List<Score> scoreList) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (CollectionUtils.isEmpty(scoreList)) return;

        for (var score : scoreList) {
            applySRAndPP(score);
        }
    }

    // 谱面理论sr和pp
    default void applySRAndPP(BeatMap beatMap, OsuMode mode, int modsInt) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (beatMap == null) return; // 谱面没有 PP，所以必须查

        var id = beatMap.getBeatMapID();

        JniResult r;
        try {
            r = getMaxPP(id, mode, modsInt);

            if (r.getPp() == 0) try {
                refreshBeatMapFile(id);
                r = getMaxPP(id, mode, modsInt);
            } catch (IOException ignored) {

            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        beatMap.setStarRating((float) r.getStar());
        DataUtil.applyBeatMapChanges(beatMap, modsInt);
    }

    default void applySRAndPP(BeatMap beatMap, MapStatisticsService.Expected expected) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (beatMap == null) return;
        JniResult r;

        var m = OsuMod.getModsValueFromAbbrList(expected.mods);
        try {
            var b = getBeatMapFileStr(beatMap.getBeatMapID()).getBytes(StandardCharsets.UTF_8);

            JniScore js = new JniScore();
            js.setCombo(expected.combo);
            js.setMode(expected.mode.toRosuMode());
            js.setMisses(expected.misses);
            js.setMods(m);

            r = Rosu.calculate(b, js);


            if (r.getPp() == 0) try {
                refreshBeatMapFile(beatMap.getBeatMapID());
                r = Rosu.calculate(b, js);
            } catch (IOException ignored) {

            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        beatMap.setStarRating((float) r.getStar());
        DataUtil.applyBeatMapChanges(beatMap, m);
    }
}
