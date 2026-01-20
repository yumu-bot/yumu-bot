package com.now.nowbot.service

import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class DailyStatisticsService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val userInfoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
) {

    /**
     * 统计任务包括 user info 以及 scores
     */
    fun collectInfoAndScores() {
        Thread.ofVirtual().name("DailyStat-Main").start {
            val startTime = System.currentTimeMillis()
            runTask()
            val endTime = System.currentTimeMillis()
            log.info("统计全部绑定用户完成, 共耗时: ${(endTime - startTime) / 1000} s")
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

    /**
     * 统计任务包括 user info 以及 scores
     */
    fun runTask() {
        log.info("开始串行统计全部绑定用户")
        OsuApiBaseService.setPriority(9)

        var offset = 0
        val count = AtomicInteger(0)

        while (IocAllReadyRunner.APP_ALIVE) {
            // 获取一批用户
            val userIDs = bindDao.getAllUserIdLimit50(offset)
            if (userIDs.isEmpty()) break


            log.info("获取到第 ${count.incrementAndGet()} 批用户：${userIDs.size} 个")

            try {
                // 执行该批次：这里面的请求现在是一个接一个发的
                processUserBatch(userIDs)

                offset += userIDs.size

                // 批次间强制休息，给 API 喘息时间
                log.info("第 ${count.get()} 批次用户已更新完成。")
                Thread.sleep(2000)
            } catch (e: Exception) {
                log.error("处理批次 ${count.get()} 时发生异常，休息 10 秒后继续", e)
                Thread.sleep(10000)
            }
        }
    }
    private fun processUserBatch(userIDs: List<Long>) {
        val needSearch = mutableListOf<Pair<Long, OsuMode>>()

        // 1. 获取用户信息 (这是 1 次 API 请求)
        val userInfoList = userApiService.getUsers(userIDs, isVariant = true)
        val yesterdayInfo = userInfoDao.getFromYesterday(userIDs)
        val userMap = userInfoList.associateBy { it.userID }

        for (user in yesterdayInfo) {
            val uid = user.userID ?: continue
            val mode = user.mode
            val currentInfo = userMap[uid] ?: continue

            val newPlayCount = when (mode) {
                OsuMode.OSU -> currentInfo.rulesets?.osu?.playCount
                OsuMode.TAIKO -> currentInfo.rulesets?.taiko?.playCount
                OsuMode.CATCH -> currentInfo.rulesets?.fruits?.playCount
                OsuMode.MANIA -> currentInfo.rulesets?.mania?.playCount
                else -> null
            }

            if (newPlayCount != null && user.playCount != newPlayCount) {
                needSearch.add(uid to mode)
            }
        }

        // 2. 串行获取成绩 (这是 N 次 API 请求)
        for ((uid, mode) in needSearch) {
            try {
                // 这里不要再套任何线程池或 fork，直接顺序调用
                scoreApiService.getRecentScore(uid, mode, 0, 999)

                // 每个成绩请求之间强制间隔，极大地降低 429 风险
                Thread.sleep(1000)
            } catch (e: Exception) {
                log.warn("获取用户 $uid 成绩失败: ${e.message}")
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}