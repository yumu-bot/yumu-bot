package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.calculate.CalculatePerformance
import com.now.nowbot.model.calculate.FullCalculatePerformance
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore

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

    /**
     * 一次批量计算星数的入参。每张谱面的模式和 mods 都可能不同，所以不能只传 beatmap。
     */
    data class BeatmapStarRequest(
        val beatmap: Beatmap,
        val mode: OsuMode,
        val mods: List<LazerMod> = emptyList(),
    )

    /**
     * 批量给谱面应用星数。
     *
     * 和逐张调用 [applyStarToBeatmap] 相比，内部只会走一次批量星数计算（本地缓存 / Rosu / 官方接口），
     * 适合一场比赛里几十张谱面的场景。
     */
    fun applyStarToBeatmaps(requests: Collection<BeatmapStarRequest>)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: Collection<LazerScore>)

    fun applyPPToScoresWithSameBeatmap(scores: Collection<LazerScore>)

    fun getPPFromAccuracies(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        combo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: DoubleArray,
        clockRate: Double? = null,
    ): List<Double>

    fun getPPFromAccuracy(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        combo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: Double,
        clockRate: Double? = null,
    ): Double

    fun getPerformanceFromAccuracy(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        combo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: Double,
        clockRate: Double?
    ): CalculatePerformance

    fun getBeatmapStar(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        hasLeaderBoard: Boolean = false
    ): Double
}