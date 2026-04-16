package com.now.nowbot.service.repairServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.containsHidden
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.TestService
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.PreparedStatement

@Service
class FixAllRankService(
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
        }

        return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        val ranges = listOf(
            (0L to 5900000000L),
            (5900000001L to 6200000000L),
            (6200000001L to 6400000000L),
            (6400000001L to 6600000000L),
        )

        var totalFix = 0L
        var totalSkip = 0L
        var current = 0L // 记录物理 ID 指针
        var batch = 0L

        try {
            log.info("[0] 任务启动")
            log.info("[0] start: 0L -> 6600000000L")

            // 只要当前的指针还没触达区间终点，就继续
            while (current < 6600000000L) {
                log.info("[0] 尝试处理起点: $current")
                val (next, f, k) = fixAllRank(0, current, 6600000000L)

                log.info("[0] success: $f, skip: $k, now: $current")

                if (next <= current) {
                    break
                }

                current = next
                totalFix += f
                totalSkip += k
                batch++
            }
            log.info("[0] 循环正常结束")
        } catch (e: Throwable) {
            log.error("[0] 致命错误!!", e)
        } finally {
            log.info("[0] 协程最终生命周期结束")
        }

        return null
    }

    private fun fixAllRank(index: Int, start: Long, end: Long): Triple<Long, Int, Int> {
        val lites = repository.findByIDRange(start, end)
        if (start > end) return Triple(end, 0, 0)
        if (lites.isEmpty()) return Triple(end, 0, 0)

        val success = mutableListOf<Long>()
        val skip = mutableListOf<Long>()

        val stats = statisticRepository.getStatistics(lites.map { it.id }, -1)
            .associate { it.id to JacksonUtil.parseObject<LazerStatistics>(it.data)!! }

        val pair = lites.mapNotNull { lite ->

            val stat = stats[lite.id] ?: run {
                log.info("[$index] ${lite.id} failed: stats not found")
                return@mapNotNull null
            }

            fun rankToByte(rank: String): Byte {
                return when(rank) {
                    "F" -> 0
                    "D" -> 1
                    "C" -> 2
                    "B" -> 3
                    "A" -> 4
                    "S" -> 5
                    "SH" -> 6
                    "X", "SS" -> 7
                    "XH", "SSH" -> 8
                    else -> 0
                }
            }

            val calculatedRank =
                rankToByte(getStandardisedRank(stat, lite.passed,  lite.mode.toInt(), lite.legacyScoreId == 0L,
                    JacksonUtil.parseObjectList(lite.mods, LazerMod::class.java)))

            if (calculatedRank != lite.rankByte) {
                success.add(lite.id)
                return@mapNotNull lite.id to calculatedRank
            } else {
                skip.add(lite.id)
                return@mapNotNull null
            }
        }

        if (pair.isNotEmpty()) {
            batchUpdate(pair)

            val maxId = lites.maxOf { it.id }
            return Triple(maxId, success.size, skip.size)
        } else {
            return Triple(end, success.size, 1000)
        }

    }

    private fun batchUpdate(updates: List<Pair<Long, Byte>>) {
        val sql = "UPDATE lazer_score_lite SET rank_byte = ? WHERE id = ?"
        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val (id, rank) = updates[i]
                ps.setByte(1, rank)
                ps.setLong(2, id)
            }
            override fun getBatchSize() = updates.size
        })
    }

    private fun getStandardisedRank(s: LazerStatistics, passed: Boolean, mode: Int, isLazer: Boolean = false, mods: List<LazerMod>): String {
        if (!passed) {
            return "F"
        }

        /*
        if (score.isLazer) {
            return score.lazerRank
        }

         */

        val total = when(mode) {
            1 -> s.great + s.ok + s.miss
            2 -> s.great + s.largeTickHit + s.smallTickHit + s.largeTickMiss + s.smallTickMiss + s.miss
            3 -> s.perfect + s.great + s.good + s.ok + s.meh + s.miss
            else -> s.great + s.ok + s.meh + s.miss
        }

        val hasMiss = s.miss > 0

        val rank = if (isLazer) {
            // lazer
            when (mode) {
                2 -> {
                    val hit = s.great + s.largeTickHit + s.smallTickHit

                    when {
                        hit == total -> "X"
                        hit * 100 > total * 98 -> "S"
                        hit * 100 > total * 94 -> "A"
                        hit * 100 > total * 90 -> "B"
                        hit * 100 > total * 85 -> "C"
                        else -> "D"
                    }
                }

                3 -> {
                    val judgement = s.perfect * 305 + s.great * 300 + s.good * 200 + s.ok * 100 + s.meh * 50 + 0
                    val max = total * 305

                    when {
                        s.perfect + s.great == total -> "X"
                        judgement * 100 > max * 95 -> "S"
                        judgement * 100 > max * 90 -> "A"
                        judgement * 100 > max * 80 -> "B"
                        judgement * 100 > max * 70 -> "C"
                        else -> "D"
                    }
                }

                else -> {
                    val judgement = if (mode == 1) {
                        s.great * 300 + s.ok * 150 + 0
                    } else {
                        s.great * 300 + s.ok * 100 + s.meh * 50 + 0
                    }

                    val max = total * 300

                    when {
                        judgement == max -> "X"
                        judgement * 100 > max * 95 -> if (hasMiss) "A" else "S"

                        judgement * 100 > max * 90 -> "A"
                        judgement * 100 > max * 80 -> "B"
                        judgement * 100 > max * 70 -> "C"
                        else -> "D"
                    }
                }
            }
        } else {
            // stable
            when (mode) {

                1 -> when {
                    s.great == total -> "X"
                    s.great * 10 > total * 9 -> if (hasMiss) "A" else "S"
                    s.great * 10 > total * 8 -> if (hasMiss) "B" else "A"
                    s.great * 10 > total * 7 -> if (hasMiss) "C" else "B"
                    s.great * 10 > total * 6 -> "C"
                    else -> "D"
                }

                2 -> {
                    val hit = s.great + s.largeTickHit + s.smallTickHit

                    when {
                        hit == total -> "X"
                        hit * 100 > total * 98 -> "S"
                        hit * 100 > total * 94 -> "A"
                        hit * 100 > total * 90 -> "B"
                        hit * 100 > total * 85 -> "C"
                        else -> "D"
                    }
                }

                3 -> {
                    val judgement = s.perfect * 300 + s.great * 300 + s.good * 200 + s.ok * 100 + s.meh * 50 + 0

                    when {
                        judgement == total * 300 -> "X"
                        judgement * 100 > total * 300 * 95 -> "S"
                        judgement * 100 > total * 300 * 90 -> "A"
                        judgement * 100 > total * 300 * 80 -> "B"
                        judgement * 100 > total * 300 * 70 -> "C"
                        else -> "D"
                    }
                }

                else -> {
                    val is50Over1P = (s.meh * 100 > total)

                    when {
                        s.great == total -> "X"

                        s.great * 10 > total * 9 -> if (hasMiss || is50Over1P) {
                            "A"
                        } else {
                            "S"
                        }

                        s.great * 10 > total * 8 -> if (hasMiss) "B" else "A"
                        s.great * 10 > total * 7 -> if (hasMiss) "C" else "B"
                        s.great * 10 > total * 6 -> "C"
                        else -> "D"
                    }
                }
            }
        }

        if (mods.containsHidden() && (rank == "S" || rank == "X")) return rank + "H"

        return rank
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}