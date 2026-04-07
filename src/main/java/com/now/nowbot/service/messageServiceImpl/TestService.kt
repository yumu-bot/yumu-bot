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
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.util.JacksonUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Duration.Companion.seconds

@Service("TEST")
class TestService(
    private val repository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
    private val beatmapApiService: OsuBeatmapApiService
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
        fun fixZeroAccuracyScores(offset: Int = 0): Int {
            val liteScores = repository.findInvalidAccuracyScores(100, offset)
            if (liteScores.isEmpty()) return 0

            val success = mutableListOf<Long>()
            val failed = mutableListOf<Long>()

            val modes = beatmapApiService.getBeatmaps(liteScores.map { it.beatmapId }).associate { it.beatmapID to it.mode.modeValue }

            liteScores.forEach { lite ->
                val stat = statisticRepository.getStatistics(listOf(lite.id), -1)
                    .firstOrNull()?.let { JacksonUtil.parseObject<LazerStatistics>(it.data) }
                    ?: run {
                        log.info("${lite.id} failed")
                        failed.add(lite.id)
                        return@forEach
                    }


                val maxStat = if (lite.rankByte > 0) {
                    stat.constructMaxStatistics(OsuMode.getMode(lite.mode))
                } else {
                    statisticRepository.getStatistics(listOf(lite.beatmapId), modes[lite.beatmapId] ?: run {
                        log.info("${lite.beatmapId} failed")
                        failed.add(lite.beatmapId)
                        return@forEach
                    })
                        .firstOrNull()?.let { JacksonUtil.parseObject<LazerStatistics>(it.data) }
                        ?: run {
                            log.info("${lite.beatmapId} failed")
                            failed.add(lite.beatmapId)
                            return@forEach
                        }
                }

                val fixedAcc = calc(stat, maxStat, lite.legacyScore == 0, OsuMode.getMode(lite.mode)) ?: run {
                    failed.add(lite.id)
                    log.info("${lite.id} failed: calc returned null")
                    return@forEach
                }

                // 修改点 1：如果是合法的准确率 (包含 0.0)，才去更新。如果你不想更新为 0 的数据，请把它加入 failed
                if (fixedAcc >= 0.0 && fixedAcc <= 1.0 && !fixedAcc.isNaN()) {
                    success.add(lite.id)
                    log.info("${lite.id} succeed with acc: $fixedAcc")
                    repository.updateAccuracy(lite.id, fixedAcc.toFloat())
                } else {
                    // 修改点 2：堵住逻辑漏洞，捕捉异常值
                    failed.add(lite.id)
                    log.info("${lite.id} failed: invalid acc calculated ($fixedAcc): $stat, $maxStat, ${OsuMode.getMode(lite.mode)}")
                }
            }

            log.info("fix success: ${success.joinToString(",")}, failed: ${failed.joinToString(",")}")

            // 修改点 3：一定要返回真正被修改的条数，而不是查询出来的条数！
            return success.size
        }

        var current: Int
        var batch = 0
        val maxBatches = 3000 // 安全阈值，防止死循环

        runBlocking {
            do {
                // 确保 fixZeroAccuracyScores 返回的是本次成功修复并保存的条数
                current = fixZeroAccuracyScores(batch * 100)
                batch++

                log.info("Batch {}: Fixed {} scores", batch, current)

                // 如果某次修复数量为 0，说明数据库里已经没有 accuracy <= 0 的数据了，提前退出
                if (current == 0) {
                    log.info("Optimization complete: No more invalid scores found.")
                    break
                }

                delay(3.seconds)
            } while (batch < maxBatches)

            if (batch >= maxBatches) {
                log.warn("Reached maximum batch limit ($maxBatches). Some scores may still be unfixed.")
            }
        }

        return null


    }



    private fun calc(stat: LazerStatistics, maxStat: LazerStatistics, isLazer: Boolean = false, mode: OsuMode): Double? {
        val s = stat
        val m = maxStat

        var total = m.great

        if (m.great == 0 && m.perfect == 0) return null

        return when (mode) {
            OSU, OSU_RELAX, OSU_AUTOPILOT -> {
                val hit = s.great + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                hit / total
            }

            TAIKO, TAIKO_RELAX -> {
                (s.great + 1.0 / 2 * s.ok) / total
            }

            CATCH, CATCH_RELAX -> {
                val hit = s.great + s.largeTickHit + s.smallTickHit
                total = m.great + m.largeTickHit + m.smallTickHit

                1.0 * hit / total
            }

            MANIA -> {
                val hit = if (isLazer) {
                    s.perfect + 300.0 / 305.0 * s.great + 200.0 / 305.0 * s.good + 100.0 / 305.0 * s.ok + 50.0 / 305.0 * s.meh
                } else {
                    s.perfect + s.great + 2.0 / 3 * s.good + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                }
                total = m.perfect
                hit / total
            }

            else -> 0.0
        }
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
