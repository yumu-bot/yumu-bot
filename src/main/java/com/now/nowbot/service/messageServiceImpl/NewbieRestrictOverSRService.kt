package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.Permission
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPService.Companion.filterScores
import com.now.nowbot.service.messageServiceImpl.BPService.Filter
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@Service("NEWBIE_RESTRICT")
class NewbieRestrictOverSRService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val botContainer: BotContainer,
): MessageService<List<LazerScore>> {
    // 这里放幻数

    // 新人群、杀手群、执行机器人
    private val newbieGroupID = 231094840L
    private val killerGroupID = 695600319L
    private val executorBotID = 1708547915L

    // 赦免图：No title，竹取飞翔，C type
    private val remitBIDs = listOf(738063L, 86324L, 1620144L)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<List<LazerScore>>
    ): Boolean {
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

                val map = beatmapApiService.getBeatMap(bid)
                val mode = OsuMode.correctConvert(inputMode.data, map.mode)

                user = getUserWithoutRange(event, ss, CmdObject(mode))
                scores = scoreApiService.getBeatMapScores(map.beatMapID, user.userID, mode)
            } else if (s.find()) {
                val bid = getBid(s)
                if (bid == 0L) return false

                val inputMode = getMode(s)
                val mods = getMod(s)

                val map = beatmapApiService.getBeatMap(bid)
                val mode = OsuMode.correctConvert(inputMode.data, map.mode)

                user = getUserWithoutRange(event, s, CmdObject(mode))
                scores = listOf(scoreApiService.getBeatMapScore(map.beatMapID, user.userID, mode, mods)?.score ?: return false)
            } else if (pr.find()) {
                val isMulti = (pr.group("s").isNullOrBlank().not() || pr.group("es").isNullOrBlank().not())

                val offset: Int
                val limit: Int

                val isPass =
                    if (pr.group("recent") != null) {
                        false
                    } else if (pr.group("pass") != null) {
                        true
                    } else {
                        return false
                    }

                val mode = getMode(pr)

                val range = getUserAndRangeWithBackoff(event, pr, mode, AtomicBoolean(), messageText, "recent")
                range.setZeroToRange100()

                if (range.data == null) return false

                if (isMulti) {
                    offset = range.getOffset(0, true)
                    limit = range.getLimit(20, true)
                } else {
                    offset = range.getOffset(0, false)
                    limit = range.getLimit(1, false)
                }

                scores = scoreApiService.getScore(range.data!!.userID, mode.data, offset, limit, isPass)
            } else if (b.find()) {
                val any: String? = b.group("any")

                // 避免指令冲突
                if (any?.contains("&sb", ignoreCase = true) == true) return false

                val isMyself = AtomicBoolean() // 处理 range
                val mode = getMode(b)

                val range = getUserAndRangeWithBackoff(event, b, mode, isMyself, messageText, "bp")
                range.setZeroToRange100()

                val conditions = DataUtil.paramMatcher(any, Filter.entries.map { it.regex }, "$REG_EQUAL|$REG_RANGE".toRegex())

                // 如果不加井号，则有时候范围会被匹配到这里来
                val rangeInConditions = conditions.lastOrNull()
                val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
                val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

                if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
                    return false
                }

                val ranges = if (hasRangeInConditions) rangeInConditions else b.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

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

                    if (hasCondition) {
                        offset = 0
                        limit = 100
                    } else if (isMultiple) {
                        offset = getOffset(0, true)
                        limit = getLimit(20, true)
                    } else {
                        offset = getOffset(0, false)
                        limit = getLimit(1, false)
                    }

                    scoreApiService.getBestScores(range2.data!!.userID, mode.data, offset, limit)
                        .mapIndexed { index: Int, score: LazerScore -> (index + 1) to score }.toMap()
                }

                val filteredScores = filterScores(bs, conditions)

                if (filteredScores.isEmpty()) return false

                scores = filteredScores.map { it.value }
            } else if (m.find()) {
                val bid = getBid(m)

                val beatMap = if (bid != 0L) {
                    beatmapApiService.getBeatMap(bid)
                } else return false

                val mode = OsuMode.getMode(m.group("mode"))
                val mods = LazerMod.getModsList(m.group("mod"))

                // 自己构建成绩
                val constructScore = LazerScore()

                constructScore.beatMap = beatMap
                constructScore.beatMapSet = beatMap.beatMapSet!!
                constructScore.ruleset = mode.modeValue.toByte()
                constructScore.mods = mods

                scores = listOf(constructScore)
            } else if (t.find()) {
                val mode = getMode(t)
                val range = getUserWithRange(event, t, mode, AtomicBoolean())
                range.setZeroDay()
                user = range.data ?: return false

                val dayStart = range.getDayStart()
                val dayEnd = range.getDayEnd()

                val bps = scoreApiService.getBestScores(user.userID, mode.data)
                val laterDay = java.time.OffsetDateTime.now().minusDays(dayStart.toLong())
                val earlierDay = java.time.OffsetDateTime.now().minusDays(dayEnd.toLong())

                scores = bps.filter { it.endedTime.isBefore(laterDay) && it.endedTime.isAfter(earlierDay) }
            } else if (ba.find()) {
                val mode = getMode(ba)
                user = getUserWithoutRange(event, ba, mode, AtomicBoolean())
                scores = scoreApiService.getBestScores(user.userID, mode.data).take(6)
            } else return false
        } catch (e: Exception) {
            return false
        }

        // 虽然主动从 API 获取成绩，可以避免本地计算的失误，但是还是容易出现误操作
        calculateApiService.applyStarToScores(scores, local = true)

        data.value = scores.filterNot {
            remitBIDs.contains(it.beatMap.beatMapID) && LazerMod.noStarRatingChange(it.mods)
        }

        return data.value.isNotEmpty()
    }

    override fun HandleMessage(event: MessageEvent, param: List<LazerScore>) {
        val sr = param.maxOf { it.beatMap.starRating }
        val silence = getSilence(sr)
        if (silence <= 0) return

        val criminal = event.sender
        val message = String.format("%.2f", sr) + " -> " + getSilenceMessage(silence)
        val executorBot = botContainer.robots[executorBotID]
            ?: run {
                log.info("检测到 ${criminal.name} 超星 ($message)，但是执行机器人并未上线。无法执行禁言任务。")
                return
            }

        val isReportable = executorBot.groupList.data?.map { it.groupId }?.contains(killerGroupID) == true

        if (Permission.isGroupAdmin(event)) {
            report(isReportable, executorBot, "检测到 ${criminal.name} 超星 ($message)，但是对方是管理员或群主，无法执行禁言任务。")
            return
        }

        // 情节严重
        if (silence >= 30 * 24 * 60 - 1) {
            report(isReportable, executorBot, "检测到 ${criminal.name} 超星 ($message)，情节严重，已按最大时间禁言。")

            executorBot.setGroupBan(newbieGroupID, event.sender.id, (30 * 24 * 60 - 1) * 60)
                ?: report(isReportable, executorBot, "检测到 ${criminal.name} 超星 ($message)，但是机器人执行禁言任务时失败了。")
        } else {
            report(isReportable, executorBot, "检测到 ${criminal.name} 超星 ($message)，正在执行禁言任务。")

            executorBot.setGroupBan(newbieGroupID, event.sender.id, silence * 60)
                ?: report(isReportable, executorBot, "检测到 ${criminal.name} 超星 ($message)，但是机器人执行禁言任务时失败了。")
        }
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
        private val log: Logger = LoggerFactory.getLogger(NewbieRestrictOverSRService::class.java)

        private fun getSilence(star: Double): Int {
            return if (star <= 5.7) {
                // 未超星
                0
            } else if (star < 6.0) {
                ((star - 5.7) * 1000).roundToInt()
            } else {
                ((star - 5.7) * 2000).roundToInt()
            }
        }

        private fun getSilenceMessage(silence: Int): String {
            return if (silence >= 1440) {
                val day = silence / 1440
                val hour = (silence - (day * 1440)) / 60
                val minute = silence - (day * 1440) - (hour * 60)

                "${day}天${hour}时${minute}分"
            } else if (silence >= 60.0) {
                val hour = silence / 60
                val minute = silence - (hour * 60)

                "${hour}时${minute}分"
            } else {
                "${silence}分"
            }
        }
    }
}