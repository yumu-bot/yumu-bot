package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.CATCH
import com.now.nowbot.model.enums.OsuMode.CATCH_RELAX
import com.now.nowbot.model.enums.OsuMode.MANIA
import com.now.nowbot.model.enums.OsuMode.OSU
import com.now.nowbot.model.enums.OsuMode.OSU_AUTOPILOT
import com.now.nowbot.model.enums.OsuMode.OSU_RELAX
import com.now.nowbot.model.enums.OsuMode.TAIKO
import com.now.nowbot.model.enums.OsuMode.TAIKO_RELAX
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.JacksonUtil
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement

@Service("TEST")
class TestService(
    private val repository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
    private val jdbcTemplate: JdbcTemplate
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText.contains("!yuumu") && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replace("!yuumu", "")
            return true
        } else {
            return false
        }

        // return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
//        val score = repository.getBeatmapScores(33804348, 3477045, 3)
//
//        val ls = score.chunked(1000) { ss ->
//            val ids = ss.map { it.id }
//            val bs = ss.map { it.beatmapId }.distinct()
//
//            val tm = statisticRepository.getStatistics(ids, -1)
//                .associateBy { it.id }
//
//            val bm = statisticRepository.getStatistics(bs, 3)
//                .associateBy { it.id }
//
//            ss.map { s ->
//                s.toLazerScore().apply {
//                    val t = tm[s.id]
//                    val b = bm[s.beatmapId]
//
//                    t?.setStatus(this)
//
//                    if (b != null) {
//                        b.setStatus(this)
//                    } else if (t != null) {
//                        this.maximumStatistics = this.statistics.constructMaxStatistics(OsuMode.getMode(s.mode))
//                    }
//                }
//            }
//        }.flatten()
//
//        val s = ls.map { it.accuracy }

        @Transactional
        fun fixZeroAccuracyScores(limit: Int, offset: Int): Int {
            val liteScores = repository.findInvalidAccuracyScores(limit, offset)
            if (liteScores.isEmpty()) return 0

            val success = mutableListOf<Long>()
            val failed = mutableListOf<Long>()

            val pair = liteScores.mapNotNull { lite ->
                val stat = statisticRepository.getStatistics(listOf(lite.id), -1)
                    .firstOrNull()?.let { JacksonUtil.parseObject<LazerStatistics>(it.data) }
                    ?: run {
                        log.info("${lite.id} failed")
                        failed.add(lite.id)
                        return@mapNotNull  null
                    }

                val fixedAcc = calc(stat, lite.legacyScore == 0, OsuMode.getMode(lite.mode)) ?: run {
                    failed.add(lite.id)
                    log.info("${lite.id} failed: calc returned null")
                    return@mapNotNull null
                }

                // 修改点 1：如果是合法的准确率 (包含 0.0)，才去更新。如果你不想更新为 0 的数据，请把它加入 failed
                if (fixedAcc >= 0.0 && fixedAcc <= 1.0 && !fixedAcc.isNaN()) {
                    success.add(lite.id)
                    // log.info("${lite.id} succeed with acc: $fixedAcc")

                    //repository.updateAccuracy(lite.id, fixedAcc.toFloat())
                    return@mapNotNull lite.id to fixedAcc.toFloat()
                } else {
                    // 修改点 2：堵住逻辑漏洞，捕捉异常值
                    failed.add(lite.id)
                    log.info("${lite.id} failed: invalid acc calculated ($fixedAcc): $stat, ${OsuMode.getMode(lite.mode)}")
                    return@mapNotNull null
                }
            }

            batchUpdate(pair)

            log.info("fix success: ${success.size}, failed: ${failed.joinToString(",")}")

            // 修改点 3：一定要返回真正被修改的条数，而不是查询出来的条数！
            return success.size
        }

        var batch = 0
        val maxBatches = 3000 // 安全阈值，防止死循环
        var all = 0
        var offset = 0

        runBlocking {
            do {
                val result = fixZeroAccuracyScores(800, offset)

                offset += 800 - result

                batch++
                all += result

                log.info("Batch ${batch}: Fixing $result, Fixed $all ")

                // 如果某次修复数量为 0，说明数据库里已经没有 accuracy <= 0 的数据了，提前退出
                if (result == 0) {
                    log.info("Optimization complete: No more invalid scores found. total failed: $offset")
                    break
                }

                //delay(500.milliseconds)
            } while (batch < maxBatches)

            if (batch >= maxBatches) {
                log.warn("Reached maximum batch limit ($maxBatches). Some scores may still be unfixed.")
            }
        }

        return null


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

    private fun calc(stat: LazerStatistics, isLazer: Boolean = false, mode: OsuMode): Double? {

        return when (mode) {
            OSU, OSU_RELAX, OSU_AUTOPILOT -> {
                val hit = stat.great + 1.0 / 3 * stat.ok + 1.0 / 6 * stat.meh
                val total = stat.great + stat.ok + stat.meh + stat.miss

                1.0 * hit / total.coerceAtLeast(1)
            }

            TAIKO, TAIKO_RELAX -> {
                val hit = (stat.great + 1.0 / 2 * stat.ok)
                val total = stat.great + stat.ok + stat.miss

                1.0 * hit / total.coerceAtLeast(1)
            }

            CATCH, CATCH_RELAX -> {
                val hit = stat.great + stat.largeTickHit + stat.smallTickHit
                val total = stat.great + stat.largeTickHit + stat.smallTickHit + stat.largeTickMiss + stat.smallTickMiss + stat.miss

                1.0 * hit / total.coerceAtLeast(1)
            }

            MANIA -> {
                val hit = if (isLazer) {
                    stat.perfect + 300.0 / 305.0 * stat.great + 200.0 / 305.0 * stat.good + 100.0 / 305.0 * stat.ok + 50.0 / 305.0 * stat.meh
                } else {
                    stat.perfect + stat.great + 2.0 / 3 * stat.good + 1.0 / 3 * stat.ok + 1.0 / 6 * stat.meh
                }
                val total = stat.perfect + stat.great + stat.good + stat.ok + stat.meh + stat.miss
                1.0 * hit / total.coerceAtLeast(1)
            }

            else -> 0.0
        }
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
