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
import com.now.nowbot.service.messageServiceImpl.BPService.BPParam
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
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("BP") class BPService(
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val infoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
    private val restrict: NewbieRestrictService,
    private val commandUtil: CommandUtil
) : MessageService<BPParam>, TencentMessageService<BPParam> {

    data class BPParam(
        val user: OsuUser,
        val historyUser: OsuUser? = null,
        val scores: Map<Int, LazerScore>,
        val isShow: Boolean,
        val isCompact: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BPParam>,
    ): Boolean {
        val matcher = Instruction.BP.matcher(messageText)
        if (!matcher.find()) return false

        val isMultiple = matcher.group("s").isNullOrBlank().not()
        val isShow = matcher.group("w").isNullOrBlank().not()

        val param = getParam(event, matcher, isMultiple, isShow, isCompact = false) ?: return false

        restrict.checkAsync(event, param.scores)

        data.value = param
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: BPParam): ServiceCallStatistic? {
        // param.asyncImage()
        val message: MessageChain = param.getMessageChain()

        try {
            event.replyAsync(message)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw IllegalStateException.Send("最好成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf( param.user.mode),
        )
    }

    override fun accept(event: MessageEvent, messageText: String): BPParam? {
        val matcher1 = OfficialInstruction.BP.matcher(messageText)
        val matcher2 = OfficialInstruction.BPS.matcher(messageText)
        val matcher3 = OfficialInstruction.BP_SHOW.matcher(messageText)

        val matcher: Matcher
        val isMultiple: Boolean
        val isShow: Boolean

        if (matcher3.find()) {
            matcher = matcher3
            isMultiple = false
            isShow = true
        } else if (matcher1.find()) {
            matcher = matcher1
            isMultiple = false
            isShow = false
        } else if (matcher2.find()) {
            matcher = matcher2
            isMultiple = true
            isShow = false
        } else {
            return null
        }

        val param = getParam(event, matcher, isMultiple, isShow, isCompact = true)

        return param
    }

    override fun reply(event: MessageEvent, param: BPParam): MessageChain? = run {
        // param.asyncImage()
        return param.getMessageChain()
    }

    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(event: MessageEvent, matcher: Matcher, isMultiple: Boolean, isShow: Boolean, isCompact: Boolean): BPParam? {
        val any: String = matcher.group(FLAG_ANY) ?: ""

        // 避免指令冲突
        if (any.contains("&sb", ignoreCase = true)) return null

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
        ) ?: throw NoSuchElementException.Player()

        val mode = commandUtil.getMode(context, event, userID)

        commandUtil.addResidualAliases(context)

        val hasCondition = pipeline.hasMatch || context.syntaxes.isNotEmpty()

        val defaultLimit = when {
            hasCondition -> 100
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
            { userApiService.getMicroOsuUser(userID, mode) },
            { scoreApiService.getBestScores(userID, mode, offset, limit) },
        )

        val user: OsuUser = async.first
        val scores: List<LazerScore> = async.second

        if (scores.isEmpty()) {
            if (offset > 0) {
                if (mode.isNotDefault) {
                    throw NoSuchElementException.BestScoreWithModeAndOffset(user.username, mode, offset)
                } else {
                    throw NoSuchElementException.BestScoreWithOffset(user.username, offset)
                }
            } else {
                throw NoSuchElementException.BestScoreWithMode(user.username, mode)
            }
        }

        calculateApiService.applyStarToScores(scores)
        BeatmapUtil.applyBeatmapChanges(scores)
        beatmapApiService.applyBeatmapExtend(scores)
        beatmapApiService.applyAlias(scores)

        val scoresByRank: Map<Int, LazerScore> = buildMap(scores.size) {
            scores.forEachIndexed { index, score -> put(index + offset + 1, score) }
        }

        val classified = scoresByRank.classify(context)

        if (classified.isEmpty()) {
            if (hasCondition) {
                throw NoSuchElementException.BestScoreFiltered(user.username)
            } else {
                throw NoSuchElementException.BestScore(user.username)
            }
        }

        val historyUser = infoDao.getHistoryUser(user)

        return BPParam(user, historyUser, classified, isShow, isCompact)
    }

    private fun BPParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1) {
                val ranks = scores.keys
                val scores = scores.values

                val body = mapOf(
                    "user" to user,
                    "history_user" to historyUser,
                    "scores" to scores,
                    "rank" to ranks,
                    "panel" to "BS",
                    "compact" to ((isCompact && scores.size >= 10) || scores.size > 100)
                )

                MessageChain(imageService.getPanel(body, "A4"))
            } else {
                val (ranking, score) = scores.entries.single()
                score.ranking = ranking

                val e5Param = ScorePRService.getE5ParamForFilteredScore(user, historyUser, score, "B", beatmapApiService, calculateApiService)

                MessageChain(imageService.getPanel(e5Param.toMap(), if (isShow) "E10" else "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }


    private fun BPParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val entries = scores.entries.take(5)
            val ss = entries.map { it.value }
            val list = entries.map { it.key to it.value }

            // beatmapApiService.applyBeatmapExtend(ss)

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            getUUScores(user, list, covers)
        } else {
            val (ranking, score) = scores.entries.single()
            score.ranking = ranking

            val cover = scoreApiService.getCover(score, CoverType.COVER)

            // beatmapApiService.applyBeatmapExtend(s)

            getUUScore(user, score, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPService::class.java)

    }
}
