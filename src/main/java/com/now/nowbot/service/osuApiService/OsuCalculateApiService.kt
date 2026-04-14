package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.calculate.CalculatePerformance
import com.now.nowbot.model.calculate.FullCalculatePerformance
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.calculate.RosuPerformance

interface OsuCalculateApiService {
    fun getScorePerfectPP(score: LazerScore): CalculatePerformance

    fun getScoreFullComboPP(score: LazerScore): CalculatePerformance

    fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): FullCalculatePerformance?

    fun applyStarToScore(score: LazerScore)

    fun applyStarToScores(scores: Collection<LazerScore>)

    fun applyStarToScores(scoreMap: Map<*, LazerScore>) {
        applyStarToScores(scoreMap.values)
    }

    fun applyStarToBeatmap(beatmap: Beatmap?, mode: OsuMode, mods: List<LazerMod>)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: Collection<LazerScore>)

    fun applyPPToScoresWithSameBeatmap(scores: Collection<LazerScore>)

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

    fun getBeatmapStar(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        hasLeaderBoard: Boolean = false
    ): Double
}