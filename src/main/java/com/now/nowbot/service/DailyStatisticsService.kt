package com.now.nowbot.service

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService
import com.now.nowbot.util.DataUtil.findCauseOfType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class DailyStatisticsService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val userInfoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
) {

    fun collectPercentiles() {
        Thread.startVirtualThread {
            val startTime = System.currentTimeMillis()
            userInfoDao.percentilesDailyUpsert()
            val endTime = System.currentTimeMillis()
            log.info("更新玩家百分比完成, 耗时: ${(endTime - startTime) / 1000} s")
        }
    }


    fun collectInfoAndScores() {
        Thread.startVirtualThread {
            val startTime = System.currentTimeMillis()
            // 在进入任务前统一设置优先级
            OsuApiBaseService.setPriority(9)
            OsuApiBaseService.updateGlobalQps(4)

            try {
                runTask()
            } finally {
                // 任务结束后清除优先级，防止影响当前线程后续逻辑
                OsuApiBaseService.updateGlobalQps(8)
                OsuApiBaseService.clearPriority()
            }
            val endTime = System.currentTimeMillis()
            log.info("统计全部绑定用户完成, 耗时: ${(endTime - startTime) / 1000} s")
        }
    }

    fun runTask() {
        log.info("开始统计全部绑定用户")

        var lastID = -1L
        var errorCount = 0
        var continuous429Count = 0

        while (true) {
            // 使用游标查询，避开 offset 漂移
            val userIDs = bindDao.getNextBindUserIDs(lastID)
            if (userIDs.isEmpty()) break

            try {
                val needSearch = mutableListOf<Pair<Long, OsuMode>>()

                saveUserInfo(userIDs, needSearch)
                savePlayData(needSearch)

                // 成功后逻辑
                lastID = userIDs.lastOrNull() ?: Long.MAX_VALUE
                errorCount = 0
                continuous429Count = 0

                log.info("已处理至 UID: $lastID")

            } catch (e: Exception) {
                val is429 = e.findCauseOfType<WebClientResponseException.TooManyRequests>()

                if (is429 != null) {
                    continuous429Count++

                    // 指数级增加冷却时间：1min, 2min, 4min... 最大 32min
                    val waitTime = minOf(60_000L * (1 shl (continuous429Count - 1)), 1920_000L)

                    log.error("API 限速中 (第 $continuous429Count 次)。冷却 ${waitTime / 60000} 分钟...")

                    if (continuous429Count > 6) {
                        log.error("连续触发 429 次数过多，可能触发了 IP 封禁或长期限制，终止今日任务。")
                        break
                    }

                    Thread.sleep(waitTime)
                    continue
                }

                // 处理非 429 的普通异常（如 500, Timeout 等）
                errorCount++
                if (errorCount < 3) {
                    log.warn("批次 [ID > $lastID] 发生异常，尝试第 $errorCount 次重试", e)
                    Thread.sleep(5000)
                } else {
                    log.error("批次 [ID > $lastID] 连续失败 3 次，跳过该批次防止死循环。用户清单: $userIDs")
                    lastID = userIDs.last() // 关键：强制移动游标，跳过这 50 个用户
                    errorCount = 0
                }
            }
        }
    }

    private fun saveUserInfo(userIDs: List<Long>, needSearch: MutableList<Pair<Long, OsuMode>>) {

        val userInfoList = userApiService.getUsers(userIDs, isVariant = true)
        val yesterdayInfo = userInfoDao.getFromYesterday(userIDs)
        val userMap = userInfoList.associateBy { it.userID }

        for (user in yesterdayInfo) {
            val uid = user.userID ?: continue
            val userInfo = userMap[uid] ?: continue

            val newPlayCount = when (user.mode) {
                OsuMode.OSU -> userInfo.rulesets?.osu?.playCount
                OsuMode.TAIKO -> userInfo.rulesets?.taiko?.playCount
                OsuMode.CATCH -> userInfo.rulesets?.fruits?.playCount
                OsuMode.MANIA -> userInfo.rulesets?.mania?.playCount
                else -> null
            } ?: continue

            if (user.playCount != newPlayCount) {
                needSearch.add(uid to user.mode)
            }
        }
    }

    private fun savePlayData(data: List<Pair<Long, OsuMode>>) {
        for ((uid, mode) in data) {
            // 这里也不再需要 sync()。
            // 循环会迅速向底层 TASKS 队列塞入请求，底层会根据 QPS 限制慢慢发。
            scoreApiService.getRecentScore(uid, mode, 0, 999)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}