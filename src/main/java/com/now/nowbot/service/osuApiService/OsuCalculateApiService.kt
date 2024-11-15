package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService
import rosu.result.JniResult

interface OsuCalculateApiService {
    fun getBeatMapPerfectPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult

    fun getScorePerfectPP(score: LazerScore): JniResult

    fun getBeatMapFullComboPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult

    fun getScoreFullComboPP(score: LazerScore): JniResult

    fun getBeatMapPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult

    fun getScorePP(score: LazerScore): JniResult

    fun getExpectedPP(beatMap: BeatMap, expected: MapStatisticsService.Expected): JniResult

    fun getScoreStatistics(score: LazerScore): Map<String, Double>

    fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>)

    fun applyStarToBeatMap(beatMap: BeatMap?, expected: MapStatisticsService.Expected)

    fun applyStarToScore(score: LazerScore)

    fun applyStarToScores(scores: List<LazerScore>)

    fun applyPPToScore(score: LazerScore)

    fun applyPPToScores(scores: List<LazerScore>)
}