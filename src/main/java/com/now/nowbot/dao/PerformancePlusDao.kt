package com.now.nowbot.dao

import com.now.nowbot.entity.PerformancePlusStatsLite
import com.now.nowbot.mapper.PerformancePlusLiteRepository
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.calculate.PPPlus
import com.now.nowbot.service.PerformancePlusAPIService
import com.now.nowbot.service.messageServiceImpl.PPPlusService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

@Component
class PerformancePlusDao(
    private val plusRepository: PerformancePlusLiteRepository,
    private val snapshotDao: UserSnapShotDao,
    private val plusAPIService: PerformancePlusAPIService
) {
    /**
     * 高阶方法：获取缓存后再返回
     */
    fun getBeatmapPerformancePlusMax(beatmap: Beatmap, mods: List<LazerMod> = emptyList()): PPPlus? {
        val exists = getBeatmapPerformancePlus(beatmap)

        if (exists != null) {
            return exists
        } else {
            val plus = plusAPIService.getMapPerformancePlus(beatmap.beatmapID, mods)

            if (plus != null) {
                Thread.startVirtualThread {
                    saveBeatmapPerformancePlus(beatmap.beatmapID, plus)
                }

                return plus
            } else {
                return null
            }
        }
    }

    /**
     * 高阶方法：获取缓存后再返回
     */
    fun getUserPerformancePlusMax(bests: Collection<LazerScore>): PPPlus? {
        // 没数据你创个吊差
        val f = bests.firstOrNull() ?: return null

        val before = snapshotDao.getLatestSnapshot(f.userID, f.mode)

        val beforeStats: PPPlus.Stats?

        val hash = snapshotDao.getHash(bests)

        if (hash != before?.contentHash && before != null && bests.size >= before.scoreIDs.size) {
            val saved = before.scoreIDs.toSet().mapNotNull { s ->
                val d = plusRepository.findDetailsByScoreID(s, f.userID)

                d?.let { d.toModel() }
            }.toList()

            beforeStats = PerformancePlusAPIService.collect(saved)
        } else {
            beforeStats = null
        }

        Thread.startVirtualThread {
            snapshotDao.upsertSnapshot(bests)
        }

        val exists = bests.mapNotNull { b ->
            val d = plusRepository.findDetailsByScoreID(b.scoreID, f.userID)

            d?.let { b.scoreID to d.toModel() }
        }.toMap()

        val notExists = bests.filterNot { it.scoreID in exists.keys }

        val locals = plusAPIService.getScoresPerformancePlus(notExists).mapNotNull { it.performance }

        if (locals.size + exists.size < bests.size) {
            log.warn("""
                表现分加数据层：有部分最好成绩遗失：
                输入 ${bests.size} 个，实际 ${locals.size} 个 + ${exists.size} 个。
                这批表现分加不写入数据库。
                """.trimIndent())
        } else {
            Thread.startVirtualThread {
                notExists.zip(locals).forEach { (s, p) ->
                    saveScorePerformancePlus(s, p)
                }
            }
        }

        val performance = PerformancePlusAPIService.collect(exists.values + locals)
        val stats = PPPlusService.calculateUserAdvancedStats(performance)

        val plus = PPPlus().apply {
            this.performance = performance
            this.advancedStats = stats
            beforeStats?.let { this.difficulty = beforeStats }

            before?.let {
                val duration = Duration.between(it.createdAt, LocalDateTime.now())
                this.combo = duration.toHours().toInt()
            }
        }

        return plus
    }

    fun saveScorePerformancePlus(score: LazerScore, performance: PPPlus.Stats?) {
        performance ?: return

        plusRepository.saveAndUpdate(
            PerformancePlusStatsLite.fromScore(score.userID, score.scoreID, score.beatmapID, performance )
        )
    }

    fun saveBeatmapPerformancePlus(beatmapID: Long, plus: PPPlus) {
        val performance = plus.performance ?: return
        val difficulty = plus.difficulty ?: return

        val list = PerformancePlusStatsLite.fromBeatmap(
            beatmapID, performance, difficulty
        )

        list.forEach {
            plusRepository.saveAndUpdate(it)
        }
    }

    fun getBeatmapPerformancePlus(beatmap: Beatmap): PPPlus? {
        return getBeatmapPerformancePlus(beatmap.beatmapID, beatmap.maxCombo)
    }

    fun getBeatmapPerformancePlus(beatmapID: Long, maxCombo: Int? = null): PPPlus? {
        val list = plusRepository.findDetailsByBeatmapID(beatmapID)

        val performance = list.find { it.scoreID == -2L }?.toModel()
            ?: return null
        val difficulty = list.find { it.scoreID == -1L }?.toModel()
            ?: return null

        return PPPlus().apply {
            this.performance = performance
            this.difficulty = difficulty
            this.accuracy = 1.0
            maxCombo?.let { this.combo = it }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PerformancePlusDao::class.java)
    }
}