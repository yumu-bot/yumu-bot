package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.RosuPerformance

interface OsuCalculateApiService {
    fun getScorePerfectPP(score: LazerScore): RosuPerformance

    fun getScoreFullComboPP(score: LazerScore): RosuPerformance

    fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): RosuPerformance.FullRosuPerformance

    fun applyBeatMapChanges(score: LazerScore)

    fun applyBeatMapChanges(scores: List<LazerScore>)

    fun applyBeatMapChanges(beatmap: Beatmap?, mods: List<LazerMod>)

    fun applyStarToScore(score: LazerScore) {
        applyStarToScore(score, local = true)
    }

    fun applyStarToScore(score: LazerScore, local: Boolean = true)

    fun applyStarToScores(scores: List<LazerScore>) {
        applyStarToScores(scores, local = true)
    }

    fun applyStarToScores(scores: List<LazerScore>, local: Boolean = true)

    fun applyStarToBeatMap(beatmap: Beatmap?, mode: OsuMode, mods: List<LazerMod>) {
        applyStarToBeatMap(beatmap, mode, mods, local = true)
    }

    fun applyStarToBeatMap(beatmap: Beatmap?, mode: OsuMode, mods: List<LazerMod>, local: Boolean = true)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: List<LazerScore>)

    fun getAccPPList(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>?,
        maxCombo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: DoubleArray
    ): List<Double>

    fun getAccPP(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>?,
        maxCombo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: Double
    ): RosuPerformance

    fun getBeatMapStarRating(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>): Double
}