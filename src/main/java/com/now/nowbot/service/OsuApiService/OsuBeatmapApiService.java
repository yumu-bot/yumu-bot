package com.now.nowbot.service.OsuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;

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

    BeatMap getBeatMapInfo(long bid);

    default BeatMap getBeatMapInfo(int bid) {
        return getBeatMapInfo((long) bid);
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

}
