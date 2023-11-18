package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;

import java.util.Arrays;
import java.util.List;

public interface OsuScoreApiService {
    /**
     * 获取bp 共获取 e 条 [s, e)
     *
     * @param s 起始 从0开始
     * @param e 数据是几条
     */
    List<Score> getBestPerformance(BinUser user, OsuMode mode, int s, int e);

    List<Score> getBestPerformance(Long id, OsuMode mode, int s, int e);

    default List<Score> getUserDefaultBestPerformance(BinUser user) {
        return getBestPerformance(user, OsuMode.DEFAULT, 0, 100);
    }

    default List<Score> getUserDefaultBestPerformance(Long id) {
        return getBestPerformance(id, OsuMode.DEFAULT, 0, 100);
    }

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param s 起始 从0开始
     * @param e 数据是几条
     */
    List<Score> getRecent(BinUser user, OsuMode mode, int s, int e);

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param s 起始 从0开始
     * @param e 数据是几条
     */
    List<Score> getRecent(long uid, OsuMode mode, int s, int e);

    /**
     * 获得成绩 包含fail 共获取 e 条 [s, e)
     *
     * @param s 起始 从0开始
     * @param e 数据是几条
     */
    List<Score> getRecentIncludingFail(BinUser user, OsuMode mode, int s, int e);

    List<Score> getRecentIncludingFail(long uid, OsuMode mode, int s, int e);

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode);

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode);

    /**
     * 此接口 ppy 还没实现, 如果带 mod,单个数据结果是错误的
     */
    BeatmapUserScore getScore(long bid, long uid, OsuMode mode, int modsValue);

    default BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Mod... mods) {
        int value = Arrays.stream(mods).map(m -> m.value).reduce(0, ((a, b) -> a | b));
        return getScore(bid, uid, mode, value);
    }

    List<Score> getScoreAll(long bid, BinUser user, OsuMode mode);


    List<Score> getScoreAll(long bid, long uid, OsuMode mode);

    List<Score> getBeatmapScores(long bid, OsuMode mode);

    /**
     * 此功能作废, ppy将其限定为 `Resource Owner` 权限
     */
    @Deprecated
    byte[] getReplay(long id, OsuMode mode);
}
