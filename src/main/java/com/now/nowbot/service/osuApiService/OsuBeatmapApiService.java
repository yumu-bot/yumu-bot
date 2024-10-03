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
    // 获取谱面：先获取本地，再获取 bs api，最后获取网页
    String getBeatMapFileString(long bid);

    byte[] getBeatMapFileByte(long bid);

    boolean hasBeatMapFileFromDirectory(long bid);

    boolean refreshBeatMapFileFromDirectory(long bid);

    // 查一下文件是否跟 checksum 是否对得上
    boolean checkBeatMap(BeatMap beatMap) throws IOException;

    boolean checkBeatMap(long bid, String checkStr) throws IOException;

    boolean checkBeatMap(BeatMap beatMap, String fileStr) throws IOException;

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

    default JniResult getMaxPP(long bid, int modInt) {
        var b = getBeatMapFileByte(bid);
        JniScore js = new JniScore();
        js.setAccuracy(100);
        js.setMods(modInt);
        return Rosu.calculate(b, js);
    }

    default JniResult getMaxPP(Score s) {
        return getMaxPP(s.getBeatMap().getBeatMapID(), s.getMode(), OsuMod.getModsValueFromAbbrList(s.getMods()));
    }

    default JniResult getMaxPP(long bid, OsuMode mode, int modInt) {
        var b = getBeatMapFileByte(bid);
        JniScore js = new JniScore();

        js.setMode(mode.toRosuMode());
        js.setAccuracy(100D);
        js.setMisses(0);
        js.setMods(modInt);
        js.setSpeed(OsuMod.getModsClockRate(modInt));

        return Rosu.calculate(b, js);
    }

    default JniResult getPP(long bid, int modInt, Statistics s, int combo) throws Exception {
        var b = getBeatMapFileByte(bid);
        JniScore js = new JniScore();
        js.setCombo(combo);
        return getJniResult(modInt, s, b, js);
    }

    default JniResult getPP(long bid, OsuMode mode, int modInt, Statistics s, int combo) {
        var b = getBeatMapFileByte(bid);
        JniScore js = new JniScore();
        js.setCombo(combo);
        js.setMode(mode.toRosuMode());
        return getJniResult(modInt, s, b, js);
    }

    default JniResult getPP(BeatMap beatMap, MapStatisticsService.Expected e) throws Exception {
        var b = getBeatMapFileString(beatMap.getBeatMapID()).getBytes(StandardCharsets.UTF_8);
        var m = OsuMod.getModsValueFromAbbrList(e.mods);

        JniScore score = new JniScore();
        score.setCombo(e.combo);
        score.setMode(e.mode.toRosuMode());
        score.setMisses(e.misses);
        score.setAccuracy(e.accuracy);
        score.setMods(m);

        return Rosu.calculate(b, score);
    }

    default JniResult getPP(Score s) {
        var b = getBeatMapFileByte(s.getBeatMap().getBeatMapID());
        var m = OsuMod.getModsValueFromAbbrList(s.getMods());
        var t = s.getStatistics();

        JniScore js = new JniScore();
        js.setCombo(s.getMaxCombo());
        js.setMode(s.getMode().toRosuMode());

        if (!s.getPassed()) {
            // 没 pass 不给 300, acc 跟 combo
            js.setN100(t.getCount100());
            js.setKatu(t.getCountKatu());
            js.setN50(t.getCount50());
            js.setMisses(t.getCountMiss());
            return getJniResult(m, b, js);
        }
        return getJniResult(m, t, b, js);
    }

    default JniResult getFcPP(Score s) {
        var b = getBeatMapFileByte(s.getBeatMap().getBeatMapID());
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
        if (!s.getPassed()) {
            // 没 pass 不给 300, misses 跟 combo
            js.setN100(t.getCount100());
            js.setKatu(t.getCountKatu());
            js.setN50(t.getCount50());
            js.setAccuracy(s.getAccuracy());
            return getJniResult(m, b, js);
        }
        return getJniResult(m, t, b, js);
    }

    default Map<String, Object> getFullStatistics(Score s) {
        var jniResult = getMaxPP(s);

        return getStats(jniResult);
    }

    default Map<String, Object> getStatistics(Score s) {
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

    @SuppressWarnings("all")
    private JniResult getJniResult(int modInt, byte[] b, JniScore score) {
        score.setMods(modInt);
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

            if (r.getPp() == 0) {
                NowbotApplication.log.info("无法获取谱面 {} 的 PP，正在刷新谱面文件！", beatMap.getBeatMapID());
                refreshBeatMapFileFromDirectory(beatMap.getBeatMapID());
                r = getPP(score);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        if (r.getPp() > 0) {
            score.setPP((float) r.getPp());
            beatMap.setStarRating((float) r.getStar());
        } else {
            applyStarFromAttributes(beatMap, score.getMode(), modsInt);
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

            if (r.getPp() == 0) {
                NowbotApplication.log.info("无法获取谱面 {} 的 PP，正在刷新谱面文件！", beatMap.getBeatMapID());
                refreshBeatMapFileFromDirectory(id);
                r = getMaxPP(id, mode, modsInt);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        if (r.getPp() > 0) {
            beatMap.setStarRating((float) r.getStar());
        } else {
            applyStarFromAttributes(beatMap, mode, modsInt);
        }

        DataUtil.applyBeatMapChanges(beatMap, modsInt);
    }

    default void applySRAndPP(BeatMap beatMap, MapStatisticsService.Expected expected) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (beatMap == null) return;
        var m = OsuMod.getModsValueFromAbbrList(expected.mods);

        JniResult r;

        try {
            var b = getBeatMapFileString(beatMap.getBeatMapID()).getBytes(StandardCharsets.UTF_8);

            JniScore js = new JniScore();
            js.setCombo(expected.combo);
            js.setMode(expected.mode.toRosuMode());
            js.setMisses(expected.misses);
            js.setMods(m);

            r = Rosu.calculate(b, js);

            if (r.getPp() == 0) {
                NowbotApplication.log.info("无法获取谱面 {} 的 PP，正在刷新谱面文件！", beatMap.getBeatMapID());
                refreshBeatMapFileFromDirectory(beatMap.getBeatMapID());
                r = Rosu.calculate(b, js);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        if (r.getPp() > 0) {
            beatMap.setStarRating((float) r.getStar());
        } else {
            applyStarFromAttributes(beatMap, expected.mode, m);
        }

        DataUtil.applyBeatMapChanges(beatMap, m);
    }

    private void applyStarFromAttributes(BeatMap beatMap, OsuMode mode, Integer modsInt) {
        try {
            var attr = getAttributes(beatMap.getId(), mode, modsInt);

            if (attr.getStarRating() != null) {
                NowbotApplication.log.info("无法获取谱面 {}，正在应用 API 提供的星数：{}", beatMap.getBeatMapID(), attr.getStarRating());
                beatMap.setStarRating(attr.getStarRating());
            } else {
                NowbotApplication.log.error("无法获取谱面 {}，无法应用 API 提供的星数！", beatMap.getBeatMapID());
            }
        } catch (Exception e) {
            NowbotApplication.log.error("无法获取谱面 {}，无法获取 API 提供的星数！", beatMap.getBeatMapID(), e);
        }
    }
}
