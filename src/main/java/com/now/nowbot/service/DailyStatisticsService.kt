package com.now.nowbot.service

import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
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
            log.info("更新玩家百分比完成, 耗时: ${(endTime - startTime) / 1000} s")
        }
    }

    fun collectingBindUser() {
        log.info("开始串行统计全部绑定用户")

        val offset = AtomicInteger(0)
        val count = AtomicInteger(0)

        while (IocAllReadyRunner.APP_ALIVE) {
            // 获取一批用户
            val users = bindDao.getBindUsersLimit50(offset.get())
            if (users.isEmpty()) break

            try {
                val processed = collectingUsers(users)
                log.info("第 ${count.get()} 批次用户已更新完成：${processed} 条更新。")
                offset.addAndGet(users.size)
            } catch (e: Exception) {
                log.error("处理批次 ${count.get()} 时发生异常：", e)
            }
        }

        log.info("统计全部绑定用户已完成")
    }

    private fun collectingUsers(users: List<BindUser>): Int {
        val ids = users.map { it.userID }

        waitForRateLimit(8000)
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

        val size = updatingUsers(needUpdate)

        return size
    }

    private fun updatingUsers(needUpdate: List<Pair<MicroUser, OsuMode>>): Int {
        needUpdate.forEach { (user, mode) ->
            try {
                waitForRateLimit(4000)
                scoreApiService.getRecentScore(user.userID, mode, 0, 999, isBackground = true)
                log.info("正在刷新用户 ${user.username} 的 ${mode.shortName} 模式成绩...")
            } catch (e: Exception) {
                log.warn("获取用户 ${user.username} 成绩失败: ${e.message}")
            }
        }

        return needUpdate.size
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}