package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.LazerMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import org.springframework.lang.Nullable;

import java.io.IOException;
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

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int value);

    default BeatmapDifficultyAttributes getAttributes(Long id) {
        return getAttributes(id, OsuMode.DEFAULT);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, int value) {
        return getAttributes(id, OsuMode.DEFAULT, value);
    }

    default BeatmapDifficultyAttributes getAttributes(Long id, @Nullable List<LazerMod> mods) {
        if (mods == null || mods.isEmpty()) return getAttributes(id, OsuMode.DEFAULT);

        int value = mods.stream().mapToInt(m -> m.type.value).reduce(0, Integer::sum);
        return getAttributes(id, value);
    }

    JsonNode lookupBeatmap(String checksum, String filename, Long id);

    BeatMapSetSearch searchBeatMapSet(Map<String, Object> query);

    // 给同一张图的成绩添加完整的谱面
    default void applyBeatMapExtendForSameScore(List<LazerScore> scoreList) {
        if (scoreList.isEmpty()) return;

        var extended = getBeatMap(scoreList.getFirst().getBeatMapID());

        for (var score : scoreList) {
            var lite = score.getBeatMap();

            score.setBeatMap(BeatMap.extend(lite, extended));
            if (extended.getBeatMapSet() != null) {
                score.setBeatMapSet(extended.getBeatMapSet());
            }
        }
    }

    // 给标准谱面添加完整的谱面
    default void applyBeatMapExtend(Match.MatchRound round) {
        var b = Objects.requireNonNullElse(round.getBeatMap(), new BeatMap());
        round.setBeatMap(getBeatMap(b.getBeatMapID()));
    }

    // 给成绩添加完整的谱面
    default void applyBeatMapExtend(LazerScore score) {
        var extended = getBeatMap(score.getBeatMapID());
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
}
