package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.lang.Nullable;

import java.util.List;

public interface OsuPPPlusApiService {
    default PPPlus getBeatMapPPPlus(long bid) {
        return getBeatMapPPPlus(bid, false, OsuMode.DEFAULT, null, null, null);
    }

    default PPPlus getBeatMapPPPlus(long bid, boolean hasLeaderBoard) {
        return getBeatMapPPPlus(bid, hasLeaderBoard, OsuMode.DEFAULT, null, null, null);
    }

    default PPPlus getBeatMapPPPlus(long bid, OsuMode mode) {
        return getBeatMapPPPlus(bid, false, mode, null, null, null);
    }

    default PPPlus getBeatMapPPPlus(long bid, boolean hasLeaderBoard, OsuMode mode) {
        return getBeatMapPPPlus(bid, hasLeaderBoard, mode, null, null, null);
    }

    PPPlus getBeatMapPPPlus(long bid, boolean hasLeaderBoard, OsuMode mode, @Nullable Integer modsInt, @Nullable Integer combo, @Nullable Statistics stat);

    List<PPPlus> getBeatMapsPPPlus(List<BeatMap> beatMaps);

    PPPlus getUserPPPlus(long uid); //这个也是获取 BP，并且统计起来
}
