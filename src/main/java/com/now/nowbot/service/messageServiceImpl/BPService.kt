package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.ScoreFilter
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
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
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.*

@Service("BP") class BPService(
    private val calculateApiService: OsuCalculateApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
) : MessageService<BPParam>, TencentMessageService<BPParam> {

    data class BPParam(val user: OsuUser, val scores: Map<Int, LazerScore>)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BPParam>,
    ): Boolean {
        val matcher = Instruction.BP.matcher(messageText)
        if (!matcher.find()) return false

        val any: String? = matcher.group("any")

        // 避免指令冲突
        if (any?.contains("&sb", ignoreCase = true) == true) return false

        val isMyself = AtomicBoolean() // 处理 range
        val mode = getMode(matcher)

        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp")
        range.setZeroToRange100()

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, "$REG_EQUAL|$REG_RANGE".toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_Cabbage)
        }

        val ranges = if (hasRangeInConditions) rangeInConditions else matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

        val range2 = if (range.start != null) {
            range
        } else {
            val start = ranges?.firstOrNull()?.toIntOrNull()
            val end = if (ranges?.size == 2) ranges.last().toIntOrNull() else null

            CmdRange(range.data!!, start, end)
        }

        val isMultiple = matcher.group("s").isNullOrBlank().not()

        val scores = range2.getBPScores(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition)

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, range.data!!.username)
        }

        data.value = BPParam(range.data!!, filteredScores)

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BPParam) {
        param.asyncImage()
        val image: ByteArray = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩")
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

        val isMyself = AtomicBoolean() // 处理 range
        val mode = getMode(matcher)

        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp")
        range.setZeroToRange100()

        val any = matcher.group("any")
        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, "$REG_EQUAL|$REG_RANGE".toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        val ranges = if (hasRangeInConditions) rangeInConditions else matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

        val range2 = if (range.start != null) {
            range
        } else {
            val start = ranges?.firstOrNull()?.toIntOrNull()
            val end = if (ranges?.size == 2) ranges.last().toIntOrNull() else null

            CmdRange(range.data!!, start, end)
        }

        val scores = range2.getBPScores(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition)

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, range.data!!.username)
        }

        return BPParam(range.data!!, filteredScores)
    }

    override fun reply(event: MessageEvent, param: BPParam): MessageChain? = run {
        param.asyncImage()
        return QQMsgUtil.getImage(param.getImage())
    }

    private fun CmdRange<OsuUser>.getBPScores(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Map<Int, LazerScore> {
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

        val isDefault = offset == 0 && limit == 1

        val scores = if (limit > 100) {
            scoreApiService.getBestScores(data!!.userID, mode, offset, 100) + scoreApiService.getBestScores(data!!.userID, mode, offset + 100, limit - 100)
        } else {
            scoreApiService.getBestScores(data!!.userID, mode, offset, limit)
        }

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        val modeStr = if (mode.isDefault()) {
            scores.firstOrNull()?.mode?.fullName ?: this.data?.currentOsuMode?.fullName ?: "默认"
        } else {
            mode.fullName
        }

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isDefault) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_PlayerRecord,
                    modeStr,
                )
            } else {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_ModeBP,
                    data!!.username,
                    modeStr,
                )
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun BPParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackground(scores.values, CoverType.COVER)
        scoreApiService.asyncDownloadBackground(scores.values, CoverType.LIST)
    }

    private fun BPParam.getImage(): ByteArray =
        if (scores.size > 1) {
            val ranks = scores.map{it.key}.toList()
            val scores = scores.map{it.value}.toList()

            val body = mapOf(
                "user" to user,
                "scores" to scores,
                "rank" to ranks,
                "panel" to "BS"
            )

            imageService.getPanel(body, "A4")
        } else {
            val score: LazerScore = scores.toList().first().second
            val beatmap = beatmapApiService.getBeatMap(score.beatMapID)
            score.beatMap = beatmap

            val original = DataUtil.getOriginal(beatmap)

            // calculateApiService.applyBeatMapChanges(score)
            // calculateApiService.applyStarToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            val body = ScorePRService.PanelE5Param(user, score, null, density, progress, original, attributes, "B", null).toMap()

            imageService.getPanel(body, "E5")
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPService::class.java)
    }
}
