package com.now.nowbot.dao

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import com.now.nowbot.mapper.BeatmapStarRatingCacheRepository
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.isValueMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.model.osu.OsuUser
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
    private val beatmapDao: BeatmapDao,
    private val scoreRepository: LazerScoreRepository,
    private val scoreStatisticRepository: LazerScoreStatisticRepository,
    private val beatmapStarRatingCacheRepository: BeatmapStarRatingCacheRepository
) {

    /**
     * 使用外界的星数
     */
    fun saveStarRatingCacheAsync(score: LazerScore, star: Float) {
        Thread.startVirtualThread {
            saveStarRatingCache(score.beatmapID, score.mode, score.mods, star, score.beatmap.hasLeaderBoard)
        }
    }

    fun saveStarRatingCacheAsync(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>, star: Float, hasLeaderBoard: Boolean = false) {
        Thread.startVirtualThread {
            saveStarRatingCache(beatmapID, mode, mods, star, hasLeaderBoard)
        }
    }

    fun saveStarRatingCache(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>, star: Float, hasLeaderBoard: Boolean = false) {
        // 只存储谱面已上架、星数正常计算、所有模组都是老模组时的星数
        if (!hasLeaderBoard || !mods.isValueMod() || star <= 0.1f) {
            return
        }

        try {
            beatmapStarRatingCacheRepository.saveAndUpdate(beatmapID, mode.modeValue, LazerMod.getModsValue(mods), star)
        } catch (e: Exception) {
            log.error("保存星级缓存失败", e)
        }
    }

    fun deleteByMode(mode: OsuMode): Long {
        return if (mode == OsuMode.DEFAULT) {
            beatmapStarRatingCacheRepository.truncateTable()
            0L
        } else {
            beatmapStarRatingCacheRepository.deleteByMode(mode.modeValue)
        }
    }

    fun getStarRatingCache(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>): Float? {

        val modsValue: Int = if (mods.isValueMod()) {
            LazerMod.getModsValue(mods)
        } else {
            0
        }

        return beatmapStarRatingCacheRepository.findByKey(beatmapID, mode.modeValue, modsValue)
    }


    // 不可以在 @Transactional 内使用虚拟线程
    // 可能会导致事务失效、数据库连接泄露等疑难杂症
    // @Transactional
    fun saveScoreAsync(scores: List<LazerScore>) {
        Thread.startVirtualThread {
            try {
                saveScores(scores)
            } catch (e: Throwable) {
                log.error("成绩数据访问对象层：保存成绩时发生错误：", e)
            }
        }
    }

    @Transactional
    fun saveScores(scores: List<LazerScore>) {
        if (scores.isEmpty()) {
            return
        }
        if (scores.size == 1) {
            saveScore(scores.first())
            return
        }
        val mode = scores.first().mode
        saveScores(scores, mode)
    }

    fun saveScore(score: LazerScore) {
        if (scoreRepository.ifScoreExists(score.scoreID) != null) {
            return
        }

        try {
            beatmapDao.saveBeatmapset(score.beatmapset)
            beatmapDao.saveBeatmap(score.beatmap)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }

        if (score.maximumStatistics.great == 0 && score.mode != OsuMode.MANIA) {
            log.warn("Score ${score.scoreID} 缺少统计数据，Rank 计算可能不准")
        }

        val data = LazerScoreLite(score)
        val statisticList: List<ScoreStatisticLite>
        val statistic = ScoreStatisticLite.createByScore(score)
        statisticList = if (scoreStatisticRepository.ifStatisticExists(score.beatmapID) != null) {
            listOf(statistic, ScoreStatisticLite.createByBeatmap(score))
        } else {
            listOf(statistic)
        }
        scoreRepository.save(data)
        scoreStatisticRepository.saveAll(statisticList)
    }

    private fun saveScores(scores: List<LazerScore>, mode: OsuMode) {
        if (scores.isEmpty()) return

        val scoreIdList = scores.map { it.scoreID }.toSet()
        val alreadySaveScoreId = scoreRepository.getSavedScoreIDs(scoreIdList)

        val filteredScoreList = scores.filterNot { alreadySaveScoreId.contains(it.scoreID) }
        if (filteredScoreList.isEmpty()) return

        try {
            val set = filteredScoreList.map { it.beatmapset }
            beatmapDao.saveBeatmapsets(set)
            val map = filteredScoreList.map { it.beatmap }
            beatmapDao.saveBeatmaps(map)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }

        val (scoreLiteList, scoreStatisticList) = filteredScoreList
            .map { LazerScoreLite(it) to ScoreStatisticLite.createByScore(it) }
            .unzip()

        val beatmapIdList = filteredScoreList.map { it.beatmapID }
        val alreadySaveBeatmapId = scoreStatisticRepository.getSavedBeatmapIDs(beatmapIdList, mode.modeValue.toInt())

        val mapStatisticList = filteredScoreList
            .filterNot { alreadySaveBeatmapId.contains(it.beatmapID) }
            .map { ScoreStatisticLite.createByBeatmap(it) }

        scoreStatisticRepository.saveAll(mapStatisticList)
        scoreStatisticRepository.saveAll(scoreStatisticList)
        scoreRepository.saveAll(scoreLiteList)
    }

    fun getUserAllScoreTime(userID: Long): List<OffsetDateTime> {
        val start = ZonedDateTime
            .of(LocalDateTime.of(2025, 1, 1, 0, 0), ZoneOffset.systemDefault())
            .toOffsetDateTime()
        val end = ZonedDateTime
            .now(ZoneOffset.systemDefault())
            .toOffsetDateTime()
        return scoreRepository.getUserAllScoreTime(userID, start, end, PageRequest.ofSize(500))
    }

    fun getUserRankedScore(userID: Long, mode: Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite> {
        return scoreRepository.getUserRankedScore(userID, mode, start, end)
    }

    fun getUsersRankedScore(userIDs: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite> {
        return scoreRepository.getUsersRankedScore(userIDs, mode, start, end)
    }

    fun getStatisticsMap(scores: Iterable<LazerScoreLite>): Map<Long, LazerStatistics> {
        return scoreStatisticRepository
            .getByScoreIDWhenGraveyard(scores.map { it.id })
            .associate { it.id to JacksonUtil.parseObject(it.data, LazerStatistics::class.java) }
    }

    /**
     * 还要自己取
     */
    fun getBeatmapScores(user: OsuUser, beatmap: Beatmap, mode: OsuMode): List<LazerScore> {
        return scoreRepository.getBeatmapScores(user.userID, beatmap.beatmapID, mode.modeValue).map { it.toLazerScore() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ScoreDao::class.java)
    }


}