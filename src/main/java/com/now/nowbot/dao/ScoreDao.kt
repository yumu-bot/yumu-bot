package com.now.nowbot.dao

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.util.JacksonUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class ScoreDao(
    val beatmapDao: BeatmapDao,
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
            beatmapDao.saveMapSet(score.beatmapset)
            beatmapDao.saveMap(score.beatmap)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }
        val data = LazerScoreLite(score)
        val statisticList: List<ScoreStatisticLite>
        val statistic = ScoreStatisticLite.createByScore(score)
        statisticList = if (scoreStatisticRepository.checkIdExists(score.beatmapID).isEmpty) {
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
            val set = scoreList.map { it.beatmapset }
            beatmapDao.saveAllMapSet(set)
            val map = scoreList.map { it.beatmap }
            beatmapDao.saveAllMap(map)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }
        val (scoreIdList, beatmapIdList) = scoreList.map { it.scoreID to it.beatmapID }.unzip()
        val alreadySaveScoreId = scoreRepository.getRecordId(scoreIdList)

        val (scoreLiteList, scoreStatisticList) = scoreList
            .filterNot { alreadySaveScoreId.contains(it.scoreID) }
            .map { LazerScoreLite(it) to ScoreStatisticLite.createByScore(it) }
            .unzip()

        if (scoreLiteList.isEmpty()) return

        val alreadySaveBeatmapId = scoreStatisticRepository.getRecordBeatmapId(beatmapIdList, mode.modeValue.toInt())

        val mapStatisticList = scoreList
            .filterNot { alreadySaveBeatmapId.contains(it.beatmapID) }
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
        return scoreRepository.getUserAllScoreTime(userId, start, end, PageRequest.ofSize(500))
    }

    fun getUserRankedScore(id: Long, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite> {
        return scoreRepository.getUserRankedScore(id, mode, start, end)
    }

    fun getUsersRankedScore(ids: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite> {
        return scoreRepository.getUsersRankedScore(ids, mode, start, end)
    }

    fun getStatisticsMap(scores: Iterable<LazerScoreLite>): Map<Long, LazerStatistics> {
        return scoreStatisticRepository
            .getByScoreId(scores.map { it.id })
            .associate { it.id to JacksonUtil.parseObject(it.data, LazerStatistics::class.java) }
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ScoreDao::class.java)
    }


}