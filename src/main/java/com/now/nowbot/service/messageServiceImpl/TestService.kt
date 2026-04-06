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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("TEST")
class TestService(
    private val repository: LazerScoreRepository,
    private val statisticRepository: LazerScoreStatisticRepository,
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

        var current: Int
        var batch = 0
        val maxBatches = 3000 // 安全阈值，防止死循环

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

        } while (batch < maxBatches)

        if (batch >= maxBatches) {
            log.warn("Reached maximum batch limit ($maxBatches). Some scores may still be unfixed.")
        }

        return null
    }

    @Transactional
    fun fixZeroAccuracyScores(offset: Int = 0): Int {
        // 1. 取出异常数据 (例如前 100 条)
        val liteScores = repository.findInvalidAccuracyScores(100, offset)
        if (liteScores.isEmpty()) return 0

        // 2. 获取对应的统计数据 (hits 数)
        // 注意：LazerScoreStatisticRepository 里的 getStatistics 需要能匹配这些 ID

        val success = mutableListOf<Long>()
        val failed = mutableListOf<Long>()

        liteScores.forEach { lite ->
            val stat = statisticRepository.getStatistics(listOf(lite.id), lite.mode.toByte())
                .firstOrNull()?.let { JacksonUtil.parseObject<LazerStatistics>(it.data) }
                ?: run {
                    failed.add(lite.id)
                    return@forEach
                }

            val maxStat = if (lite.rankByte > 0) {
                stat.constructMaxStatistics(OsuMode.getMode(lite.mode))
            } else {
                statisticRepository.getStatistics(listOf(lite.id), -1)
                    .firstOrNull()?.let { JacksonUtil.parseObject<LazerStatistics>(it.data) }
                    ?: run {
                        failed.add(lite.id)
                        return@forEach
                    }
            }

            val fixedAcc = calc(stat, maxStat, lite.legacyScore == 0, OsuMode.getMode(lite.mode)) ?: run {
                failed.add(lite.id)
                return@forEach
            }

            if (fixedAcc > 0 && fixedAcc <= 1.0) {
                success.add(lite.id)
                lite.accuracy = fixedAcc.toFloat()
            }
        }

        log.info("fix success: {}, failed: {}", success.size, failed.size)

        // 6. 批量存回数据库
        return repository.saveAll(liteScores).size
    }

    private fun calc(stat: LazerStatistics, maxStat: LazerStatistics, isLazer: Boolean = false, mode: OsuMode): Double? {
        val m = stat
        val s = maxStat

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
