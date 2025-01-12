package com.now.nowbot.dao

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.LazerStatistics
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.util.JacksonUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.*

@Component
class ScoreDao(
    val beatMapDao: BeatMapDao,
    val scoreRepository: LazerScoreRepository,
    val scoreStatisticRepository: LazerScoreStatisticRepository,
) {
    @Transactional
    fun saveScoreAsync(scoreList: List<LazerScore>) {
        Thread.startVirtualThread {
            try {
                saveScore(scoreList)
            } catch (e: Throwable) {
                log.error("save score error", e)
            }
        }
    }

    @Transactional
    fun saveScore(scoreList: List<LazerScore>) {
        if (scoreList.isEmpty()) {
            return
        }
        if (scoreList.size == 1) {
            saveScore(scoreList.first())
            return
        }
        val mode = scoreList.first().mode
        saveScore(scoreList, mode)
    }

    fun saveScore(score: LazerScore) {
        if (scoreRepository.checkIdExists(score.scoreID).isPresent) {
            return
        }
        try {
            beatMapDao.saveMapSet(score.beatMapSet)
            beatMapDao.saveMap(score.beatMap)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }
        val data = LazerScoreLite(score)
        val statisticList: List<ScoreStatisticLite>
        val statistic = ScoreStatisticLite.createByScore(score)
        statisticList = if (scoreStatisticRepository.checkIdExists(score.beatMapID).isEmpty) {
            listOf(statistic, ScoreStatisticLite.createByBeatmap(score))
        } else {
            listOf(statistic)
        }
        scoreRepository.save(data)
        scoreStatisticRepository.saveAll(statisticList)
    }

    private fun saveScore(scoreList: List<LazerScore>, mode: OsuMode) {
        if (scoreList.isEmpty()) return
        try {
            val set = scoreList.map { it.beatMapSet }
            beatMapDao.saveAllMapSet(set)
            val map = scoreList.map { it.beatMap }
            beatMapDao.saveAllMap(map)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }
        val (scoreIdList, beatmapIdList) = scoreList.map { it.scoreID to it.beatMapID }.unzip()
        val alreadySaveScoreId = scoreRepository.getRecordId(scoreIdList)

        val (scoreLiteList, scoreStatisticList) = scoreList
            .filterNot { alreadySaveScoreId.contains(it.scoreID) }
            .map { LazerScoreLite(it) to ScoreStatisticLite.createByScore(it) }
            .unzip()

        if (scoreLiteList.isEmpty()) return

        val alreadySaveBeatmapId = scoreStatisticRepository.getRecordBeatmapId(beatmapIdList, mode.modeValue.toInt())

        val mapStatisticList = scoreList
            .filterNot { alreadySaveBeatmapId.contains(it.beatMapID) }
            .map { ScoreStatisticLite.createByBeatmap(it) }

        scoreStatisticRepository.saveAll(mapStatisticList)
        scoreStatisticRepository.saveAll(scoreStatisticList)
        scoreRepository.saveAll(scoreLiteList)
    }

    fun getUserAllScoreTime(userId: Long): List<OffsetDateTime> {
        val start = ZonedDateTime
            .of(LocalDateTime.of(2025, 1, 1, 0, 0), ZoneOffset.systemDefault())
            .toOffsetDateTime()
        val end = ZonedDateTime
            .now(ZoneOffset.systemDefault())
            .toOffsetDateTime()
        return scoreRepository.getUserAllScoreTime(userId, start, end)
    }

    fun getAllRankedScore(userId: Long, date: LocalDate, beatmapApiService: OsuBeatmapApiService): ScoreDailyStatistic {
        val start = ZonedDateTime
            .of(date, LocalTime.of(0, 0), ZoneOffset.systemDefault())
            .toOffsetDateTime()
        val end = start.plusDays(1)

        val scores = scoreRepository.getUserRankedScore(userId, OsuMode.OSU.modeValue, start, end)
        val statisticsMap = scoreStatisticRepository.getByScoreId(scores.map { it.id }).associate {
            it.id to JacksonUtil.parseObject(it.data, LazerStatistics::class.java)
        }

        val (passScore, failedScore) = scores.partition { it.passed }

        // this.sliderTailHit is tth
        val passStatistics = passScore.toStatistics(statisticsMap)

        val failedStatistics = failedScore.toStatistics(statisticsMap)

        val tth = passStatistics
            .sumOf { it.second.sliderTailHit } + failedStatistics.sumOf { it.second.sliderTailHit }

        val pc = passStatistics.size + failedStatistics.size

        val fileTimeAll =
            beatmapApiService.getAllFailTime(failedStatistics.map { it.first.beatmapId to it.second.sliderTailHit })
        val passTimeMap = beatmapApiService
            .getAllBeatmapHitLength(passStatistics.map { it.first.beatmapId }.toSet())
            .toMap()

        val pt = passScore.sumOf { passTimeMap[it.beatmapId] ?: 0 } + fileTimeAll.sum()
        return ScoreDailyStatistic(userId, date, pc, pt, tth)
    }

    fun List<LazerScoreLite>.toStatistics(statisticsMap: Map<Long, LazerStatistics>): List<Pair<LazerScoreLite, LazerStatistics>> {
        return mapNotNull {
            val stat = statisticsMap[it.id]?.apply { this.sliderTailHit = getTotalHits(OsuMode.OSU) }
            if (stat != null) {
                it to stat
            } else {
                null
            }
        }.filter { it.second.sliderTailHit > 30 }
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ScoreDao::class.java)
    }

    data class ScoreDailyStatistic(
        val userId: Long,
        val date: LocalDate,
        val playCount: Int,
        val playTime: Int,
        val totalHit: Int,
    )
}