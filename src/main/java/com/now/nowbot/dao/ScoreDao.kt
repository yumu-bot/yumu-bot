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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class ScoreDao(
    private val beatmapDao: BeatmapDao,
    private val scoreRepository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
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

    fun deleteByMode(mode: OsuMode): Int {
        return if (mode == OsuMode.DEFAULT) {
            beatmapStarRatingCacheRepository.truncateTable()
            0
        } else {
            beatmapStarRatingCacheRepository.deleteByMode(mode.modeValue)
        }
    }

    /**
     * 提前确保这里是 valueMod
     */
    fun getStarRatingCache(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>): Float? {
        val modsValue: Int = LazerMod.getModsValue(mods)
        return beatmapStarRatingCacheRepository.getStarRating(beatmapID, mode.modeValue, modsValue)
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
        val ss = scores.map { it.scoreID }

        val exists = ss.chunked(1000) { sss ->
            scoreRepository.exists(sss)
        }.flatten().toSet()

        val notExistsScore = scores.filterNot { exists.contains(it.scoreID) }

        val bs = scores.map { it.beatmapID }
        val existsBeatmap = bs.chunked(1000) { bss ->
            statisticRepository.exists(bss, -1)
        }.flatten().toSet()

        val notExistsBeatmap = scores.filterNot { existsBeatmap.contains(it.beatmapID) || (!it.passed && !it.isLazer) }

        if (notExistsScore.isEmpty() && notExistsBeatmap.isEmpty()) return

        val uniqueSets = notExistsScore.map { it.beatmapset }.sortedBy { it.beatmapsetID }.associateBy { it.beatmapsetID }.values
        val uniqueBeatmaps = notExistsScore.map { it.beatmap }.sortedBy { it.beatmapID }.associateBy { it.beatmapID }.values

        try {
            beatmapDao.saveBeatmaps(uniqueBeatmaps)
        } catch (e: Exception) {
            log.error("批量存储 beatmap 异常", e)
        }

        try {
            beatmapDao.saveBeatmapsets(uniqueSets)
        } catch (e: Exception) {
            log.error("批量存储 beatmapset 异常", e)
        }

        // 3. 准备成绩基础数据和统计数据
        val scoreLites = mutableListOf<LazerScoreLite>()
        val allStatistics = mutableListOf<ScoreStatisticLite>()

        notExistsScore.forEach { score ->
            scoreLites.add(LazerScoreLite(score))
            allStatistics.add(ScoreStatisticLite.createByScore(score))
        }

        notExistsBeatmap.forEach { score ->
            allStatistics.add(ScoreStatisticLite.createByBeatmap(score))
        }

        scoreRepository.saveAll(scoreLites)
        statisticRepository.saveAll(allStatistics)
    }

    fun saveScore(score: LazerScore) {
        if (scoreRepository.exists(score.scoreID)) {
            return
        }

        try {
            beatmapDao.saveBeatmapset(score.beatmapset)
            beatmapDao.saveBeatmap(score.beatmap)
        } catch (e: Exception) {
            log.error("统计成绩中存储 beatmap 异常", e)
        }

        val data = LazerScoreLite(score)
        val statisticList: List<ScoreStatisticLite>
        val statistic = ScoreStatisticLite.createByScore(score)

        statisticList = if (statisticRepository.exists(score.beatmapID) || (!score.isLazer && !score.passed)) {
            listOf(statistic)
        } else {
            listOf(statistic, ScoreStatisticLite.createByBeatmap(score))
        }

        scoreRepository.save(data)
        statisticRepository.saveAll(statisticList)
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

    fun getUserRankedScore(userID: Long, mode: Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScore> {
        return scoreRepository.getUserRankedScore(userID, mode, start, end).applyStatistics()
    }

    fun getUsersRankedScore(userIDs: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScore> {
        return scoreRepository.getUsersRankedScore(userIDs, mode, start, end).applyStatistics()
    }

    fun getBeatmapStatistics(beatmapIDs: Collection<Long>): Map<Long, LazerStatistics> {
        return statisticRepository
            .getStatistics(beatmapIDs, -1)
            .distinctBy { it.id }
            .associate { it.id to JacksonUtil.parseObject(it.data)!! }
    }

    /**
     * 还要自己取
     */
    fun getBeatmapScores(user: OsuUser, beatmap: Beatmap, mode: OsuMode): List<LazerScore> {
        return scoreRepository.getBeatmapScores(user.userID, beatmap.beatmapID, mode.modeValue).applyStatistics()
    }

    fun getBeatmapScores(userIDs: Collection<Long>, beatmapID: Long, mode: OsuMode): List<LazerScore> {
        return userIDs.chunked(500).flatMap { chunkedIDs ->
            scoreRepository.getUsersBestScore(chunkedIDs, beatmapID, mode.modeValue)
        }.applyStatistics()
    }

    fun getYesterdayCount(userID: Long, mode: OsuMode): Long {
        val time = LocalDate.now().minusDays(1)

        return scoreRepository.getCountBetween(userID, mode.modeValue, LocalDateTime.of(time, LocalTime.MIN), LocalDateTime.of(time, LocalTime.MAX))
    }

    fun getScoresFromIDs(scoreIDs: Collection<Long>): List<LazerScore> {
        return scoreRepository.getScoresFromIDs(scoreIDs).applyStatistics()
    }

    fun Collection<LazerScoreLite>.applyStatistics(): List<LazerScore> {
        if (this.isEmpty()) return emptyList()

        val mode = this.firstOrNull()?.mode?.toByte() ?: 0.toByte()

        return this.chunked(1000) { ss ->
            val ids = ss.map { it.id }
            val bs = ss.map { it.beatmapId }.distinct()

            val tm = statisticRepository.getStatistics(ids, -1)
                .associateBy { it.id }

            val bm = statisticRepository.getStatistics(bs, mode)
                .associateBy { it.id }

            ss.map { s ->
                s.toLazerScore().apply {
                    val t = tm[s.id]
                    val b = bm[s.beatmapId]

                    t?.setStatus(this)

                    if (b != null) {
                        b.setStatus(this)
                    } else if (t != null) {
                        this.maximumStatistics = this.statistics.constructMaxStatistics(OsuMode.getMode(s.mode))
                    }
                }
            }
        }.flatten()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ScoreDao::class.java)
    }


}