package com.now.nowbot.service.repairServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.TachyonScoreLite
import com.now.nowbot.entity.TachyonStatisticsLite
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.TestService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.PreparedStatement
import java.sql.Types

@Service
@CheckPermission(isSuperAdmin = true)
class LazerToTachyonService(
    private val legacyScoreRepository: LazerScoreRepository,
    private val legacyStatisticsRepository: LazerScoreStatisticRepository,
    private val jdbcTemplate: JdbcTemplate
) : MessageService<Boolean> {

    // 设定每次处理的批次大小，防止一次性读取内存溢出
    private val BATCH_SIZE = 1000

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {
        /*
        val fix = "!" + "lt"

        if (messageText.contains(fix) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = true
            return true
        }

        val fix2 = "!" + "ls"

        if (messageText.contains(fix2) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = false
            return true
        }

         */

        return false
    }

    override fun handleMessage(event: MessageEvent, param: Boolean): ServiceCallStatistic? {
        var totalInserted = 0

        if (param) {
            var lastId: Long = 0
            while (true) {
                val batch = legacyScoreRepository.findByLastID(lastId, BATCH_SIZE)
                if (batch.isEmpty()) break

                val toInsert = batch.map { TachyonScoreLite.transfer(it) }
                batchInsertScore(toInsert)

                totalInserted += batch.size
                lastId = batch.last().id

                if (totalInserted % 10000 == 0 || batch.size < BATCH_SIZE) {
                    log.info("【Score】已处理: $totalInserted 条，当前 ID：${lastId}")
                }
            }
        } else {
            var lastId: Long = 0
            while (true) {
                val batch = legacyStatisticsRepository.findByLastID(lastId, BATCH_SIZE)
                if (batch.isEmpty()) break

                val toInsert = batch.map { TachyonStatisticsLite.transfer(it) }
                batchInsertStats(toInsert)

                totalInserted += batch.size
                lastId = batch.last().id // 确保这里的 id 类型和上面的 lastId 一致

                if (totalInserted % 10000 == 0 || batch.size < BATCH_SIZE) {
                    log.info("【Stats】已处理: $totalInserted 条，当前 ID：${lastId}")
                }
            }
        }

        event.reply("处理完成！总插入：${totalInserted}")
        return null
    }

    private fun batchInsertScore(dataList: List<TachyonScoreLite>) {
        val sql = """
        INSERT INTO tachyon_score
        (id, user_id, beatmap_id, build_id, mods, data, pp, accuracy, combo, time, fc, pass, legacy, score, mode, rank) 
        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
    """.trimIndent()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val data = dataList[i]
                ps.setLong(1, data.scoreID)
                ps.setLong(2, data.userID)
                ps.setLong(3, data.beatmapID)

                // 处理可空字段
                if (data.buildID != null) {
                    ps.setInt(4, data.buildID!!)
                } else {
                    ps.setNull(4, Types.INTEGER)
                }

                // 处理数组 (PostgreSQL)
                if (data.mods != null) {
                    val modsArray = ps.connection.createArrayOf("bpchar", data.mods!!.toTypedArray())
                    ps.setArray(5, modsArray)
                } else {
                    ps.setNull(5, Types.ARRAY)
                }

                ps.setString(6, data.modsData)
                ps.setFloat(7, data.pp)
                ps.setFloat(8, data.accuracy)
                ps.setInt(9, data.maxCombo)
                ps.setObject(10, data.time)
                ps.setBoolean(11, data.perfect)
                ps.setBoolean(12, data.passed)

                if (data.legacy != null) {
                    ps.setInt(13, data.legacy!!)
                } else {
                    ps.setNull(13, Types.INTEGER)
                }

                ps.setInt(14, data.score)
                ps.setByte(15, data.mode)
                ps.setByte(16, data.rank)
            }
            override fun getBatchSize() = dataList.size
        })
    }

    private fun batchInsertStats(dataList: List<TachyonStatisticsLite>) {
        val sql = """
        INSERT INTO tachyon_statistics
        (id, mode, perfect, great, good, ok, meh, miss, ignore_hit, ignore_miss, 
         large_tick_hit, large_tick_miss, small_tick_hit, small_tick_miss, 
         slider_tail_hit, large_bonus, small_bonus, legacy_combo_increase) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id, mode) DO NOTHING
    """.trimIndent()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val data = dataList[i]
                ps.setLong(1, data.statisticsID)
                ps.setByte(2, data.mode)
                ps.setInt(3, data.statistics.perfect)
                ps.setInt(4, data.statistics.great)
                ps.setInt(5, data.statistics.good)
                ps.setInt(6, data.statistics.ok)
                ps.setInt(7, data.statistics.meh)
                ps.setInt(8, data.statistics.miss)
                ps.setInt(9, data.statistics.ignoreHit)
                ps.setInt(10, data.statistics.ignoreMiss)
                ps.setInt(11, data.statistics.largeTickHit)
                ps.setInt(12, data.statistics.largeTickMiss)
                ps.setInt(13, data.statistics.smallTickHit)
                ps.setInt(14, data.statistics.smallTickMiss)
                ps.setInt(15, data.statistics.sliderTailHit)
                ps.setInt(16, data.statistics.largeBonus)
                ps.setInt(17, data.statistics.smallBonus)
                ps.setInt(18, data.statistics.legacyComboIncrease)
            }
            override fun getBatchSize() = dataList.size
        })
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)
    }
}