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

    /**
     * 统计任务包括 user info 以及 scores
     */
    fun asyncTask() {
        Thread.startVirtualThread {
            val startTime = System.currentTimeMillis()
            runTask()
            val endTime = System.currentTimeMillis()
            log.info("统计全部绑定用户完成, 耗时: ${(endTime - startTime) / 1000} s")
        }
    }

    /**
     * 统计任务包括 user info 以及 scores
     */
    fun runTask() {
        log.info("开始统计全部绑定用户")
        OsuApiBaseService.setPriority(9)
        var offset = 0
        var userIDs: List<Long> = bindDao.getAllUserIdLimit50(offset)
        var errorCount = 0
        while (userIDs.isNotEmpty()) {
            try {
                val needSearch = mutableListOf<Pair<Long, OsuMode>>()
                saveUserInfo(userIDs, needSearch)
                savePlayData(needSearch)
            } catch (e: Exception) {
                if (errorCount < 3) {
                    log.error("统计用户第${offset / 50}轮出现异常, 正在重试...", e)
                    errorCount++
                    continue
                } else {
                    log.error("统计用户第${offset / 50}轮连续异常, 退出本轮", e)
                    log.error("下面 UID 已跳过[\n${userIDs.joinToString(",\n")}\n]")
                }
            }

            offset += userIDs.size
            userIDs = bindDao.getAllUserIdLimit50(offset)
        }
    }

    private fun saveUserInfo(uidList: List<Long>, needSearch: MutableList<Pair<Long, OsuMode>>) {
        val userInfoList = userApiService.getUsers(uidList, isVariant = true)
        val yesterdayInfo = userInfoDao.getFromYesterday(uidList)
        val userMap = userInfoList.associateBy { it.userID }
        for (user in yesterdayInfo) {
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
                needSearch.add(user.userID to mode)
            }
        }
    }

    private fun savePlayData(data: MutableList<Pair<Long, OsuMode>>) {
        for ((uid, mode) in data) {
            log.info("save $uid $mode")
            scoreApiService.getRecentScore(uid, mode, 0, 999)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}