package com.now.nowbot.service.repairServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.TestService
import com.now.nowbot.util.JacksonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.PreparedStatement

@Service
class FixLazerAccuracyService(
    private val repository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
    private val jdbcTemplate: JdbcTemplate
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        val fix = "!" + "fl"

        if (messageText.contains(fix) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replace(fix, "")
            return true
        }

        return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        val ranges = listOf(
            (0L to 5900000000L),
            (5900000001L to 6300000000L),
            (6300000001L to 6600000000L),
            (6600000001L to 6700000000L),
        )

        runBlocking {
            val jobs = ranges.mapIndexed { index, (rangeStart, rangeEnd) ->

                // 开启并发任务
                launch(Dispatchers.IO) {
                    log.info("[$index] start: $rangeStart -> $rangeEnd")

                    var totalFix = 0L
                    var totalFail = 0L
                    var current = rangeStart // 记录物理 ID 指针
                    var batch = 0L

                    // 只要当前的指针还没触达区间终点，就继续
                    while (current < rangeEnd) {
                        val (next, s, f) = fixBorderRank(index, current, rangeEnd)

                        if (s + f == 0 || next <= current) {
                            break
                        }

                        log.info("[$index] success: $s, fail: $f, now: $current")

                        current = next
                        totalFix += s
                        totalFail += f
                        batch++
                    }

                    log.info("[$index] all done, success: $totalFix, fail $totalFail")
                }
            }

            jobs.joinAll()
        }
        log.info("所有并发修复任务已完成")
        return null

    }

    private fun fixBorderRank(index: Int, start: Long, end: Long): Triple<Long, Int, Int> {
        val lites = repository.findByIDRange(start, end)
        if (start > end) return Triple(end, 0, 0)
        if (lites.isEmpty()) return Triple(start + 1000, 0, 0)

        val success = mutableListOf<Long>()
        val failed = mutableListOf<Long>()

        val stats = statisticRepository.getStatistics(lites.map { it.id }, -1)
            .associate { it.id to JacksonUtil.parseObject<LazerStatistics>(it.data)!! }

        val beatmaps = lites.map { it.rankByte to it.beatmapId }
            .groupBy({ it.first }, { it.second })

        val maxStats: Map<Long, LazerStatistics> = beatmaps.flatMap { (rank, bids) ->
            statisticRepository.getStatistics(bids, rank).map { entity ->

                val stat = JacksonUtil.parseObject<LazerStatistics>(entity.data)!!

                entity.id to stat
            }
        }.toMap()

        val pair = lites.mapNotNull { lite ->

            val stat = stats[lite.id] ?: run {
                log.info("[$index] ${lite.id} failed: great not found")
                return@mapNotNull null
            }

            val max = maxStats[lite.beatmapId] ?: run {
                log.info("[$index] ${lite.beatmapId} failed: max not found")
                return@mapNotNull null
            }

            val fixedAcc = getFixedAccuracy(lite.mode.toByte(), stat, max, lite.legacyScoreId == 0L)

            if (fixedAcc != null && fixedAcc >= 0f) {
                success.add(lite.id)
                return@mapNotNull lite.id to fixedAcc
            } else if (fixedAcc != null) {
                failed.add(lite.id)
                return@mapNotNull null
            } else {
                return@mapNotNull null
            }
        }

        batchUpdate(pair)

        return Triple(lites.maxOfOrNull { it.id } ?: end, success.size, failed.size)
    }

    private fun batchUpdate(updates: List<Pair<Long, Float>>) {
        val sql = "UPDATE lazer_score_lite SET accuracy = ? WHERE id = ?"
        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val (id, acc) = updates[i]
                ps.setFloat(1, acc)
                ps.setLong(2, id)
            }
            override fun getBatchSize() = updates.size
        })
    }

    fun getFixedAccuracy(
        modeByte: Byte,
        stat: LazerStatistics,
        max: LazerStatistics,
        isLazer: Boolean,
    ): Float? {
        if (!isLazer) return null

        return when(modeByte) {
            0.toByte() -> {
                if (max.largeTickHit == 0) return -1f

                val acc = stat.great * 300 + stat.ok * 100 + stat.meh * 50 + stat.sliderTailHit * 150 + stat.largeTickHit * 30
                val total = (max.great) * 300 + max.sliderTailHit * 150 + max.largeTickHit * 30

                (acc / total).toFloat()
            }

            else -> null
        }
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}