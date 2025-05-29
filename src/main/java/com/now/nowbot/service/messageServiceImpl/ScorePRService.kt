package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.UUScore
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.ScoreFilter
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SCORE_PR")
class ScorePRService(
    private val osuApiWebClient: WebClient,
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    data class ScorePRParam(val user: OsuUser, val scores: Map<Int, LazerScore>, val isPass: Boolean = false)

    data class PanelE5Param(
        val user: OsuUser,
        val score: LazerScore,
        val position: Int?,
        val density: IntArray,
        val progress: Double,
        val original: Map<String, Any>,
        val attributes: Any,
        val panel: String,
        val health: Map<Int, Double>?
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf(
                "user" to user,
                "score" to score,
                "density" to density,
                "progress" to progress,
                "original" to original,
                "attributes" to attributes,
                "panel" to panel,
            )

            if (position != null) {
                out["position"] = position
            }

            if (health != null) {
                out["health"] = mapOf(
                    "time" to health.map { it.key },
                    "percent" to health.map { it.value }
                )
            }

            return out
        }
    }

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<ScorePRParam>
    ): Boolean {
        val matcher = Instruction.SCORE_PR.matcher(messageText)
        if (!matcher.find()) return false

        val any: String? = matcher.group("any")

        // 避免指令冲突
        if (any?.contains("&sb", ignoreCase = true) == true) return false

        val isPass =
                if (matcher.group("recent") != null) {
                    false
                } else if (matcher.group("pass") != null) {
                    true
                } else {
                    log.error("成绩分类失败：")
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "成绩")
                }

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)

        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

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

        val isMultiple = (matcher.group("s").isNullOrBlank().not() || matcher.group("es").isNullOrBlank().not())

        val isAvoidance: Boolean = CmdUtil.isAvoidance(messageText, "recent")

        val scores = range2.getRecentScores(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition, isPass, isAvoidance)

        if (scores.isEmpty() && isAvoidance) {
            return false
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterRecent, range.data!!.username)
        }

        data.value = ScorePRParam(range.data!!, filteredScores, isPass)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        param.asyncImage()
        val messageChain: MessageChain = param.getMessageChain()

        try {
            event.reply(messageChain)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): ScorePRParam? {
        val m1 = OfficialInstruction.SCORE_PASS.matcher(messageText)
        val m2 = OfficialInstruction.SCORE_PASSES.matcher(messageText)
        val m3 = OfficialInstruction.SCORE_RECENT.matcher(messageText)
        val m4 = OfficialInstruction.SCORE_RECENTS.matcher(messageText)

        val matcher: Matcher

        val isPass: Boolean
        val isMultiple: Boolean

        if (m1.find()) {
            matcher = m1
            isPass = true
            isMultiple = false
        } else if (m2.find()) {
            matcher = m2
            isPass = true
            isMultiple = true
        } else if (m3.find()) {
            matcher = m3
            isPass = false
            isMultiple = false
        } else if (m4.find()) {
            matcher = m4
            isPass = false
            isMultiple = true
        } else {
            return null
        }

        val any: String? = matcher.group("any")

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)

        val range = getUserWithRange(event, matcher, mode, isMyself)

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

        val scores = range2.getRecentScores(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition, isPass, false)

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterRecent, range.data!!.username)
        }

        return ScorePRParam(range.data!!, filteredScores, isPass)
    }

    override fun reply(event: MessageEvent, param: ScorePRParam): MessageChain? {
        return param.getMessageChain()
    }

    private fun CmdRange<OsuUser>.getRecentScores(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
        isAvoidance: Boolean = false,
    ): Map<Int, LazerScore> {

        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        val isDefault = offset == 0 && limit == 1

        val scores = try {
            scoreApiService.getScore(data!!.userID, mode, offset, limit, isPass)
        } catch (e: Exception) {
            if (isAvoidance) {
                log.info("最近成绩：退避成功。请求者：{}", data?.username)
                return emptyMap()
            } else {
                throw e
            }
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
                    GeneralTipsException.Type.G_Null_Recent,
                    modeStr,
                )
            } else {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_ModeRecent,
                    data!!.username,
                    modeStr,
                )
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun ScorePRParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackground(scores.values)
        scoreApiService.asyncDownloadBackground(scores.values, CoverType.LIST)
    }

    private fun ScorePRParam.getMessageChain(): MessageChain {
        try {
            if (scores.size > 1) {
                val ranks = scores.map{it.key}.toList()
                val scores = scores.map{it.value}.toList()

                scoreApiService.asyncDownloadBackground(scores, CoverType.LIST)
                scoreApiService.asyncDownloadBackground(scores, CoverType.COVER)

                val body = mapOf(
                    "user" to user,
                    "score" to scores,
                    "rank" to ranks,
                    "panel" to if (isPass) "PS" else "RS"
                )

                calculateApiService.applyPPToScores(scores)

                val image = imageService.getPanel(body, "A5")
                return QQMsgUtil.getImage(image)
            } else {
                // 单成绩发送
                val score = scores.values.first()

                scoreApiService.asyncDownloadBackground(score, CoverType.LIST)
                scoreApiService.asyncDownloadBackground(score, CoverType.COVER)

                val e5 = getE5ParamForFilteredScore(user, score, (if (isPass) "P" else "R"), beatmapApiService, calculateApiService)

                return QQMsgUtil.getImage(imageService.getPanel(e5.toMap(), "E5"))
            }
        } catch (e: Exception) {
            return getUUMessage()
        }
    }

    private fun ScorePRParam.getUUMessage(): MessageChain {
        val score = scores.values.first()

        val d = UUScore(score, beatmapApiService, calculateApiService)

        val imgBytes = osuApiWebClient.get()
            .uri(d.url ?: "")
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()

        return QQMsgUtil.getTextAndImage(d.scoreLegacyOutput, imgBytes)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRService::class.java)

        // 用于已筛选过的成绩。此时成绩内的谱面是已经计算过的，无需再次计算
        fun getE5ParamForFilteredScore(user: OsuUser, score: LazerScore, panel: String, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService): PanelE5Param {
            val originalBeatMap = beatmapApiService.getBeatMap(score.beatMapID)

            beatmapApiService.applyBeatMapExtend(score, originalBeatMap)

            val original = DataUtil.getOriginal(originalBeatMap)

            calculateApiService.applyPPToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(originalBeatMap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, null, density, progress, original, attributes, panel, null)

        }

        // 用于未筛选过的成绩。此时成绩的谱面还需要重新计算
        fun getE5Param(
            user: OsuUser,
            score: LazerScore,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score)
            return getScore4PanelE5AfterExtended(user, score, position = null, panel, beatmapApiService, calculateApiService)
        }

        fun getE5Param(
            user: OsuUser,
            score: LazerScore,
            beatMap: BeatMap,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score, beatMap)
            return getScore4PanelE5AfterExtended(user, score, position, panel, beatmapApiService, calculateApiService)
        }

        private fun getScore4PanelE5AfterExtended(
            user: OsuUser,
            score: LazerScore,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            val beatmap = score.beatMap
            val original = DataUtil.getOriginal(beatmap)

            calculateApiService.applyPPToScore(score)
            calculateApiService.applyBeatMapChanges(score)
            calculateApiService.applyStarToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, position, density, progress, original, attributes, panel, null)
        }
    }
}
