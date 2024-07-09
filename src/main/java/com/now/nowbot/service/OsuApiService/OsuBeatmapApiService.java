package com.now.nowbot.service.OsuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.util.DataUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import rosu.Rosu;
import rosu.parameter.JniScore;
import rosu.result.JniResult;

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

    String downloadBeatMapFileForce(long bid);

    /**
     * 查一下文件是否跟 checksum 是否对起来
     *
     * @return 是否对得上
     */
    boolean checkBeatMap(BeatMap beatMap) throws IOException;

    boolean checkBeatMap(long bid, String checkStr) throws IOException;

    BeatMap getBeatMapInfo(long bid);

    default BeatMap getBeatMapInfo(int bid) {
        return getBeatMapInfo((long) bid);
    }

    BeatMapSet getBeatMapSetInfo(long sid);

    default BeatMapSet getBeatMapSetInfo(int sid) {
        return getBeatMapSetInfo((long) sid);
    }

    default BeatMap getBeatMapInfoFromDataBase(int bid) {
        return getBeatMapInfoFromDataBase((long) bid);
    }

    BeatMap getBeatMapInfoFromDataBase(long bid);

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

        score.setMode(mode.toRosuMode());
        score.setAccuracy(100D);
        score.setMisses(0);
        score.setMods(modInt);
        score.setSpeed(OsuMod.getModsClockRate(modInt));

        return Rosu.calculate(b, score);
    }

    default JniResult getPP(long bid, int modInt, Statistics s) throws Exception {
        var b = getBeatMapFile(bid).getBytes(StandardCharsets.UTF_8);
        JniScore score = new JniScore();
        score.setCombo(s.getMaxCombo());
        return getJniResult(modInt, s, b, score);
    }

    default JniResult getPP(long bid, OsuMode mode, int modInt, Statistics s) throws Exception {
        var b = getBeatMapFile(bid).getBytes(StandardCharsets.UTF_8);
        JniScore score = new JniScore();
        score.setCombo(s.getMaxCombo());
        score.setMode(mode.toRosuMode());
        return getJniResult(modInt, s, b, score);
    }

    @NotNull
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

    default void applyStarRatingChange(List<Score> scoreList) {
        if (CollectionUtils.isEmpty(scoreList)) return;

        for (var score : scoreList) {
            var modsInt = OsuMod.getModsValueFromAbbrList(score.getMods());
            // score.getPP() 实际上永远不会为 null, 因为里面判断了 null 返回 0
            if (!OsuMod.hasChangeRating(modsInt)) continue;

            var beatMap = score.getBeatMap();
            JniResult r;
            try {
                r = getMaxPP(beatMap.getBeatMapID(), score.getMode(), modsInt);
            } catch (Exception e) {
                NowbotApplication.log.error("计算时出现异常", e);
                continue;
            }

            beatMap.setStarRating((float) r.getStar());
        }
    }

    default void applyStarRatingChange(BeatMap beatMap, OsuMode mode, int modsInt) {
        if (beatMap == null) return; // 谱面没有 PP，所以必须查
        JniResult r;

        try {
            r = getMaxPP(beatMap.getBeatMapID(), mode, modsInt);
        } catch (Exception e) {
            NowbotApplication.log.error("计算时出现异常", e);
            return;
        }

        beatMap.setStarRating((float) r.getStar());
        DataUtil.applyBeatMapChanges(beatMap, modsInt);
    }
}
