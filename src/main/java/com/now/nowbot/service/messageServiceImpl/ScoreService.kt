package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SCORE") class ScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    data class ScoreParam(
        val user: OsuUser,
        val mode: OsuMode,
        val beatmap: Beatmap,
        val mods: List<LazerMod>,
        val isMyself: Boolean,
        val isMultipleScore: Boolean,
    )

    @Throws(TipsException::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<ScoreParam>,
    ): Boolean {
        val m2 = Instruction.SCORES.matcher(messageText)
        val m = Instruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean

        val matcher: Matcher

        if (m2.find()) {
            matcher = m2
            isMultipleScore = true
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
        } else {
            return false
        }

        val inputMode = getMode(matcher)
        val isMyself = AtomicBoolean(false)

        val bid = getBid(matcher)

        val map = if (bid != 0L) {
            beatmapApiService.getBeatMap(bid)
        } else {
            // 备用方法
            val currentMode = CmdObject(OsuMode.DEFAULT)

            val user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, isMyself, messageText, "score")

            val score = scoreApiService.getRecentScore(user.userID, currentMode.data!!, 0, 1).first()

            beatmapApiService.getBeatMap(score.beatmapID)
        }

        if (!map.hasLeaderBoard) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_LeaderBoard, map.previewName)
        }

        val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

        val user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, CmdObject(mode), isMyself, messageText, "score")

        val mods = getMod(matcher)

        data.value = ScoreParam(user, mode, map, mods, isMyself.get(), isMultipleScore)

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: ScoreParam) {
        val message = if (param.isMultipleScore) {
            getMultipleScore(param)
        } else {
            getSingleScore(param)
        }

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("谱面成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): ScoreParam? {
        val m2 = OfficialInstruction.SCORES.matcher(messageText)
        val m = OfficialInstruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val matcher: Matcher

        if (m2.find()) {
            matcher = m2
            isMultipleScore = true
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
        } else {
            return null
        }

        val inputMode = getMode(matcher)
        val isMyself = AtomicBoolean(false)

        val bid = getBid(matcher)

        val map = if (bid != 0L) {
            beatmapApiService.getBeatMap(bid)
        } else {
            // 备用方法
            val currentMode = CmdObject(OsuMode.DEFAULT)
            val user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, isMyself, messageText, "score")

            val score = scoreApiService.getRecentScore(user.userID, currentMode.data!!, 0, 1).first()

            beatmapApiService.getBeatMap(score.beatmapID)
        }

        if (!map.hasLeaderBoard) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_LeaderBoard, map.previewName)
        }

        val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

        val user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, CmdObject(mode), isMyself, messageText, "score")

        val mods = getMod(matcher)

        return ScoreParam(user, mode, map, mods, isMyself.get(), isMultipleScore)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return if (param.isMultipleScore) {
            getMultipleScore(param)
        } else {
            getSingleScore(param)
        }
    }

    private fun getMultipleScore(param: ScoreParam): MessageChain {
        val mode = param.mode
        val user = param.user
        val b = param.beatmap
        val bid = b.beatmapID

        val scores: List<LazerScore> =
            scoreApiService.getBeatMapScores(bid, user.userID, mode).sortedByDescending { it.pp }

        if (scores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, b.previewName)
        }

        val image: ByteArray = if (scores.size > 1) {
            calculateApiService.applyStarToScores(scores)
            calculateApiService.applyBeatMapChanges(scores)
            beatmapApiService.applyBeatMapExtendForSameScore(scores, b)

            scoreApiService.asyncDownloadBackground(scores.first(), CoverType.COVER)
            scoreApiService.asyncDownloadBackground(scores.first(), CoverType.LIST)

            val body = mapOf(
                "user" to user,

                "rank" to (1..(scores.size)).toList(),
                "score" to scores,
                "panel" to "SS"
            )

            imageService.getPanel(body, "A5")
        } else {
            val score = scores.first()

            val e5Param = ScorePRService.getE5Param(user, score, b, null, "S", beatmapApiService, calculateApiService)

            scoreApiService.asyncDownloadBackground(score, CoverType.COVER)
            scoreApiService.asyncDownloadBackground(score, CoverType.LIST)

            imageService.getPanel(e5Param.toMap(), "E5")
        }

        return QQMsgUtil.getImage(image)
    }

    private fun getSingleScore(param: ScoreParam): MessageChain {
        val user = param.user

        val b = param.beatmap
        val bid = b.beatmapID
        val mode = param.mode

        val score: LazerScore
        val position: Int?

        if (param.mods.isNotEmpty()) {
            score = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode, param.mods)?.score
            } catch (e: WebClientResponseException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, b.previewName)
            }!!

            // beatmapApiService.applyBeatMapExtend(score!!, b)
            position = null
        } else {
            val beatmapScore = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode)!!
            } catch (e: WebClientResponseException.NotFound) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, b.previewName)
            }

            score = beatmapScore.score!!
            position = beatmapScore.position
        }

        val e5Param = ScorePRService.getE5Param(user, score, b, position, "S", beatmapApiService, calculateApiService)

        scoreApiService.asyncDownloadBackground(score, CoverType.COVER)
        scoreApiService.asyncDownloadBackground(score, CoverType.LIST)

        val image: ByteArray = imageService.getPanel(e5Param.toMap(), "E5")

        return QQMsgUtil.getImage(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
