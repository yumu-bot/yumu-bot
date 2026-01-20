package com.now.nowbot.service

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.StructuredTaskScope

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
        log.info("开始并发统计全部绑定用户")
        // 设置较低优先级，避免干扰在线用户的即时请求
        OsuApiBaseService.setPriority(9)

        try {
            // 使用 StructuredTaskScope 管理这一批次的所有虚拟线程任务
            StructuredTaskScope.ShutdownOnFailure().use { scope ->
                var offset = 0
                val allTasks = mutableListOf<StructuredTaskScope.Subtask<Unit>>()

                while (true) {
                    val userIDs = bindDao.getAllUserIdLimit50(offset)
                    if (userIDs.isEmpty()) break

                    // 为每一组 50 个用户开启一个并发任务
                    val subtask = scope.fork {
                        processUserBatch(userIDs)
                    }
                    allTasks.add(subtask)

                    offset += userIDs.size

                    Thread.sleep(2000)
                }

                // 等待所有任务完成（底层 API Base 会控制实际执行频率）
                scope.join()
                scope.throwIfFailed()
            }
        } catch (e: Exception) {
            log.error("统计任务执行期间发生严重异常", e)
        } finally {
            OsuApiBaseService.clearPriority()
        }
    }

    private fun processUserBatch(userIDs: List<Long>) {
        try {
            // 动态背压：如果底层待处理任务太多，这里直接阻塞，不往里塞任务了
            while (OsuApiBaseService.getTaskQueueSize() > 40) {
                log.debug("底层任务积压，后台投递暂停...")
                Thread.sleep(3000)
            }

            val needSearch = mutableListOf<Pair<Long, OsuMode>>()

            // 1. 获取用户信息 (内部会调用 ApiBase.request)
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
                } ?: continue

                if (user.playCount != newPlayCount) {
                    needSearch.add(uid to mode)
                }
            }

            // 2. 如果有新成绩，并发获取成绩（可选：再次 fork 或直接循环）
            // 这里因为是在独立的 fork 线程中，直接循环也是并发的（相对于其他 batch）
            for ((uid, mode) in needSearch) {
                scoreApiService.getRecentScore(uid, mode, 0, 999)
            }

            log.debug("Batch 处理完成: {} users, {} need update", userIDs.size, needSearch.size)
        } catch (e: Exception) {
            log.error("处理用户批次失败: $userIDs", e)
            // 这里不抛出异常，允许其他 batch 继续执行
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyStatisticsService::class.java)
    }
}