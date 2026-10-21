package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreClassifier.Companion.classify
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.NewbieRestrictService
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.LazerModExtractor
import com.now.nowbot.util.command.OsuModeExtractor
import com.now.nowbot.util.command.QQExtractor
import com.now.nowbot.util.command.RangeExtractor
import com.now.nowbot.util.command.ScoreClassifierExtractor
import com.now.nowbot.util.command.StripPipeline
import com.now.nowbot.util.command.UserIDExtractor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("SCORE_PR")
class ScorePRService(
    private val imageService: ImageService,
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val infoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
    private val commandUtil: CommandUtil,
    private val restrict: NewbieRestrictService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    data class ScorePRParam(
        val user: OsuUser,
        val history: OsuUser? = null,
        val scores: Map<Int, LazerScore>,
        val isPass: Boolean = false,
        val isShow: Boolean = false,
        val isCompact: Boolean = false
    )

    data class PanelE5Param(
        val user: OsuUser,
        val history: OsuUser? = null,
        val score: LazerScore,
        val position: Int?,
        val density: IntArray,
        val progress: Double,
        val original: Map<String, Any>,
        val attributes: Any?,
        val panel: String,
        val health: Map<Int, Double>? = null,
    ) {
        fun toMap(): Map<String, Any?> {
            val out = mapOf(
                "user" to user,
                "score" to score,
                "history_user" to history,
                "density" to density,
                "progress" to progress,
                "original" to original,
                "attributes" to attributes,
                "panel" to panel,
                "position" to position,
                "health" to mapOf(
                    "time" to health?.keys,
                    "percent" to health?.values
                )
            )

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
            matcher.group("recent") == null && if (matcher.group("pass") != null) {
                true
            } else {
                log.error("最近成绩分类失败：")
                throw IllegalStateException.ClassCast("最近成绩")
            }


        val param = getParam(event, matcher, isMultiple, isPass, isShow, isCompact = false) ?: return false

        restrict.checkAsync(event, param.scores)

        data.value = param
        return true
    }

    override fun handleMessage(event: MessageEvent, param: ScorePRParam): ServiceCallStatistic? {
        // param.asyncImage()
        val messageChain: MessageChain = param.getMessageChain()

        try {
            event.replyAsync(messageChain)
        } catch (e: Exception) {
            log.error("最近成绩：发送失败", e)
            throw IllegalStateException.Send("最近成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.mode),
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

        val param = getParam(event, matcher, isMultiple, isPass, isShow, isCompact = true)

        return param
    }

    override fun reply(event: MessageEvent, param: ScorePRParam): MessageChain? {
        // param.asyncImage()
        return param.getMessageChain()
    }


    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(
        event: MessageEvent,
        matcher: Matcher,
        isMultiple: Boolean,
        isPass: Boolean,
        isShow: Boolean,
        isCompact: Boolean
    ): ScorePRParam? {
        val any: String = matcher.group(FLAG_ANY) ?: ""

        // 避免指令冲突
        if (any.contains("&sb", ignoreCase = true)) return null

        // 指令已经把玩家名、模式、范围、条件全部收进 any 里，这里统一剥离。
        val pipeline = StripPipeline(any)
            .apply(OsuModeExtractor)
            .apply(LazerModExtractor)
            .apply(UserIDExtractor)
            .apply(QQExtractor)
            .apply(ScoreClassifierExtractor)
            .apply(RangeExtractor)
            .trim()

        val context = pipeline.context

        val userID = commandUtil.getUserID(
            context,
            event,
            onNull = { isMyself ->
                if (isMyself) {
                    throw BindException.NotBindException.YouNotBind()
                } else {
                    throw BindException.NotBindException.UserNotBind()
                }
            },
        ) ?: throw BindException.NotBindException.UserNotBind()

        // 新解析器只回退到群模式；个人模式仍按绑定里的模式补，保持和旧指令一致。
        val mode = commandUtil.getMode(context, event)
            .takeUnless { it == OsuMode.DEFAULT }
            ?: bindDao.getBindModeFromID(userID)?.takeUnless { it == OsuMode.DEFAULT }
            ?: OsuMode.DEFAULT

        val hasCondition = pipeline.hasMatch

        // 和旧的 InstructionRange.getOffsetAndLimit 对齐：
        // 有筛选条件且未给范围时，先取 100 条回来筛；#0 取 100 条。
        val defaultLimit = when {
            hasCondition -> 200
            isMultiple -> 20
            else -> 1
        }

        val (offset, limit) = commandUtil.getRange(
            context,
            defaultOffset = 0,
            defaultLimit = defaultLimit,
            multiple = isMultiple,
            max = 200,
        )

        val async = AsyncMethodExecutor.awaitPair(
            { userApiService.getOsuUser(userID, mode) },
            { scoreApiService.getScore(userID, mode, offset, limit, isPass) },
        )

        val user: OsuUser = async.first
        val scores: List<LazerScore> = async.second

        if (scores.isEmpty()) {
            if (isPass) {
                if (offset > 0) {
                    throw NoSuchElementException.PassedScoreWithOffset(user.username, mode, offset)
                } else {
                    throw NoSuchElementException.PassedScore(user.username, mode)
                }
            } else {
                if (offset > 0) {
                    throw NoSuchElementException.RecentScoreWithOffset(user.username, mode, offset)
                } else {
                    throw NoSuchElementException.RecentScore(user.username, mode)
                }
            }
        }

        calculateApiService.applyStarToScores(scores)
        BeatmapUtil.applyBeatmapChanges(scores)

        beatmapApiService.applyBeatmapExtend(scores)

        // 别名现在要参与筛选，必须在 classify 之前挂到 beatmapset 上
        beatmapApiService.applyAlias(scores)

        // 必须先按原数组（含 offset）把「名次 -> 成绩」固化下来，再交给 classify 筛选。
        // classify 的 Map 重载会按成绩筛值、键（名次）原样保留，因此不会丢掉原数组的位置信息。
        val scoresByRank: Map<Int, LazerScore> = buildMap(scores.size) {
            scores.forEachIndexed { index, score -> put(index + offset + 1, score) }
        }

        val filteredScores = scoresByRank.classify(context)

        if (filteredScores.isEmpty()) {
            if (hasCondition) {
                if (isPass) {
                    throw NoSuchElementException.PassedScore(user.username, user.mode)
                } else {
                    throw NoSuchElementException.RecentScore(user.username, user.mode)
                }
            } else {
                if (isPass) {
                    throw NoSuchElementException.PassedScoreFiltered(user.username, user.mode)
                } else {
                    throw NoSuchElementException.RecentScoreFiltered(user.username, user.mode)
                }
            }
        }

        val historyUser = infoDao.getHistoryUser(user)

        return ScorePRParam(user, historyUser, filteredScores, isPass, isShow, isCompact)
    }


//    private fun ScorePRParam.asyncImage() = run {
//        scoreApiService.asyncDownloadBackgroundFromScores(scores.values, listOf(CoverType.COVER, CoverType.LIST))
//    }

    private fun ScorePRParam.getMessageChain(): MessageChain {
        try {
            if (scores.size > 1) {
                val ranks = scores.keys
                val scores = scores.values

                val body = mapOf(
                    "user" to user,
                    "history_user" to history,
                    "scores" to scores,
                    "rank" to ranks,
                    "panel" to if (isPass) "PS" else "RS",
                    "compact" to ((isCompact && scores.size >= 10) || scores.size > 100)
                )

                calculateApiService.applyPPToScores(scores)

                val image = imageService.getPanel(body, "A5")
                return MessageChain(image)
            } else {
                // 单成绩发送
                val (ranking, score) = scores.entries.single()

                score.ranking = ranking

                val e5 = getE5ParamForFilteredScore(user, history, score, (if (isPass) "P" else "R"), beatmapApiService, calculateApiService)

                return MessageChain(imageService.getPanel(e5.toMap(), if (isShow) "E10" else "E5"))
            }
        } catch (e: Exception) {
            log.error("最近成绩：渲染失败", e)
            return getUUMessageChain()
        }
    }

    private fun ScorePRParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val list = scores.entries.take(5).map { Pair(it.key, it.value) }
            val ss = list.map { it.second }

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            calculateApiService.applyPPToScores(ss)

            getUUScores(user, list, covers)
        } else {
            val (ranking, score) = scores.entries.single()
            score.ranking = ranking

            val cover = scoreApiService.getCover(score, CoverType.COVER)

            AsyncMethodExecutor.awaitPair (
                { beatmapApiService.applyBeatmapExtend(score) },
                { calculateApiService.applyPPToScore(score) },
            )

            getUUScore(user, score, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRService::class.java)

        // 用于已筛选过的成绩。此时成绩内的谱面是已经计算过的，无需再次计算
        fun getE5ParamForFilteredScore(user: OsuUser, history: OsuUser? = null, score: LazerScore, panel: String, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService): PanelE5Param {
            //beatmapApiService.applyBeatmapExtend(score)

            val original = score.beatmap.originalDetails.toMap()

            calculateApiService.applyPPToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(score.beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, history, score, score.ranking, density, progress, original, attributes, panel)

        }

        // 用于未筛选过的成绩。此时成绩的谱面还需要重新计算
        fun getE5Param(
            user: OsuUser,
            history: OsuUser? = null,
            score: LazerScore,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService
        ): PanelE5Param {
            beatmapApiService.applyBeatmapExtend(score)
            beatmapApiService.applyAlias(score)
            return getE5ParamAfterExtended(user, history, score, score.ranking, panel, beatmapApiService, calculateApiService)
        }

        private fun getE5ParamAfterExtended(
            user: OsuUser,
            history: OsuUser? = null,
            score: LazerScore,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            val beatmap = score.beatmap
            val original = BeatmapUtil.getDetailMap(beatmap)

            // 同一份成绩对象的就地加工必须串行（applyBeatmapChanges 是就地叠加，不能与其他步骤交叉）
            calculateApiService.applyPPToScore(score)
            BeatmapUtil.applyBeatmapChanges(score)
            calculateApiService.applyStarToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, history, score, position ?: score.ranking, density, progress, original, attributes, panel)
        }
    }
}
