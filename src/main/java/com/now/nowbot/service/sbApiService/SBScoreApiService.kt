package com.now.nowbot.service.sbApiService

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.ppysb.SBScore

interface SBScoreApiService {
    fun getScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, offset: Int? = 0, limit: Int? = 100, includeLoved: Boolean = true, includeFailed: Boolean = false, scope: String = "recent"): List<SBScore>

    fun getPassedScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, offset: Int? = 0, limit: Int? = 100): List<SBScore> {
        return getScore(id, username, mods, mode, offset, limit, includeLoved = true, includeFailed = false, scope = "recent")
    }

    fun getRecentScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, offset: Int? = 0, limit: Int? = 100): List<SBScore> {
        return getScore(id, username, mods, mode, offset, limit, includeLoved = true, includeFailed = true, scope = "recent")
    }

    fun getBestScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, offset: Int? = 0, limit: Int? = 100, isLoved: Boolean = false): List<SBScore> {
        return getScore(id, username, mods, mode, offset, limit, includeLoved = isLoved, includeFailed = true, scope = "best")
    }

    fun getBeatmapScore(
        id: Long?,
        md5: String? = null,
        mods: List<LazerMod>?,
        mode: OsuMode?,
        offset: Int? = 0,
        limit: Int? = 100,
        scope: String = "recent"
    ): List<SBScore>

    fun getBeatmapRecentScore(
        id: Long?,
        mods: List<LazerMod>?,
        mode: OsuMode?,
    ): List<SBScore> {
        return getBeatmapScore(
            id = id,
            mods = mods,
            mode = mode,
            offset = 0,
            limit = 100,
            scope = "recent"
        )
    }

    fun getLeaderBoardScore(
        id: Long?,
        mods: List<LazerMod>?,
        mode: OsuMode?,
    ): List<SBScore> {
        return getBeatmapScore(
            id = id,
            mods = mods,
            mode = mode,
            offset = 0,
            limit = 100,
            scope = "best"
        )
    }
}