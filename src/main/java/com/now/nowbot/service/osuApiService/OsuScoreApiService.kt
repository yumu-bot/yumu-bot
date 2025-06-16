package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.BindUser
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Replay
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.BeatmapUserScore
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser

interface OsuScoreApiService {

    // 获取最好成绩
    fun getBestScores(id: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getBestScores(user: BindUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore> {
        return getBestScores(user.userID, mode, offset, limit)
    }

    fun getBestScores(user: OsuUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore> {
        return getBestScores(user.userID, mode, offset, limit)
    }

    fun getBestScores(user: OsuUser): List<LazerScore> {
        return getBestScores(user.userID, user.currentOsuMode, 0, 100)
    }

    fun getBestScores(user: OsuUser, mode: OsuMode?): List<LazerScore> {
        return getBestScores(user.userID, mode, 0, 100)
    }

    fun getBestScores(user: BindUser, mode: OsuMode?): List<LazerScore> {
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

    /**
     * 获得成绩 不包含fail 共获取 e 条 [s, e)
     *
     * @param offset 起始 从0开始
     * @param limit 数据是几条
     */
    fun getPassedScore(user: BindUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

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
        user: BindUser,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore>

    fun getRecentScore(uid: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getRecentScore(user: OsuUser): List<LazerScore> {
        return getRecentScore(user.userID, user.currentOsuMode, 0, 100)
    }

    fun getBeatMapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore?

    fun getBeatMapScore(bid: Long, user: BindUser, mode: OsuMode?): BeatmapUserScore?

    fun getBeatMapScore(bid: Long, uid: Long, mode: OsuMode?, mods: Iterable<LazerMod?>): BeatmapUserScore?

    fun getBeatMapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode,
        mods: Iterable<LazerMod?>,
    ): BeatmapUserScore?

    fun getBeatMapScores(bid: Long, user: BindUser, mode: OsuMode?): List<LazerScore>

    fun getBeatMapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore>

    fun getLeaderBoardScore(bid: Long, mode: OsuMode?, legacy: Boolean = false): List<LazerScore>


    fun asyncDownloadBackground(scores: Iterable<LazerScore>, type: Iterable<CoverType>) {
        type.forEach {
            asyncDownloadBackground(scores, it)
        }
    }


    fun asyncDownloadBackground(score: LazerScore, type: Iterable<CoverType>) {
        type.forEach {
            asyncDownloadBackground(listOf(score), it)
        }
    }

    fun asyncDownloadBackground(scores: Iterable<LazerScore>, type: CoverType? = CoverType.COVER)

    fun asyncDownloadBackground(score: LazerScore, type: CoverType? = CoverType.COVER) {
        asyncDownloadBackground(listOf(score), type)
    }


    fun getReplay(score: LazerScore): Replay?
}
