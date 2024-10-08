package com.now.nowbot.service.osuApiService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.BeatmapUserScore;
import com.now.nowbot.model.json.OsuUser;
import com.now.nowbot.model.json.Score;

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
        return getBestPerformance(user.getUserID(), user.getCurrentOsuMode(), 0, 100);
    }

    default List<Score> getBestPerformance(OsuUser user, int offset, int limit) {
        return getBestPerformance(user.getUserID(), user.getCurrentOsuMode(), offset, limit);
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

    default List<Score> getRecent(long uid, OsuMode mode, int offset, int limit, boolean isPass) {
        if (isPass) {
            return getRecent(uid, mode, offset, limit);
        } else {
            return getRecentIncludingFail(uid, mode, offset, limit);
        }
    }

    default List<Score> getRecent(OsuUser user) {
        return getRecent(user.getUserID(), user.getCurrentOsuMode(), 0, 100);
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
        return getRecentIncludingFail(user.getUserID(), user.getCurrentOsuMode(), 0, 100);
    }

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode);

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode);

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Iterable<OsuMod> mods);

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode, Iterable<OsuMod> mods);

    List<Score> getScoreAll(long bid, BinUser user, OsuMode mode);


    List<Score> getScoreAll(long bid, long uid, OsuMode mode);

    List<Score> getBeatMapScores(long bid, OsuMode mode);

    /**
     * 此功能作废, ppy将其限定为 `Resource Owner` 权限
     */
    @Deprecated
    byte[] getReplay(long id, OsuMode mode);
}
