package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.BindUser
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Replay
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.BeatmapUserScore
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser

interface OsuScoreApiService {

    fun getCovers(scores: List<LazerScore>, type: CoverType): List<ByteArray?>

    fun getCover(score: LazerScore, type: CoverType): ByteArray?

    // 获取最好成绩
    fun getBestScores(id: Long, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore>

    fun getBestScores(user: BindUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore> {
        return getBestScores(user.userID, mode, offset, limit)
    }

    fun getBestScores(user: OsuUser, mode: OsuMode?, offset: Int, limit: Int): List<LazerScore> {
        return getBestScores(user.userID, mode, offset, limit)
    }

    fun getBestScores(user: OsuUser): List<LazerScore> {
        return getBestScores(user.userID, user.currentOsuMode, 0, 200)
    }

    fun getBestScores(user: OsuUser, mode: OsuMode?): List<LazerScore> {
        return getBestScores(user.userID, mode, 0, 200)
    }

    fun getBestScores(user: BindUser, mode: OsuMode?): List<LazerScore> {
        return getBestScores(user = user, mode = mode, 0, 200)
    }

    fun getBestScores(id: Long, mode: OsuMode?): List<LazerScore> {
        return getBestScores(id = id, mode = mode, 0, 200)
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

    /*

     */
    fun getScore(scoreID: Long): LazerScore

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

    fun getRecentScore(uid: Long, mode: OsuMode?, offset: Int, limit: Int, isBackground: Boolean = false): List<LazerScore>

    fun getRecentScore(user: OsuUser): List<LazerScore> {
        return getRecentScore(user.userID, user.currentOsuMode, 0, 100)
    }

    fun getBeatmapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore?

    fun getBeatmapScore(bid: Long, user: BindUser, mode: OsuMode?): BeatmapUserScore?

    fun getBeatmapScore(bid: Long, uid: Long, mode: OsuMode?, mods: Collection<LazerMod?>): BeatmapUserScore?

    fun getBeatmapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode,
        mods: Collection<LazerMod>,
    ): BeatmapUserScore?

    fun getBeatmapScores(bid: Long, user: BindUser, mode: OsuMode?): List<LazerScore>

    fun getBeatmapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore>

    fun getLeaderBoardScore(
        bindUser: BindUser?,
        bid: Long,
        mode: OsuMode?,
        mods: Collection<LazerMod>?,
        type: String?,
        legacy: Boolean = false
    ): List<LazerScore>

    fun asyncDownloadBackgroundFromScores(beatmap: Beatmap, type: Iterable<CoverType>) {
        type.forEach {
            asyncDownloadBackground(listOf(beatmap.beatmapset?.covers ?: Covers()), it)
        }
    }

    fun asyncDownloadBackground(covers: Iterable<Covers>, type: CoverType? = CoverType.COVER)

    fun asyncDownloadBackgroundFromScores(scores: Iterable<LazerScore>, type: CoverType? = CoverType.COVER) {
        asyncDownloadBackground(scores.map { it.beatmapset.covers }, type)
    }

    fun asyncDownloadBackgroundFromScores(scores: Iterable<LazerScore>, type: Iterable<CoverType>) {
        type.forEach {
            asyncDownloadBackgroundFromScores(scores, it)
        }
    }


    fun getReplay(score: LazerScore): Replay?
}
