package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.LazerMod;
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

    double getPlayPercentage(LazerScore score);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue);

    default BeatmapDifficultyAttributes getAttributes(Long id) {
        return getAttributes(id, OsuMode.DEFAULT);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, int value) {
        return getAttributes(id, OsuMode.DEFAULT, value);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, LazerMod... mods) {
        int value = Arrays.stream(mods).mapToInt(m -> m.type.value).reduce(0, Integer::sum);
        return getAttributes(id, value);
    }

    /**
     * 应该单独写一个返回类型
     */
    JsonNode lookupBeatmap(String checksum, String filename, Long id);

    BeatMapSetSearch searchBeatMapSet(Map<String, Object> query);

    default JniResult getMaxPP(long bid, List<LazerMod> mods) {
        return getMaxPP(bid, OsuMode.DEFAULT, mods);
    }

    default JniResult getMaxPP(LazerScore s) {
        return getMaxPP(s.getBeatMapID(), s.getMode(), s.getMods());
    }

    default JniResult getMaxPP(long bid, OsuMode ruleset, List<LazerMod> mods) {
        var b = getBeatMapFileByte(bid);
        JniScore js = new JniScore();

        applyDifficultyAdjust(js, mods);

        js.setMode(ruleset.toRosuMode());
        js.setAccuracy(100D);
        js.setMisses(0);
        js.setMods(LazerMod.getModsValue(mods));
        js.setSpeed(LazerMod.getModSpeedForStarCalculate(mods));

        return Rosu.calculate(b, js);
    }

    default JniResult getPP(BeatMap beatMap, MapStatisticsService.Expected e) throws Exception {
        var b = getBeatMapFileByte(beatMap.getBeatMapID());
        JniScore js = new JniScore();

        applyDifficultyAdjust(js, LazerMod.getModsList(e.mods));

        js.setCombo(e.combo);
        js.setMode(e.mode.toRosuMode());
        js.setMisses(e.misses);
        js.setAccuracy(e.accuracy);
        js.setMods(LazerMod.getModsValueFromAcronyms(e.mods));

        return Rosu.calculate(b, js);
    }

    default JniResult getPP(LazerScore s) {
        var b = getBeatMapFileByte(s.getBeatMapID());
        var t = s.getStatistics();

        JniScore js = new JniScore();

        applyDifficultyAdjust(js, s.getMods());

        js.setMods(LazerMod.getModsValue(s.getMods()));
        js.setCombo(s.getMaxCombo());
        js.setSpeed(LazerMod.getModSpeedForStarCalculate(s.getMods()));
        js.setMode(s.getMode().toRosuMode());

        if (s.getPassed()) {
            return getJniResult(s, b, js);
        } else {
            // 没 pass 不给 300, acc 跟 combo
            js.setN100(Objects.requireNonNullElse(t.getOk(), 0));
            js.setKatu(Objects.requireNonNullElse(t.getGood(), 0));
            js.setN50(Objects.requireNonNullElse(t.getMeh(), 0));
            js.setMisses(Objects.requireNonNullElse(t.getMiss(), 0));

            return getJniResult(b, js);
        }
    }

    default JniResult getFcPP(LazerScore s) {
        var b = getBeatMapFileByte(s.getBeatMapID());
        var m = s.getMods();
        var r = s.getMode();
        var t = s.getStatistics().clone();
        var a = s.getAccuracy();

        if (s.getScoreHit() > 0 && t.getMiss() != null && t.getMiss() > 0) {
            t.setMiss(0);
            t.setGreat(Objects.requireNonNullElse(t.getGreat(), 0) + t.getMiss());
        }

        if (s.getBeatMap().getMaxCombo() == null) {
            applyBeatMapExtend(s);
        }

        JniScore js = new JniScore();

        applyDifficultyAdjust(js, m);

        js.setMode(s.getMode().toRosuMode());
        js.setMods(LazerMod.getModsValue(s.getMods()));

        if (!s.getPassed()) {
            // 没 pass 不给 300, misses 跟 combo
            js.setN100(Objects.requireNonNullElse(t.getOk(), 0));
            js.setN50(Objects.requireNonNullElse(t.getMeh(), 0));
            js.setAccuracy(s.getAccuracy());
            return getJniResult(b, js);
        }

        return getJniResult(t, m, a, r, b, js);
    }

    default Map<String, Object> getFullStatistics(LazerScore s) {
        var jniResult = getMaxPP(s);

        return getStats(jniResult);
    }

    default Map<String, Object> getStatistics(LazerScore s) {
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
    private JniResult getJniResult(LazerScore score, byte[] map, JniScore jni) {
        return getJniResult(score.getStatistics(), score.getMods(), score.getAccuracy(), score.getMode(), map, jni);
    }

    @NonNull
    private JniResult getJniResult(LazerScore.StatisticsV2 t, List<LazerMod> mods, double accuracy, OsuMode ruleset, byte[] map, JniScore jni) {
        jni.setMods(LazerMod.getModsValue(mods));
        jni.setAccuracy(accuracy);

        switch (ruleset) {
            case TAIKO -> {
                if (t.getOk() != null) jni.setN100(t.getOk());
            }
            case CATCH -> {
                if (t.getLargeTickHit() != null) jni.setN100(t.getLargeTickHit());
                if (t.getSmallTickHit() != null) jni.setN50(t.getSmallTickHit());
            }
            case MANIA -> {
                if (t.getPerfect() != null) jni.setGeki(t.getPerfect());
                if (t.getGood() != null) jni.setKatu(t.getGood());
                if (t.getOk() != null) jni.setN100(t.getOk());
                if (t.getMeh() != null) jni.setN50(t.getMeh());
            }
            default -> {
                if (t.getOk() != null) jni.setN100(t.getOk());
                if (t.getMeh() != null) jni.setN50(t.getMeh());
            }
        }
        if (t.getGreat() != null) jni.setN300(t.getGreat());
        if (t.getMiss() != null) jni.setMisses(t.getMiss());

        // 这个要留着, 因为是调用了 native 方法
        // 那边如果有 null 会直接导致虚拟机炸掉退出, 注解不会在运行时检查是不是 null
        /*
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
            score.setAccuracy(s.get());
        }

         */

        return Rosu.calculate(map, jni);
    }

    @SuppressWarnings("all")
    private JniResult getJniResult(byte[] map, JniScore score) {
        return Rosu.calculate(map, score);
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtend(List<LazerScore> scoreList) {
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
    default void applyBeatMapExtend(LazerScore score) {
        var extended = getBeatMap(score.getBeatMap().getBeatMapID());
        var lite = score.getBeatMap();

        score.setBeatMap(BeatMap.extend(lite, extended));
        if (extended.getBeatMapSet() != null) {
            score.setBeatMapSet(extended.getBeatMapSet());
        }
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtendFromDataBase(LazerScore score) {
        var extended = getBeatMapFromDataBase(score.getBeatMap().getBeatMapID());
        var lite = score.getBeatMap();

        score.setBeatMap(BeatMap.extend(lite, extended));
        if (extended.getBeatMapSet() != null) {
            score.setBeatMapSet(extended.getBeatMapSet());
        }
    }

    default void applySRAndPP(LazerScore score) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;

        // 没有变星数，并且有 PP，略过
        if (!LazerMod.hasChangeRating(score.getMods()) && score.getPP() > 0d) return;

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
            score.setPP(r.getPp());
            beatMap.setStarRating((float) r.getStar());
        } else {
            applyStarFromAttributes(beatMap, score.getMode(), score.getMods());
        }

        DataUtil.applyBeatMapChanges(beatMap, score.getMods());
    }

    default void applySRAndPP(List<LazerScore> scoreList) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (CollectionUtils.isEmpty(scoreList)) return;

        for (var score : scoreList) {
            applySRAndPP(score);
        }
    }

    // 谱面理论sr和pp
    default void applySRAndPP(BeatMap beatMap, OsuMode ruleset, List<LazerMod> mods) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (beatMap == null) return; // 谱面没有 PP，所以必须查

        var id = beatMap.getBeatMapID();

        JniResult r;
        try {
            r = getMaxPP(id, ruleset, mods);

            if (r.getPp() == 0) {
                NowbotApplication.log.info("无法获取谱面 {} 的 PP，正在刷新谱面文件！", beatMap.getBeatMapID());
                refreshBeatMapFileFromDirectory(id);
                r = getMaxPP(id, ruleset, mods);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        if (r.getPp() > 0) {
            beatMap.setStarRating((float) r.getStar());
        } else {
            applyStarFromAttributes(beatMap, ruleset, mods);
        }

        DataUtil.applyBeatMapChanges(beatMap, mods);
    }

    default void applySRAndPP(BeatMap beatMap, MapStatisticsService.Expected expected) {
        if (ContextUtil.getContext("breakApplySR", false, Boolean.class)) return;
        if (beatMap == null) return;
        var mods = LazerMod.getModsList(expected.mods);

        JniResult r;

        try {
            var b = getBeatMapFileString(beatMap.getBeatMapID()).getBytes(StandardCharsets.UTF_8);

            JniScore js = new JniScore();
            js.setCombo(expected.combo);
            js.setMode(expected.mode.toRosuMode());
            js.setMisses(expected.misses);
            js.setMods(LazerMod.getModsValue(mods));

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
            applyStarFromAttributes(beatMap, expected.mode, mods);
        }

        DataUtil.applyBeatMapChanges(beatMap, mods);
    }

    private void applyStarFromAttributes(BeatMap beatMap, OsuMode ruleset, List<LazerMod> mods) {
        try {
            var attr = getAttributes(beatMap.getId(), ruleset, LazerMod.getModsValue(mods));

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

    // 应用 DA 模组会修改的四维
    private void applyDifficultyAdjust(JniScore jni, List<LazerMod> mods) {
        for (var m : mods) {
            if (Objects.equals(m.type.acronym, "DA")) {
                if (m.getCs() != null) {
                    jni.setCs(m.getCs());
                }
                if (m.getAr() != null) {
                    jni.setAr(m.getAr());
                }
                if (m.getOd() != null) {
                    jni.setOd(m.getOd());
                }
                if (m.getHp() != null) {
                    jni.setHp(m.getHp());
                }
            }
        }
    }
}
