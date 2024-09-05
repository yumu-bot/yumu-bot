package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.BeatmapUserScore
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.ScorePRService.Companion.getScore4PanelE5
import com.now.nowbot.service.MessageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.throwable.ServiceException.BindException
import com.now.nowbot.throwable.ServiceException.ScoreException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("SCORE")
class ScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    data class ScoreParam(
        val user: OsuUser?,
        val mode: OsuMode?,
        val bid: Long,
        val modsStr: String,
        val isDefault: Boolean,
        val isMyself: Boolean
    )

    @Throws(TipsException::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<ScoreParam>): Boolean {
        val matcher = Instruction.SCORE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val mode = getMode(matcher)
        val isMyself = AtomicBoolean(false)
        val isDefault = OsuMode.isDefaultOrNull(mode.data)
        val user: OsuUser?
        try {
            user = getUserWithOutRange(event, matcher, mode, isMyself)
        } catch (e: BindException) {
            if (isMyself.get() && messageText.lowercase(Locale.getDefault()).contains("score")) {
                log.info("score 退避")
                return false
            }
            throw if (isMyself.get()) ScoreException(ScoreException.Type.SCORE_Me_TokenExpired) else ScoreException(
                ScoreException.Type.SCORE_Player_TokenExpired
            )
        }

        if (Objects.isNull(user)) {
            throw ScoreException(ScoreException.Type.SCORE_Me_TokenExpired)
        }

        val bid = getBid(matcher)
        data.value = ScoreParam(user, mode.data, bid, getMod(matcher), isDefault, isMyself.get())

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: ScoreParam) {
        val from = event.subject
        val message = getMessageChain(param)
        try {
            from.sendMessage(message)
        } catch (e: Exception) {
            log.error("成绩：发送失败", e)
            throw ScoreException(ScoreException.Type.SCORE_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): ScoreParam? {
        val matcher = OfficialInstruction.SCORE.matcher(messageText)
        if (!matcher.find()) return null


        val mode = getMode(matcher)
        val isMyself = AtomicBoolean(false)
        val isDefault = OsuMode.isDefaultOrNull(mode.data)
        val user = getUserWithOutRange(event, matcher, mode, isMyself)

        val bid = getBid(matcher)
        return ScoreParam(user, mode.data, bid, getMod(matcher), isDefault, isMyself.get())
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return getMessageChain(param)
    }

    private fun getMessageChain(param: ScoreParam): MessageChain {
        val mode = param.mode
        val user = param.user!!
        val isDefault = param.isDefault

        val bid = param.bid

        // 处理 mods
        val modsStr = param.modsStr

        val score: Score?
        if (StringUtils.hasText(modsStr)) {
            val scoreall: BeatmapUserScore
            val osuMods = OsuMod.getModsList(modsStr)
            try {
                scoreall = scoreApiService.getScore(bid, user!!.userID, mode, osuMods)
                score = scoreall.score
            } catch (e: WebClientResponseException) {
                throw ScoreException(ScoreException.Type.SCORE_Score_NotFound, bid.toString())
            }
            beatmapApiService.applyBeatMapExtend(score)
        } else {
            score = try {
                scoreApiService.getScore(bid, user!!.userID, mode).score
            } catch (e: WebClientResponseException.NotFound) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    try {
                        scoreApiService.getScore(bid, user!!.userID, OsuMode.DEFAULT).score
                    } catch (e1: WebClientResponseException) {
                        throw ScoreException(ScoreException.Type.SCORE_Mode_NotFound)
                    }
                } else {
                    throw ScoreException(ScoreException.Type.SCORE_Mode_SpecifiedNotFound, mode!!.getName())
                }
            } catch (e: WebClientResponseException.Unauthorized) {
                if (param.isMyself) {
                    throw ScoreException(ScoreException.Type.SCORE_Me_TokenExpired)
                } else {
                    throw ScoreException(ScoreException.Type.SCORE_Player_TokenExpired)
                }
            }
        }

        val image: ByteArray
        val e5Param = getScore4PanelE5(user, score!!, beatmapApiService)

        try {
            image = imageService.getPanelE5(e5Param)
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("成绩：渲染失败", e)
            throw ScoreException(ScoreException.Type.SCORE_Render_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
