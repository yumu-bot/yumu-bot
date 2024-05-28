package com.now.nowbot.service.OsuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.util.DataUtil;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import rosu.osu.JniResult;
import rosu.osu.JniScore;
import rosu.osu.Rosu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface OsuBeatmapApiService {
    /**
     * 下载beatmap(.osu)文件
     *
     * @param bid 谱面id
     * @return osu文件字符串
     */
    String getBeatMapFile(long bid) throws Exception;

    boolean downloadBeatMapFile(long bid);

    String downloadBeatMapFileForce(long bid);

    /**
     * 查一下文件是否跟 checksum 是否对起来
     *
     * @return 是否对得上
     */
    default boolean checkBeatMap(BeatMap beatMap) throws IOException {
        if (beatMap == null) return false;
        return checkBeatMap(beatMap.getId(), beatMap.getMd5());
    }

    boolean checkBeatMap(long bid, String checkStr) throws IOException;

    BeatMap getBeatMapInfo(long bid);

    default BeatMap getBeatMapInfo(int bid) {
        return getBeatMapInfo((long) bid);
    }

    BeatMapSet getBeatMapSetInfo(long sid);

    default BeatMapSet getBeatMapSetInfo(int sid) {
        return getBeatMapSetInfo((long) sid);
    }

    BeatMap getMapInfoFromDB(long bid);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue);

    default BeatmapDifficultyAttributes getAttributes(Long id) {
        return getAttributes(id, OsuMode.DEFAULT);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, int modsValue) {
        return getAttributes(id, OsuMode.DEFAULT, modsValue);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, Mod... mods) {
        int value = Arrays.stream(mods).mapToInt(m -> m.value).reduce(0, Integer::sum);
        return getAttributes(id, value);
    }

    /**
     * 应该单独写一个返回类型
     */
    JsonNode lookupBeatmap(String checksum, String filename, Long id);

    Search searchBeatmap(Map<String, Object> query);

    default JniResult getMaxPP(long bid, int modInt) throws Exception {
        var b = getBeatMapFile(bid).getBytes(StandardCharsets.UTF_8);
        JniScore score = new JniScore();
        score.setAccuracy(100);
        score.setMods(modInt);
        return Rosu.calculate(b, score);
    }

    default JniResult getMaxPP(long bid, OsuMode mode, int modInt) throws Exception {
        var b = getBeatMapFile(bid).getBytes(StandardCharsets.UTF_8);
        JniScore score = new JniScore();
        score.setMode(mode.toMode());
        score.setAccuracy(100);
        score.setMods(modInt);
        return Rosu.calculate(b, score);
    }

    default JniResult getPP(long bid, int modInt, Statistics s) throws Exception {
        var b = getBeatMapFile(bid).getBytes(StandardCharsets.UTF_8);
        JniScore score = new JniScore();
        score.setCombo(s.getMaxCombo());
        score.setMods(modInt);
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
        var r = Rosu.calculate(b, score);
        return r;
    }

    default void applyModChangeForScores(List<Score> scoreList, @NonNull OsuMode mode) {
        if (CollectionUtils.isEmpty(scoreList)) return;

        // 一次搞定
        for (var s : scoreList) {
            var v = Mod.getModsValueFromAbbrList(s.getMods());

            if (Mod.hasChangeRating(v)) {
                var b = s.getBeatMap();
                JniResult r;
                try {
                    r = getMaxPP(b.getId(), mode, v);
                } catch (Exception e) {
                    NowbotApplication.log.error("计算时出现异常", e);
                    continue;
                }

                b.setStarRating((float) r.getStar());
                b.setOD(DataUtil.OD(b.getOD(), v));
                b.setAR(DataUtil.AR(b.getAR(), v));
                b.setCS(DataUtil.CS(b.getCS(), v));
                b.setHP(DataUtil.HP(b.getHP(), v));
                if (Mod.hasDt(v)) {
                    b.setBPM(b.getBPM() * 1.5f);
                    b.setTotalLength(Math.round(b.getTotalLength() / 1.5f));
                } else if (Mod.hasHt(v)) {
                    b.setBPM(b.getBPM() * 0.75f);
                    b.setTotalLength(Math.round(b.getTotalLength() / 0.75f));
                }
            }
        }
    }
}
