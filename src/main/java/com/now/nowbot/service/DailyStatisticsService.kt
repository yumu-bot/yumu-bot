package com.now.nowbot.service

import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.DataUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class DailyStatisticsService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val userInfoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
) {
    private val isRunning = AtomicBoolean(false)

    private val lastRequestTime = AtomicLong(0L)  // 原子变量，记录最后请求时间

    // 简单的速率限制方法
    private fun waitForRateLimit(intervalMillis: Long = 1000) {
        while (true) {
            val now = System.currentTimeMillis()
            val last = lastRequestTime.get()
            val elapsed = now - last

            if (elapsed >= intervalMillis) {
                if (lastRequestTime.compareAndSet(last, now)) {
                    return
                }
            } else {
                Thread.sleep(intervalMillis - elapsed)
            }
        }
    }

    fun collectInfoAndScores(callback: () -> Unit = {}) {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("任务已在运行中，跳过本次触发")
            return
        }

        Thread.ofVirtual().name("DailyStat-Main").start {
            try {
                collectingBindUser()
                callback()
            } catch (e: Exception) {
                log.error("任务失败", e)
            } finally {
                isRunning.set(false)
            }
        }
    }

    fun collectPercentiles() {
        Thread.ofVirtual().name("DailyStat-Percentiles").start {
            val startTime = System.currentTimeMillis()
            userInfoDao.percentilesDailyUpsert()
            val endTime = System.currentTimeMillis()
            log.info("更新玩家百分比完成, 耗时: ${DataUtil.time2HMS(endTime - startTime)}")
        }
    }

    fun collectingBindUser() {
        log.info("开始串行统计全部绑定用户")

        val startTime = System.currentTimeMillis()

        val offset = AtomicInteger(0)
        val batch = AtomicInteger(1)
        val count = AtomicInteger(0)
        val score = AtomicInteger(0)

        while (IocAllReadyRunner.APP_ALIVE) {
            // 获取一批用户
            val users = bindDao.getBindUsersLimit50(offset.get())
            if (users.isEmpty()) break

            try {
                val (t, s) = collectingUsers(users)

                log.info("""
                    第 ${batch.get()} 批用户已更新完成：
                    需要更新：$t 人，总计 ${count.addAndGet(t)} 人，
                    其中包含：$s 条成绩，总计 ${score.addAndGet(s)} 条。
                    """.trimIndent())
                offset.addAndGet(users.size)
            } catch (e: Exception) {
                log.error("第 ${batch.get()} 批次发生异常：", e)
            }

            batch.getAndIncrement()
        }

        val endTime = System.currentTimeMillis()

        log.info("统计已完成，耗时： ${DataUtil.time2HMS(endTime - startTime)}")
    }

    private fun collectingUsers(users: List<BindUser>): Pair<Int, Int> {
        val ids = users.map { it.userID }

        waitForRateLimit(5000)
        val stats = userApiService.getUsers(users = ids, isVariant = true, isBackground = true)

        val needUpdate = stats.flatMap { micro ->
            val plays = userInfoDao.getPlayCountsFromUserIDBeforeToday(micro.userID)

            val currents = listOf(
                micro.rulesets?.osu?.playCount ?: 0L,
                micro.rulesets?.taiko?.playCount ?: 0L,
                micro.rulesets?.fruits?.playCount ?: 0L,
                micro.rulesets?.mania?.playCount ?: 0L,
            )

            plays.mapIndexed { index, pc ->
                val current = currents[index]
                val changed = current != pc

                val mode = OsuMode.getMode(index)

                Triple(micro, mode, changed)
            }.filter {
                it.third
            }.map {
                it.first to it.second
            }
        }

        val scoreCount = updatingUsers(needUpdate)

        return needUpdate.size to scoreCount
    }

    private fun updatingUsers(needUpdate: List<Pair<MicroUser, OsuMode>>): Int {
        val scoreCount = AtomicInteger(0)

        needUpdate.forEach { (user, mode) ->
            try {
                waitForRateLimit(2500)
                val count = scoreApiService.getRecentScore(user.userID, mode, 0, 999, isBackground = true).size
                log.info("正在刷新 ${user.username}：${mode.shortName} 模式的 $count 条成绩...")
                scoreCount.addAndGet(count)
            } catch (e: Exception) {
                log.warn("获取 ${user.username} 成绩失败: ${e.message}")
            }
        }

        return scoreCount.get()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}