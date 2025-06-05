package com.now.nowbot.service.sbApiService

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.ppysb.SBScore

interface SBScoreApiService {
    fun getScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, limit: Int? = 100, includedLoved: Boolean = true, includeFailed: Boolean = false, scope: String = "recent"): List<SBScore>

    fun getPassedScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, limit: Int? = 100): List<SBScore> {
        return getScore(id, username, mods, mode, limit, includedLoved = true, includeFailed = false, scope = "recent")
    }

    fun getRecentScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, limit: Int? = 100): List<SBScore> {
        return getScore(id, username, mods, mode, limit, includedLoved = true, includeFailed = true, scope = "recent")
    }

    fun getBestScore(id: Long? = null, username: String? = null, mods: List<LazerMod>? = null, mode: OsuMode? = OsuMode.OSU, limit: Int? = 100, isLoved: Boolean = false): List<SBScore> {
        return getScore(id, username, mods, mode, limit, includedLoved = isLoved, includeFailed = true, scope = "best")
    }
}