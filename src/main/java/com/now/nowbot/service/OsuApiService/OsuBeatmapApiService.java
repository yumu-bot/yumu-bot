package com.now.nowbot.service.OsuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import rosu.osu.JniResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

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

    JniResult getMaxPP(long bid) throws Exception;
}
