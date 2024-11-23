package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.RosuPerformance
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService

interface OsuCalculateApiService {
    fun getBeatMapPerfectPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance

    fun getScorePerfectPP(score: LazerScore): RosuPerformance

    fun getBeatMapFullComboPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance

    fun getScoreFullComboPP(score: LazerScore): RosuPerformance

    fun getBeatMapPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance

    fun getScorePP(score: LazerScore): RosuPerformance

    fun getExpectedPP(beatMap: BeatMap, expected: MapStatisticsService.Expected): RosuPerformance

    fun getScoreStatistics(score: LazerScore): Map<String, Double>

    fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>)

    fun applyStarToBeatMap(beatMap: BeatMap?, expected: MapStatisticsService.Expected)

    fun applyStarToScore(score: LazerScore)

    fun applyStarToScores(scores: List<LazerScore>)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: List<LazerScore>)
}