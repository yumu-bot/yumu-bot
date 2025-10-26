package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
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
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SCORE_PR")
class ScorePRService(
    private val imageService: ImageService,
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    data class ScorePRParam(val user: OsuUser, val scores: Map<Int, LazerScore>, val isPass: Boolean = false, val isShow: Boolean = false)

    data class PanelE5Param(
        val user: OsuUser,
        val score: LazerScore,
        val position: Int?,
        val density: IntArray,
        val progress: Double,
        val original: Map<String, Any>,
        val attributes: Any,
        val panel: String,
        val health: Map<Int, Double>? = null,
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PanelE5Param

            return score.scoreID == other.score.scoreID
        }

        override fun hashCode(): Int {
            return score.scoreID.hashCode()
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
        val isShow = matcher.group("w").isNullOrBlank().not()

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("最近成绩分类失败：")
                throw IllegalStateException.ClassCast("最近成绩")
            }

        val param = getParam(event, messageText, matcher, isMultiple, isPass, isShow) ?: return false

        data.value = param
        return true
    }

    override fun handleMessage(event: MessageEvent, param: ScorePRParam): ServiceCallStatistic? {
        param.asyncImage()
        val messageChain: MessageChain = param.getMessageChain()

        try {
            event.reply(messageChain)
        } catch (e: Exception) {
            log.error("最近成绩：发送失败", e)
            throw IllegalStateException.Send("最近成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.currentOsuMode),
        )
    }

    override fun accept(event: MessageEvent, messageText: String): ScorePRParam? {
        val m1 = OfficialInstruction.SCORE_PASS.matcher(messageText)
        val m2 = OfficialInstruction.SCORE_PASSES.matcher(messageText)
        val m3 = OfficialInstruction.SCORE_RECENT.matcher(messageText)
        val m4 = OfficialInstruction.SCORE_RECENTS.matcher(messageText)
        val m5 = OfficialInstruction.SCORE_PASS_SHOW.matcher(messageText)
        val m6 = OfficialInstruction.SCORE_RECENT_SHOW.matcher(messageText)

        val matcher: Matcher

        val isPass: Boolean
        val isMultiple: Boolean
        val isShow: Boolean

        if (m5.find()) {
            matcher = m5
            isPass = true
            isMultiple = false
            isShow = true
        } else if (m6.find()) {
            matcher = m6
            isPass = false
            isMultiple = false
            isShow = true
        } else if (m1.find()) {
            matcher = m1
            isPass = true
            isMultiple = false
            isShow = false
        } else if (m2.find()) {
            matcher = m2
            isPass = true
            isMultiple = true
            isShow = false
        } else if (m3.find()) {
            matcher = m3
            isPass = false
            isMultiple = false
            isShow = false
        } else if (m4.find()) {
            matcher = m4
            isPass = false
            isMultiple = true
            isShow = false
        } else {
            return null
        }

        val param = getParam(event, messageText, matcher, isMultiple, isPass, isShow)

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
    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultiple: Boolean, isPass: Boolean, isShow: Boolean): ScorePRParam? {
        val any: String = matcher.group(FLAG_ANY) ?: ""

        // 避免指令冲突
        if (any.contains("&sb", ignoreCase = true)) return null

        val isMyself = AtomicBoolean(true) // 处理 range
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        id.setZeroToRange100()

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, REG_RANGE.toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)
        }?.split(REG_HYPHEN.toRegex())

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


            val async = AsyncMethodExecutor.awaitPairCallableExecute(
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

        beatmapApiService.applyBeatmapExtend(scores.map { it.value })

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.PassedScoreFiltered(user.username, user.currentOsuMode)
            } else {
                throw NoSuchElementException.RecentScoreFiltered(user.username, user.currentOsuMode)
            }
        }

        return ScorePRParam(user, filteredScores, isPass, isShow)
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
                throw NoSuchElementException.PassedScore(this.data!!.toString(), mode)
            } else {
                throw NoSuchElementException.RecentScore(this.data!!.toString(), mode)
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
                throw NoSuchElementException.PassedScore(data!!.username, data!!.currentOsuMode)
            } else {
                throw NoSuchElementException.RecentScore(data!!.username, data!!.currentOsuMode)
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun ScorePRParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackgroundFromScores(scores.values, listOf(CoverType.COVER, CoverType.LIST))
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
                return MessageChain(image)
            } else {
                // 单成绩发送
                val pair = scores.toList().first()

                val score: LazerScore = pair.second
                score.ranking = pair.first

                val e5 = getE5ParamForFilteredScore(user, score, (if (isPass) "P" else "R"), beatmapApiService, calculateApiService)

                return MessageChain(imageService.getPanel(e5.toMap(), if (isShow) "E10" else "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }

    private fun ScorePRParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val list = scores.toList().take(5)
            val ss = list.map { it.second }

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            calculateApiService.applyPPToScores(ss)

            getUUScores(user, list, covers)
        } else {

            val s = scores.toList().first().second

            val cover = scoreApiService.getCover(s, CoverType.COVER)

            AsyncMethodExecutor.awaitPairCallableExecute (
                { beatmapApiService.applyBeatmapExtend(s) },
                { calculateApiService.applyPPToScore(s) },
            )

            getUUScore(user, s, cover)
        }
    }

    /*
    private fun ScorePRParam.getUUMessage(): MessageChain {
        val score = scores.values.first()

        val d = UUScore(score, beatmapApiService, calculateApiService)

        val imgBytes = osuApiWebClient.get()
            .uri(d.url ?: "")
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()!!

        return MessageChain(d.scoreLegacyOutput, imgBytes)
    }

     */

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

            return PanelE5Param(user, score, score.ranking, density, progress, original, attributes, panel)

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
            return getE5ParamAfterExtended(user, score, score.ranking, panel, beatmapApiService, calculateApiService)
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
            return getE5ParamAfterExtended(user, score, position ?: score.ranking, panel, beatmapApiService, calculateApiService)
        }

        private fun getE5ParamAfterExtended(
            user: OsuUser,
            score: LazerScore,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            val beatmap = score.beatmap
            val original = DataUtil.getOriginal(beatmap)

            AsyncMethodExecutor.awaitRunnableExecute(
                listOf(
                    AsyncMethodExecutor.Runnable { calculateApiService.applyPPToScore(score) },
                    AsyncMethodExecutor.Runnable { calculateApiService.applyBeatMapChanges(score) },
                    AsyncMethodExecutor.Runnable { calculateApiService.applyStarToScore(score) },
                )
            )

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, position ?: score.ranking, density, progress, original, attributes, panel)
        }
    }
}
