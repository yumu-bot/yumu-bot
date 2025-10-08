package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.NewbieDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService

import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToLong

@Service("NEWBIE_RESTRICT")
class NewbieRestrictService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao,
    private val newbieDao: NewbieDao,
    private val botContainer: BotContainer,
    config: NewbieConfig,
): MessageService<Collection<LazerScore>> {
    // 这里放幻数

    // 新人群、杀手群、执行机器人
    private val newbieGroupID = config.newbieGroup
    private val killerGroupID = config.killerGroup
    private val executorBotID = config.hydrantBot

    // 赦免图：No title，竹取飞翔，C type
    private val remitBIDs = config.remitBIDs.toList()

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Collection<LazerScore>>
    ): Boolean {
        // TODO 测试的时候记得别忘了取消注释这个
        if (event.subject !is Group || event.subject.id != newbieGroupID) return false

        val ss = Instruction.SCORES.matcher(messageText)
        val s = Instruction.SCORE.matcher(messageText)

        val pr = Instruction.SCORE_PR.matcher(messageText)
        val t = Instruction.TODAY_BP.matcher(messageText)
        val b = Instruction.BP.matcher(messageText)
        val m = Instruction.MAP.matcher(messageText)
        val ba = Instruction.BP_ANALYSIS.matcher(messageText)

        val scores: List<LazerScore>
        val user: OsuUser

        try {
            if (ss.find()) {
                val bid = getBid(ss)
                if (bid == 0L) return false

                val inputMode = getMode(ss)

                val map = beatmapApiService.getBeatmap(bid)
                val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                user = getUserWithoutRange(event, ss, CmdObject(mode))
                scores = scoreApiService.getBeatmapScores(map.beatmapID, user.userID, mode)

                beatmapApiService.applyBeatmapExtendForSameScore(scores, map)
                calculateApiService.applyStarToScores(scores, local = false)
            } else if (s.find()) {
                val bid = getBid(s)
                if (bid == 0L) return false

                val inputMode = getMode(s)
                val mods = getMod(s)

                val map = beatmapApiService.getBeatmap(bid)
                val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                user = getUserWithoutRange(event, s, CmdObject(mode))
                scores = listOf(scoreApiService.getBeatMapScore(map.beatmapID, user.userID, mode, mods)?.score ?: return false)
                calculateApiService.applyStarToScores(scores, local = false)
            } else if (pr.find()) {
                val any: String = pr.group("any") ?: ""

                // 避免指令冲突
                if (any.contains("&sb", ignoreCase = true)) return false

                val isPass =
                    if (pr.group("recent") != null) {
                        false
                    } else if (pr.group("pass") != null) {
                        true
                    } else {
                        return false
                    }

                val isMyself = AtomicBoolean()
                val mode = getMode(pr)

                val range = getUserAndRangeWithBackoff(event, pr, mode, isMyself, messageText, "recent")

                range.setZeroToRange100()

                val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, REG_RANGE.toRegex())

                // 如果不加井号，则有时候范围会被匹配到这里来
                val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
                val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
                val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

                if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
                    return false
                }

                val ranges = if (hasRangeInConditions) {
                    rangeInConditions
                } else {
                    pr.group(FLAG_RANGE)
                }?.split(REG_HYPHEN.toRegex())

                val range2 = if (range.start != null) {
                    range
                } else {
                    val start = ranges?.firstOrNull()?.toIntOrNull()
                    val end = if (ranges?.size == 2) ranges.last().toIntOrNull() else null

                    CmdRange(range.data!!, start, end)
                }

                val isMultiple = (pr.group("s").isNullOrBlank().not() || pr.group("es").isNullOrBlank().not())

                val isAvoidance: Boolean = CmdUtil.isAvoidance(messageText, "recent")

                val ps = range2.run {
                    val offset: Int
                    val limit: Int

                    if (hasCondition && this.start == null) {
                        offset = 0
                        limit = 100
                    } else if (isMultiple) {
                        offset = getOffset(0, true)
                        limit = getLimit(20, true)
                    } else {
                        offset = getOffset(0, false)
                        limit = getLimit(1, false)
                    }

                    val pss = scoreApiService.getScore(this.data!!.userID, mode.data, offset, limit, isPass)

                    calculateApiService.applyStarToScores(pss, local = false)
                    calculateApiService.applyBeatMapChanges(pss)

                    // 检查查到的数据是否为空
                    if (pss.isEmpty()) {
                        return false
                    }

                    pss.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
                }

                if (ps.isEmpty() && isAvoidance) {
                    return false
                }

                val filteredScores = ScoreFilter.filterScores(ps, conditions)

                if (filteredScores.isEmpty()) {
                    return false
                }

                scores = filteredScores.map { it.value }
            } else if (b.find()) {
                val any: String = b.group("any") ?: ""

                // 避免指令冲突
                if (any.contains("&sb", ignoreCase = true)) return false

                val isMyself = AtomicBoolean() // 处理 range
                val mode = getMode(b)

                val range = getUserAndRangeWithBackoff(event, b, mode, isMyself, messageText, "bp")
                range.setZeroToRange100()

                val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, REG_RANGE.toRegex())

                // 如果不加井号，则有时候范围会被匹配到这里来
                val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
                val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
                val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

                if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
                    return false
                }

                val ranges = if (hasRangeInConditions) {
                    rangeInConditions
                } else {
                    b.group(FLAG_RANGE)
                }?.split(REG_HYPHEN.toRegex())

                val range2 = if (range.start != null) {
                    range
                } else {
                    val start = ranges?.firstOrNull()?.toIntOrNull()
                    val end = if (ranges?.size == 2) ranges.last().toIntOrNull() else null

                    CmdRange(range.data!!, start, end)
                }

                val isMultiple = b.group("s").isNullOrBlank().not()

                val bs = range2.run {
                    val offset: Int
                    val limit: Int

                    if (hasCondition && this.start == null) {
                        offset = 0
                        limit = 200
                    } else if (isMultiple) {
                        offset = getOffset(0, true)
                        limit = getLimit(20, true)
                    } else {
                        offset = getOffset(0, false)
                        limit = getLimit(1, false)
                    }

                    val bss = scoreApiService.getBestScores(range2.data!!.userID, mode.data, offset, limit)
                    calculateApiService.applyStarToScores(bss, local = false)

                    bss.mapIndexed { index: Int, score: LazerScore -> (index + 1) to score }.toMap()
                }

                val filteredScores = ScoreFilter.filterScores(bs, conditions)

                if (filteredScores.isEmpty()) return false

                scores = filteredScores.map { it.value }
            } else if (m.find()) {
                val bid = getBid(m)

                val beatmap = if (bid > 0L) {
                    beatmapApiService.getBeatmap(bid)
                } else return false

                val mode = OsuMode.getMode(m.group("mode"))
                val mods = LazerMod.getModsList(m.group("mod"))

                // 自己构建成绩
                val constructScore = LazerScore()

                constructScore.beatmapID = beatmap.beatmapID
                constructScore.beatmap = beatmap
                constructScore.beatmapset = beatmap.beatmapset!!
                constructScore.ruleset = mode.modeValue
                constructScore.mods = mods

                scores = listOf(constructScore)
                calculateApiService.applyStarToScores(scores, local = false)
            } else if (t.find()) {
                val mode = getMode(t)
                val range = getUserWithRange(event, t, mode, AtomicBoolean())
                range.setZeroDay()
                user = range.data ?: return false

                val dayStart = range.getDayStart()
                val dayEnd = range.getDayEnd()

                val bps = scoreApiService.getBestScores(user.userID, mode.data)
                val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
                val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())

                scores = bps.filter { it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isBefore(laterDay) && it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isAfter(earlierDay) }
                calculateApiService.applyStarToScores(scores, local = false)
            } else if (ba.find()) {
                /*
                val mode = getMode(ba)
                user = getUserWithoutRange(event, ba, mode, AtomicBoolean())
                scores = scoreApiService.getBestScores(user.userID, mode.data).take(6)

                 */
                // TODO BA 在部分情况下会忽略群聊模式，暂时禁用

                return false
            } else return false
        } catch (e: Throwable) {
            return false
        }

        data.value = scores.filterNot {
            remitBIDs.contains(it.beatmapID) && LazerMod.noStarRatingChange(it.mods)
        }

        return !data.value.isNullOrEmpty()
    }

    override fun handleMessage(event: MessageEvent, param: Collection<LazerScore>): ServiceCallStatistic? {
        val score = param.maxByOrNull { it.beatmap.starRating } ?: return null

        val beatmap = beatmapApiService.getBeatmap(score.beatmapID)
        beatmapApiService.applyBeatmapExtend(score, beatmap)

        val sr = score.beatmap.starRating
        val silence = getSilence(sr)
        if (silence <= 0) return null

        val criminal = event.sender

        val criminalUsername = try {
            bindDao.getBindFromQQ(criminal.id).username
        } catch (e: Exception) {
            "未绑定"
        }

        val username = score.user.username

        val count7 = newbieDao.getRestrictedCountWithin(criminal.id, 7L * 24 * 60 * 60 * 1000)
        val duration7 = getTime(newbieDao.getRestrictedDurationWithin(criminal.id, 7L * 24 * 60 * 60 * 1000) / 60000)

        val t = newbieDao.getRestricted(criminal.id)
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
            newbieDao.saveRestricted(criminal.id, sr, System.currentTimeMillis(), min(silence * 60000L, 7L * 24 * 60 * 60 * 1000))
        } catch (e: Throwable) {
            log.error(sb.append("但是保存记录失败了。").toString(), e)
        }

        val executorBot = botContainer.robots[executorBotID]
            ?: run {
                log.info(sb.append("但是执行机器人并未上线。无法执行禁言任务。").toString())
                return null
            }

        val isReportable = executorBot.groupList.data?.map { it.groupId }?.contains(killerGroupID) == true

        if (Permission.isGroupAdmin(event)) {
            report(isReportable, executorBot, sb.append("但是对方是管理员或群主，无法执行禁言任务。").toString())
            return null
        }

        // 情节严重
        if (silence >= 30 * 24 * 60 - 1) {
            report(isReportable, executorBot, sb.append("情节严重，已按最大时间禁言。").toString())

            executorBot.setGroupBan(newbieGroupID, event.sender.id, (30 * 24 * 60 - 1) * 60)
                ?: report(isReportable, executorBot, sb.append("但是机器人执行禁言任务失败了。").toString())
        } else {
            report(isReportable, executorBot, sb.append("正在执行禁言任务。").toString())

            executorBot.setGroupBan(newbieGroupID, event.sender.id, (silence * 60).toInt())
                ?: report(isReportable, executorBot, sb.append("但是机器人执行禁言任务失败了。").toString())
        }

        // 不记录 NR 的调用
        return null
    }

    private fun report(isReportable: Boolean = false, executorBot: Bot? = null, messageText: String) {
        if (isReportable && executorBot != null) {
            executorBot.sendGroupMsg(killerGroupID, messageText, true)
            return
        } else {
            log.info(messageText)
            return
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