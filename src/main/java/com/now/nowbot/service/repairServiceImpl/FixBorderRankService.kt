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
class FixBorderRankService(
    private val repository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
    private val jdbcTemplate: JdbcTemplate
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        val fix = "!" + "fb"

        if (messageText.contains(fix) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replace(fix, "")
            return true
        } else {
            return false
        }

        // return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        val ranges = listOf(
            (2000L to 1600000000L),
            (1600000001L to 3200000000L),
            (3200000001L to 4800000000L),
            (4800000001L to 6600000000L),
        )

        runBlocking {
            val jobs = ranges.mapIndexed { index, (rangeStart, rangeEnd) ->

                // 开启并发任务
                launch(Dispatchers.IO) {
                    log.info("[$index] start: $rangeStart -> $rangeEnd")

                    var totalFix = 0L
                    var totalSkip = 0L
                    var current = rangeStart // 记录物理 ID 指针
                    var batch = 0L

                    // 只要当前的指针还没触达区间终点，就继续
                    while (current < rangeEnd) {
                        val (next, f, k) = fixBorderRank(index, current, rangeEnd)

                        if (f + k == 0 || next <= current) {
                            break
                        }

                        log.info("[$index] success: $f, skip: $k, now: $current")

                        current = next
                        totalFix += f
                        totalSkip += k
                        batch++
                    }

                    log.info("[$index] all done, success: $totalFix, skip $totalSkip")
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
        if (lites.isEmpty()) return Triple(start + 1000, 0, 1000)

        val success = mutableListOf<Long>()
        val skip = mutableListOf<Long>()

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

            val max = maxStats[lite.id]

            val great = stat.great
            val misses = stat.miss

            val total = when(lite.rankByte) {
                1.toByte() -> max?.great ?: (stat.miss + stat.ok + stat.great)

                else -> max?.great ?: (stat.miss + stat.meh + stat.ok + stat.great)
            }

            val fixedRank = getFixedRankByte(lite.mode.toByte(), lite.rankByte, great, misses, total)

            if (fixedRank != null) {
                success.add(lite.id)
                return@mapNotNull lite.id to fixedRank
            } else {
                skip.add(lite.id)
                return@mapNotNull null
            }
        }

        batchUpdate(pair)

        return Triple(lites.maxOfOrNull { it.id } ?: end, success.size, skip.size)
    }

    private fun batchUpdate(updates: List<Pair<Long, Byte>>) {
        val sql = "UPDATE lazer_score_lite SET rank_byte = ? WHERE id = ?"
        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val (id, acc) = updates[i]
                ps.setByte(1, acc)
                ps.setLong(2, id)
            }
            override fun getBatchSize() = updates.size
        })
    }

    /**
     * 使用纯整数逻辑判断是否需要降级
     */
    fun getFixedRankByte(
        modeByte: Byte,
        rankByte: Byte,
        great: Int,
        misses: Int,
        total: Int
    ): Byte? {
        if (modeByte != 0.toByte() && modeByte != 1.toByte()) return null
        if (total == 0) return null

        val hasMiss = misses > 0
        val greatP10 = great * 10

        // 只有在没有 Miss 的情况下，S, A, B 才会受 0.9, 0.8, 0.7 的影响
        if (!hasMiss) {
            if (greatP10 == total * 9 && rankByte in 5.toByte()..6.toByte()) return 4
            if (greatP10 == total * 8 && rankByte == 4.toByte()) return 3
            if (greatP10 == total * 7 && rankByte == 3.toByte()) return 2
        }

        // 无论有没有 Miss，p300 = 0.6 的 C 都会掉到 D
        if (greatP10 == total * 6 && rankByte == 2.toByte()) return 1

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}