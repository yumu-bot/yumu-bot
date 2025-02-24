package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
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
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
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
        val beatMap: BeatMap,
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
        if (bid == 0L) throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
        val beatMap = beatmapApiService.getBeatMap(bid)

        val mode = OsuMode.correctConvert(inputMode.data, beatMap.mode)

        val user: OsuUser = try {
            getUserWithoutRange(event, matcher, CmdObject(mode), isMyself)
        } catch (e: BindException) {
            if (isMyself.get() && messageText.lowercase(Locale.getDefault()).contains("score")) {
                log.info("score 退避")
                return false
            }
            throw if (isMyself.get()) {
                GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
            } else {
                GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
            }
        }

        val mods = getMod(matcher)

        data.value = ScoreParam(user, mode, beatMap, mods, isMyself.get(), isMultipleScore)

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
        if (bid == 0L) throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
        val beatMap = beatmapApiService.getBeatMap(bid)

        val mode = OsuMode.correctConvert(inputMode.data, beatMap.mode)
        val user = getUserWithoutRange(event, matcher, CmdObject(mode), isMyself)

        val mods = getMod(matcher)

        return ScoreParam(user, mode, beatMap, mods, isMyself.get(), isMultipleScore)
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
        val b = param.beatMap
        val bid = b.beatMapID

        var scores: List<LazerScore> = try {
            scoreApiService.getBeatMapScores(bid, user.userID, mode)
                .sortedByDescending { it.PP }
        } catch (e: WebClientResponseException.NotFound) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, b.previewName)
        } catch (e: WebClientResponseException.Unauthorized) {
            if (param.isMyself) {
                throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
            }
        }

        if (scores.isEmpty()) { // 当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
            if (b.mode == OsuMode.OSU && mode != OsuMode.OSU && mode != OsuMode.DEFAULT) {
                scores = scoreApiService.getBeatMapScores(bid, user.userID, OsuMode.DEFAULT)
                    .sortedByDescending { it.PP }
            } else {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_SpecifiedMode,
                    mode.getName(),
                )
            }
        }

        if (scores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, b.previewName)
        }

        val image: ByteArray

        try {
            if (scores.size > 1) {
                calculateApiService.applyStarToScores(scores)
                calculateApiService.applyBeatMapChanges(scores)
                beatmapApiService.applyBeatMapExtendForSameScore(scores, b)
                image = imageService.getPanelA5(user, scores, "SS")
            } else {
                val score = scores.first()
                val e5Param = ScorePRService.getScore4PanelE5(user, score, b, null, "S", beatmapApiService, calculateApiService)

                image = imageService.getPanel(e5Param.toMap(), "E5")
            }

            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("成绩：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩")
        }
    }

    private fun getSingleScore(param: ScoreParam): MessageChain {
        val user = param.user

        val b = param.beatMap
        val bid = b.beatMapID
        val mode = param.mode

        val score: LazerScore?
        val position: Int?

        if (param.mods.isNotEmpty()) {
            score = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode, param.mods)?.score
            } catch (e: WebClientResponseException) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_Score,
                    b.previewName,
                )
            }

            beatmapApiService.applyBeatMapExtend(score!!, b)
            position = null
        } else {
            val beatMapScore = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode)
            } catch (e: WebClientResponseException.NotFound) { // 当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (b.mode == OsuMode.OSU && mode != OsuMode.OSU && mode != OsuMode.DEFAULT) {
                    try {
                        scoreApiService.getBeatMapScore(bid, user.userID, OsuMode.DEFAULT)
                    } catch (e1: WebClientResponseException) {
                        throw GeneralTipsException(
                            GeneralTipsException.Type.G_Null_Score,
                            b.previewName,
                        )
                    }
                } else {
                    throw GeneralTipsException(
                        GeneralTipsException.Type.G_Null_SpecifiedMode,
                        mode.getName(),
                    )
                }
            } catch (e: WebClientResponseException.Unauthorized) {
                if (param.isMyself) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
                } else {
                    throw GeneralTipsException(
                        GeneralTipsException.Type.G_TokenExpired_Player
                    )
                }
            }
            score = beatMapScore!!.score!!
            position = beatMapScore.position
        }

        val image: ByteArray

        val e5Param = ScorePRService.getScore4PanelE5(user, score, b, position, "S", beatmapApiService, calculateApiService)

        try {
            image = imageService.getPanel(e5Param.toMap(), "E5")
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("成绩：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
