package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface OsuScoreApiService {

    /**
     * 获取bp 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    List<Score> getBestPerformance(BinUser user, OsuMode mode, int offset, int limit);

    List<Score> getBestPerformance(Long id, OsuMode mode, int offset, int limit);

    default List<Score> getBestPerformance(OsuUser user) {
        return getBestPerformance(user.getUID(), user.getOsuMode(), 0, 100);
    }

    default List<Score> getBestPerformance(OsuUser user, int offset, int limit) {
        return getBestPerformance(user.getUID(), user.getOsuMode(), offset, limit);
    }

    default List<Score> getUserDefaultBestPerformance(BinUser user) {
        return getBestPerformance(user, OsuMode.DEFAULT, 0, 100);
    }

    default List<Score> getUserDefaultBestPerformance(Long id) {
        return getBestPerformance(id, OsuMode.DEFAULT, 0, 100);
    }

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    List<Score> getRecent(BinUser user, OsuMode mode, int offset, int limit);

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    List<Score> getRecent(long uid, OsuMode mode, int offset, int limit);

    default List<Score> getRecent(long uid, OsuMode mode, int offset, int limit, boolean isPassed) {
        if (isPassed) {
            return getRecent(uid, mode, offset, limit);
        } else {
            return getRecentIncludingFail(uid, mode, offset, limit);
        }
    }

    default List<Score> getRecent(OsuUser user) {
        return getRecent(user.getUID(), user.getOsuMode(), 0, 100);
    }

    /**
     * 获得成绩 包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    List<Score> getRecentIncludingFail(BinUser user, OsuMode mode, int offset, int limit);

    List<Score> getRecentIncludingFail(long uid, OsuMode mode, int offset, int limit);

    default List<Score> getRecentIncludingFail(OsuUser user) {
        return getRecentIncludingFail(user.getUID(), user.getOsuMode(), 0, 100);
    }

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode);

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode);

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Iterable<Mod> mods);

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode, Iterable<Mod> mods);

    List<Score> getScoreAll(long bid, BinUser user, OsuMode mode);


    List<Score> getScoreAll(long bid, long uid, OsuMode mode);

    List<Score> getBeatmapScores(long bid, OsuMode mode);

    /**
     * 此功能作废, ppy将其限定为 `Resource Owner` 权限
     */
    @Deprecated
    byte[] getReplay(long id, OsuMode mode);
}
