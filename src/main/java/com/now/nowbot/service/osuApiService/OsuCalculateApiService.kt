package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.RosuPerformance

interface OsuCalculateApiService {
    fun getScorePerfectPP(score: LazerScore): RosuPerformance

    fun getScoreFullComboPP(score: LazerScore): RosuPerformance

    fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): RosuPerformance.FullRosuPerformance

    fun applyBeatMapChanges(score: LazerScore)

    fun applyBeatMapChanges(scores: List<LazerScore>)

    fun applyBeatMapChanges(beatMap: BeatMap?, mods: List<LazerMod>)

    fun applyStarToScore(score: LazerScore) {
        applyStarToScore(score, local = true)
    }

    fun applyStarToScore(score: LazerScore, local: Boolean = true)

    fun applyStarToScores(scores: List<LazerScore>) {
        applyStarToScores(scores, local = true)
    }

    fun applyStarToScores(scores: List<LazerScore>, local: Boolean = true)

    fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>) {
        applyStarToBeatMap(beatMap, mode, mods, local = true)
    }

    fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>, local: Boolean = true)

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

    fun getBeatMapStarRating(beatMapID: Long, mode: OsuMode, mods: List<LazerMod>): Double
}