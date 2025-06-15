package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPService.BPParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.command.*
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.*

@Service("BP") class BPService(
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
) : MessageService<BPParam>, TencentMessageService<BPParam> {

    data class BPParam(val user: OsuUser, val scores: Map<Int, LazerScore>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BPParam>,
    ): Boolean {
        val matcher = Instruction.BP.matcher(messageText)
        if (!matcher.find()) return false

        val isMultiple = matcher.group("s").isNullOrBlank().not()

        val param = getParam(event, messageText, matcher, isMultiple) ?: return false

        data.value = param
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BPParam) {
        param.asyncImage()
        val image: ByteArray = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw IllegalStateException.Send("最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPParam? {
        val matcher1 = OfficialInstruction.BP.matcher(messageText)
        val matcher2 = OfficialInstruction.BPS.matcher(messageText)

        val matcher: Matcher
        val isMultiple: Boolean

        if (matcher1.find()) {
            matcher = matcher1
            isMultiple = false
        } else if (matcher2.find()) {
            matcher = matcher2
            isMultiple = true
        } else {
            return null
        }

        val param = getParam(event, messageText, matcher, isMultiple)

        return param
    }

    override fun reply(event: MessageEvent, param: BPParam): MessageChain? = run {
        param.asyncImage()
        return QQMsgUtil.getImage(param.getImage())
    }

    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultiple: Boolean): BPParam? {
        val any: String? = matcher.group("any")

        // 避免指令冲突
        if (any?.contains("&sb", ignoreCase = true) == true) return null

        val isMyself = AtomicBoolean(true) // 处理 range
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        id.setZeroToRange100()

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, "$REG_EQUAL|$REG_RANGE".toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())
        }

        val user: OsuUser
        val scores: Map<Int, LazerScore>

        // 高效的获取方式
        if (id.data != null) {
            val id2 = if (id.start != null) {
                id
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(id.data!!, start, end)
            }

            val deferred = scope.async {
                userApiService.getOsuUser(id2.data!!, mode.data!!)
            }

            val deferred2 = scope.async {
                id2.getBestsFromUserID(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition)
            }

            runBlocking {
                user = deferred.await()
                scores = deferred2.await()
            }

            scope.cancel()
        } else {
            // 经典的获取方式

            val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp")
            range.setZeroToRange100()

            val range2 = if (range.start != null) {
                range
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(range.data!!, start, end)
            }

            user = range2.data!!

            scores = range2.getBestsFromOsuUser(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition)
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw NoSuchElementException.BestScoreFiltered(user.username)
        }

        return BPParam(user, scores)
    }

    private fun <T> CmdRange<T>.getOffsetAndLimit(
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Pair<Int, Int> {
        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 200
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        return offset to limit
    }

    private fun CmdRange<Long>.getBestsFromUserID(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getBestScores(this.data!!, mode, offset, limit)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(this.data!!.toString(), mode)
        }

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun CmdRange<OsuUser>.getBestsFromOsuUser(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getBestScores(data!!.userID, mode, offset, limit)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(this.data!!.username, mode)
        }

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun BPParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

    private fun BPParam.getImage(): ByteArray =
        if (scores.size > 1) {
            val ranks = scores.map { it.key }
            val scores = scores.map { it.value }

            val body = mapOf(
                "user" to user,
                "scores" to scores,
                "rank" to ranks,
                "panel" to "BS"
            )

            imageService.getPanel(body, "A4")
        } else {
            val e5Param = ScorePRService.getE5ParamForFilteredScore(user, scores.toList().first().second, "B", beatmapApiService, calculateApiService)

            imageService.getPanel(e5Param.toMap(), "E5")
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPService::class.java)

        private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(4))
    }
}
