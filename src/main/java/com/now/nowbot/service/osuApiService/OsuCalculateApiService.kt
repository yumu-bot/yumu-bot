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

    fun applyStarToScore(score: LazerScore)

    fun applyStarToScores(scores: List<LazerScore>)

    fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: List<LazerScore>)

    fun getAccFcPPList(
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

    fun getStar(beatMapID: Long, mode: OsuMode, mods: List<LazerMod>): Double
}