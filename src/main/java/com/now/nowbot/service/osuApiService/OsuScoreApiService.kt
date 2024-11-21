package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.BinUser
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatmapUserScore
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser

interface OsuScoreApiService {

    // 获取最好成绩
    fun getBestScores(user: BinUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getBestScores(id: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getBestScores(user: OsuUser): List<LazerScore> {
        return getBestScores(user.userID, user.currentOsuMode, 0, 100)
    }

    fun getBestScores(user: BinUser, mode: OsuMode?): List<LazerScore> {
        return getBestScores(user = user, mode = mode, 0, 100)
    }

    fun getBestScores(id: Long, mode: OsuMode?): List<LazerScore> {
        return getBestScores(id = id, mode = mode, 0, 100)
    }

    fun getBestScores(user: OsuUser, offset: Int, limit: Int): List<LazerScore> {
        return getBestScores(user.userID, user.currentOsuMode, offset, limit)
    }

    // 默认获取成绩的接口，不用区分 pass 或 recent
    fun getScore(
        uid: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
        isPass: Boolean?,
    ): List<LazerScore> {
        return if (isPass == true) {
            getPassedScore(uid, mode, offset, limit)
        } else {
            getRecentScore(uid, mode, offset, limit)
        }
    }

    // 默认获取成绩的接口，不用区分 pass 或 recent，默认拿 recent
    fun getScore(
        uid: Long,
        mode: OsuMode?,
        isPass: Boolean?,
    ): List<LazerScore> {
        return getScore(uid = uid, mode = mode, offset = 0, limit = 100, isPass = isPass ?: false)
    }

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    fun getPassedScore(user: BinUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    fun getPassedScore(uid: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getPassedScore(user: OsuUser): List<LazerScore> {
        return getPassedScore(user.userID, user.currentOsuMode, 0, 100)
    }

    /**
     * 获得成绩 包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    fun getRecentScore(
            user: BinUser,
            mode: OsuMode?,
            offset: Int,
            limit: Int,
    ): List<LazerScore>

    fun getRecentScore(uid: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getRecentScore(user: OsuUser): List<LazerScore> {
        return getRecentScore(user.userID, user.currentOsuMode, 0, 100)
    }

    fun getBeatMapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore?

    fun getBeatMapScore(bid: Long, user: BinUser, mode: OsuMode?): BeatmapUserScore?

    fun getBeatMapScore(bid: Long, uid: Long, mode: OsuMode?, mods: Iterable<LazerMod?>): BeatmapUserScore?

    fun getBeatMapScore(
        bid: Long,
        user: BinUser,
        mode: OsuMode,
        mods: Iterable<LazerMod?>,
    ): BeatmapUserScore?

    fun getBeatMapScores(bid: Long, user: BinUser, mode: OsuMode?): List<LazerScore>

    fun getBeatMapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore>

    fun getLeaderBoardScore(bid: Long, mode: OsuMode?): List<LazerScore>
}
