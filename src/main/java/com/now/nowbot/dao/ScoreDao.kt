package com.now.nowbot.dao

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class ScoreDao(
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
        val data = LazerScoreLite(score)
        val statisticList: List<ScoreStatisticLite>
        val statistic = ScoreStatisticLite.createByScore(score)
        if (scoreStatisticRepository.checkIdExists(score.beatMapID).isEmpty) {
            statisticList = listOf(statistic, ScoreStatisticLite.createByBeatmap(score))
        } else {
            statisticList = listOf(statistic)
        }
        scoreRepository.save(data)
        scoreStatisticRepository.saveAll(statisticList)
    }

    private fun saveScore(scoreList: List<LazerScore>, mode: OsuMode) {
        if (scoreList.isEmpty()) return
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

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ScoreDao::class.java)
    }
}