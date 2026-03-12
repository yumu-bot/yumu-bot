package com.now.nowbot.service

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.NewbieDao
import com.now.nowbot.model.osu.LazerMod.Companion.isNotAffectStarRating
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import kotlin.math.min
import kotlin.math.roundToLong

@Service("NEWBIE_RESTRICT")
class NewbieRestrictService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val bindDao: BindDao,
    private val newbieDao: NewbieDao,
    private val botContainer: BotContainer,
    config: NewbieConfig,
) {
    // 新人群、杀手群、执行机器人
    private val newbieGroupID = config.newbieGroup
    private val killerGroupID = config.killerGroup
    private val executorBotID = config.hydrantBot

    // 赦免图：No title，竹取飞翔，C type
    private val remitBIDs = config.remitBIDs.toSet()

    fun checkAsync(event: MessageEvent, score: LazerScore) {
        Thread.ofVirtual().name("newbie-restrict").start {
            try {
                check(event, score)
            } catch (e: Exception) {
                log.error("新人群超星禁言：发生错误：${e.message}", e)
            }
        }
    }

    fun checkAsync(event: MessageEvent, scores: List<LazerScore>) {
        Thread.ofVirtual().name("newbie-restrict").start {
            try {
                var maxScore: LazerScore? = null

                for (score in scores) {
                    // 过滤逻辑
                    if (remitBIDs.contains(score.beatmapID) && score.mods.isNotAffectStarRating()) {
                        continue
                    }

                    // 寻找最大值逻辑
                    if (maxScore == null || score.beatmap.starRating > maxScore.beatmap.starRating) {
                        maxScore = score
                    }
                }

                maxScore?.let { check(event, it) }
            } catch (e: Throwable) {
                log.error("新人群超星禁言：发生错误：${e.message}", e)
            }
        }
    }

    fun checkAsync(event: MessageEvent, scoreMap: Map<*, LazerScore>) {
        Thread.ofVirtual().name("newbie-restrict").start {
            try {
                var maxScore: LazerScore? = null

                for (score in scoreMap.values) {
                    // 过滤逻辑
                    if (remitBIDs.contains(score.beatmapID) && score.mods.isNotAffectStarRating()) {
                        continue
                    }

                    // 寻找最大值逻辑
                    if (maxScore == null || score.beatmap.starRating > maxScore.beatmap.starRating) {
                        maxScore = score
                    }
                }

                maxScore?.let { check(event, it) }
            } catch (e: Throwable) {
                log.error("新人群超星禁言：发生错误：${e.message}", e)
            }
        }
    }

    private fun check(event: MessageEvent, score: LazerScore) {
        if (event.subject.contactID != newbieGroupID || event.subject !is Group) return

        val beatmap = beatmapApiService.getBeatmap(score.beatmapID)
        beatmapApiService.applyBeatmapExtend(score, beatmap)

        val sr = score.beatmap.starRating
        val silence = getSilence(sr)
        if (silence <= 0) return

        val criminal = event.sender

        val criminalUsername = bindDao.getBindFromQQOrNull(criminal.contactID)?.username ?: "未绑定"

        val username = score.user.username

        val count7 = newbieDao.getRestrictedCountWithin(criminal.contactID, 7L * 24 * 60 * 60 * 1000)
        val duration7 = getTime(newbieDao.getRestrictedDurationWithin(criminal.contactID, 7L * 24 * 60 * 60 * 1000) / 60000)

        val t = newbieDao.getRestricted(criminal.contactID)
        val count = t.size
        val duration = getTime(t.sumOf { it.duration ?: 0L } / 60000)

        val formatter = DecimalFormat("#.##")

        val last5 = t.asSequence()
            .filter { it.time != null && it.star != null }
            .sortedByDescending { it.time!! }
            .mapNotNull { it.star }
            .take(5)
            .toList()

        val final5 = if (last5.isNotEmpty()) {
            last5.joinToString(", ") { formatter.format(it) }
        } else {
            "0"
        }

        val punishment = String.format("%.2f", sr) + "星 -> " + getTime(silence)

        val sb = StringBuilder()

        sb.append("检测到 ${criminal.name} (${criminalUsername}) 超星。").append('\n')
            .append("玩家：${username}").append('\n')
            .append("星数：${punishment}").append('\n')
            .append("超星谱面：${score.previewName}").append('\n')
            .append("七天内：${count7}次，${duration7}。").append('\n')
            .append("总计：${count}次，${duration}。").append('\n')
            .append("最近五次星数：[${final5}]。").append('\n').append('\n')

        try {
            newbieDao.saveRestricted(criminal.contactID, sr, System.currentTimeMillis(),
                min(silence * 60000L, 7L * 24 * 60 * 60 * 1000)
            )
        } catch (e: Throwable) {
            log.error(sb.append("但是保存记录失败了。").toString(), e)
        }

        val executorBot = runCatching {
            botContainer.robots[executorBotID]
        }.getOrNull()

        if (executorBot == null) {
            log.info(sb.append("但是执行机器人并未上线。无法执行禁言任务。").toString())
            return
        }

        val isReportable = runCatching {
            executorBot.groupList.data?.any { it.groupId == killerGroupID }
        }.getOrNull() == true

        if (Permission.isGroupAdmin(event)) {
            report(isReportable, executorBot, sb.append("但是对方是管理员或群主，无法执行禁言任务。").toString())
            return
        }

        // 最后保险，确保不乱禁人
        if (event.subject.contactID != newbieGroupID) return

        // 情节严重
        if (silence >= 30 * 24 * 60 - 1) {
            report(isReportable, executorBot, sb.append("情节严重，已按最大时间禁言。").toString())


            val action = runCatching {
                executorBot.setGroupBan(newbieGroupID, event.sender.contactID, (30 * 24 * 60 - 1) * 60)
            }.getOrNull()


            if (action == null) {
                report(isReportable, executorBot, sb.append("但是机器人执行禁言任务失败了。").toString())
            }

        } else {
            report(isReportable, executorBot, sb.append("正在执行禁言任务。").toString())

            val action = runCatching {
                executorBot.setGroupBan(newbieGroupID, event.sender.contactID, (silence * 60).toInt())
            }.getOrNull()

            if (action == null) {
                report(isReportable, executorBot, sb.append("但是机器人执行禁言任务失败了。").toString())
            }
        }
    }

    private fun report(isReportable: Boolean = false, executorBot: Bot? = null, messageText: String) {
        if (isReportable && executorBot != null) {
            runCatching {
                executorBot.sendGroupMsg(killerGroupID, messageText, true)
            }.onFailure {
                log.warn("新人群禁言：正在报告禁言任务，但是发送失败了。\n原消息如下：\n${messageText}")
            }
        } else {
            log.warn("新人群禁言：正在报告禁言任务，但是不可发送或执行机器人未上线。\n原消息如下：\n${messageText}")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NewbieRestrictService::class.java)

        /**
         * 获取禁言时长（分钟）
         */
        private fun getSilence(star: Double): Long {
            return if (star <= 5.7) {
                // 未超星
                0L
            } else if (star < 6.0) {
                ((star - 5.7) * 1000).roundToLong()
            } else {
                ((star - 5.7) * 2000).roundToLong()
            }
        }

        private fun getTime(minutes: Long): String {
            return if (minutes >= 1440) {
                val day = minutes / 1440
                val hour = (minutes - (day * 1440)) / 60
                val minute = minutes - (day * 1440) - (hour * 60)

                "${day}天${hour}时${minute}分"
            } else if (minutes >= 60.0) {
                val hour = minutes / 60
                val minute = minutes - (hour * 60)

                "${hour}时${minute}分"
            } else {
                "${minutes}分"
            }
        }
    }
}