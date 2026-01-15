package com.now.nowbot.service

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DailyStatisticsService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val userInfoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
) {

    object SimpleRateLimiter {
        private var nextAvailableTime = System.currentTimeMillis()

        @Synchronized
        fun sync(qps: Int = 8) {
            val interval = 1000 / qps

            val now = System.currentTimeMillis()
            if (now < nextAvailableTime) {
                val sleepTime = nextAvailableTime - now
                Thread.sleep(sleepTime)
                nextAvailableTime += interval
            } else {
                nextAvailableTime = now + interval
            }
        }
    }

    /**
     * 统计任务包括 user info 以及 scores
     */
    fun collectInfoAndScores() {
        Thread.startVirtualThread {
            val startTime = System.currentTimeMillis()
            runTask()
            val endTime = System.currentTimeMillis()
            log.info("统计全部绑定用户完成, 耗时: ${(endTime - startTime) / 1000} s")
        }
    }

    fun collectPercentiles() {
        Thread.startVirtualThread {
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
        log.info("开始统计全部绑定用户")
        OsuApiBaseService.setPriority(9)

        var offset = 0
        var errorCount = 0

        while (true) {
            val userIDs = bindDao.getAllUserIdLimit50(offset)
            if (userIDs.isEmpty()) break

            try {
                val needSearch = mutableListOf<Pair<Long, OsuMode>>()

                // 内部已包含 SimpleRateLimiter.sync()
                saveUserInfo(userIDs, needSearch)

                // 内部已包含 SimpleRateLimiter.sync()
                savePlayData(needSearch)

                // 执行成功：重置错误计数，移动位移
                errorCount = 0
                offset += userIDs.size

            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("429")) {
                    // 核心修改：遇到429绝对不要立即重试或增加offset
                    log.error("触发 API 限速(429)，虚拟线程进入深度冷却...")
                    // 遇到429时，即便有频率限制也说明额度耗尽，建议至少休息 30-60s
                    Thread.sleep(60_000)
                    continue // 原地重试，不增加 offset
                }

                errorCount++
                if (errorCount < 3) {
                    log.warn("第 ${offset / 50} 轮异常，尝试第 $errorCount 次重试", e)
                    Thread.sleep(2000) // 普通异常轻微休息
                } else {
                    // 连续多次失败，为了保证任务继续，跳过这 50 个用户
                    log.error("第 ${offset / 50} 轮连续失败，跳过 UIDs: $userIDs")
                    offset += userIDs.size
                    errorCount = 0
                }
            }
        }
    }

    private fun saveUserInfo(userIDs: List<Long>, needSearch: MutableList<Pair<Long, OsuMode>>) {
        SimpleRateLimiter.sync()
        val userInfoList = userApiService.getUsers(userIDs, isVariant = true)
        val yesterdayInfo = userInfoDao.getFromYesterday(userIDs)
        val userMap = userInfoList.associateBy { it.userID }

        for (user in yesterdayInfo) {
            if (user.userID == null) continue

            val mode = user.mode
            val userInfo = userMap[user.userID] ?: continue
            val newPlayCount = when (user.mode) {
                OsuMode.OSU -> userInfo.rulesets?.osu?.playCount ?: continue
                OsuMode.TAIKO -> userInfo.rulesets?.taiko?.playCount ?: continue
                OsuMode.CATCH -> userInfo.rulesets?.fruits?.playCount ?: continue
                OsuMode.MANIA -> userInfo.rulesets?.mania?.playCount ?: continue
                else -> {
                    log.error("统计用户出现异常: 数据库中出现 default mode")
                    continue
                }
            }

            log.info("用户 ${user.userID} 模式 $mode playCount: ${user.playCount} -> $newPlayCount")

            if (user.playCount != newPlayCount) {
                needSearch.add(user.userID!! to mode)
            }
        }
    }

    private fun savePlayData(data: MutableList<Pair<Long, OsuMode>>) {
        for ((uid, mode) in data) {
            // log.info("save $uid $mode")
            SimpleRateLimiter.sync()
            scoreApiService.getRecentScore(uid, mode, 0, 999)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}