package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.cosu.CosuResponse
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.RosuPerformance

interface OsuCalculateApiService {
    fun getScorePerfectPP(score: LazerScore): RosuPerformance

    fun getScoreFullComboPP(score: LazerScore): RosuPerformance

    fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): RosuPerformance.FullRosuPerformance?

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

    fun calculateDifficulty(bid: Long, mode: OsuMode, mods: List<LazerMod>? = null): CosuResponse

    fun getScoreMapStar(score: LazerScore): Double
}