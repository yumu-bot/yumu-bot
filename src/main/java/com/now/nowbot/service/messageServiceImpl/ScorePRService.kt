package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.ScoreFilter
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.UUScore
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
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import kotlinx.coroutines.*
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
    private val userApiService: OsuUserApiService,
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

        val isMultiple = (matcher.group("s").isNullOrBlank().not() || matcher.group("es").isNullOrBlank().not())

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("最近成绩分类失败：")
                throw IllegalStateException.ClassCast("最近成绩")
            }

        val param = getParam(event, messageText, matcher, isMultiple, isPass) ?: return false

        data.value = param
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        param.asyncImage()
        val messageChain: MessageChain = param.getMessageChain()

        try {
            event.reply(messageChain)
        } catch (e: Exception) {
            log.error("最近成绩：发送失败", e)
            throw IllegalStateException.Send("最近成绩")
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

        val param = getParam(event, messageText, matcher, isMultiple, isPass)

        return param
    }

    override fun reply(event: MessageEvent, param: ScorePRParam): MessageChain? {
        param.asyncImage()
        return param.getMessageChain()
    }


    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultiple: Boolean, isPass: Boolean): ScorePRParam? {
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


            val async = AsyncMethodExecutor.awaitPairWithMapSupplierExecute(
                { userApiService.getOsuUser(id2.data!!, mode.data!!) },
                { id2.getRecentsFromUserID(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition, isPass) }
            )

            user = async.first
            scores = async.second
        } else {
            // 经典的获取方式

            val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "re")
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

            scores = range2.getRecentsFromOsuUser(mode.data ?: OsuMode.DEFAULT, isMultiple, hasCondition, isPass)
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.PassedScoreFiltered(user.username, user.currentOsuMode)
            } else {
                throw NoSuchElementException.RecentScoreFiltered(user.username, user.currentOsuMode)
            }
        }

        return ScorePRParam(user, filteredScores, isPass)
    }


    private fun <T> CmdRange<T>.getOffsetAndLimit(
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Pair<Int, Int> {
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

        return offset to limit
    }

    private fun CmdRange<Long>.getRecentsFromUserID(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getScore(this.data!!, mode, offset, limit, isPass)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(this.data!!.toString(), mode)
            } else {
                throw NoSuchElementException.PassedScore(this.data!!.toString(), mode)
            }
        }

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun CmdRange<OsuUser>.getRecentsFromOsuUser(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
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

        val scores = scoreApiService.getScore(data!!.userID, mode, offset, limit, isPass)

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(data!!.username, data!!.currentOsuMode)
            } else {
                throw NoSuchElementException.PassedScore(data!!.username, data!!.currentOsuMode)
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun ScorePRParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

    private fun ScorePRParam.getMessageChain(): MessageChain {
        try {
            if (scores.size > 1) {
                val ranks = scores.map { it.key }
                val scores = scores.map { it.value }

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
            .block()!!

        return QQMsgUtil.getTextAndImage(d.scoreLegacyOutput, imgBytes)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRService::class.java)

        // 用于已筛选过的成绩。此时成绩内的谱面是已经计算过的，无需再次计算
        fun getE5ParamForFilteredScore(user: OsuUser, score: LazerScore, panel: String, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService): PanelE5Param {
            val originalBeatMap = beatmapApiService.getBeatmap(score.beatmapID)

            beatmapApiService.applyBeatmapExtend(score, originalBeatMap)

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
            beatmapApiService.applyBeatmapExtend(score)
            return getScore4PanelE5AfterExtended(user, score, position = null, panel, beatmapApiService, calculateApiService)
        }

        fun getE5Param(
            user: OsuUser,
            score: LazerScore,
            beatmap: Beatmap,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            beatmapApiService.applyBeatmapExtend(score, beatmap)
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
            val beatmap = score.beatmap
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
