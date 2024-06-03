package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;

import java.util.Arrays;
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
        return getBestPerformance(user.getUID(), user.getMode(), 0, 100);
    }

    default List<Score> getBestPerformance(OsuUser user, int offset, int limit) {
        return getBestPerformance(user.getUID(), user.getMode(), offset, limit);
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
        return getRecent(user.getUID(), user.getMode(), 0, 100);
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
        return getRecentIncludingFail(user.getUID(), user.getMode(), 0, 100);
    }

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
