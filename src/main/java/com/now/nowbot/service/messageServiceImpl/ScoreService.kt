package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRService.Companion.getScore4PanelE5
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.serviceException.BindException
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
            throw if (isMyself.get()) {
                GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
            } else {
                GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
            }
        }

        if (Objects.isNull(user)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }

        val bid = getBid(matcher)
        data.value = ScoreParam(user, mode.data, bid, getMod(matcher), isDefault, isMyself.get())

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: ScoreParam) {
        val message = getMessageChain(param)
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("谱面成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面成绩")
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

        if (bid == 0L) throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)

        // 处理 mods
        val modsStr = param.modsStr

        val score: LazerScore?
        if (StringUtils.hasText(modsStr)) {
            val osuMods = OsuMod.getModsList(modsStr)
            score = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode, osuMods)?.score
            } catch (e: WebClientResponseException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, bid.toString())
            }
            beatmapApiService.applyBeatMapExtend(score)
        } else {
            score = try {
                scoreApiService.getBeatMapScore(bid, user.userID, mode)?.score
            } catch (e: WebClientResponseException.NotFound) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    try {
                        scoreApiService.getBeatMapScore(bid, user.userID, OsuMode.DEFAULT)?.score
                    } catch (e1: WebClientResponseException) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score, bid.toString())
                    }
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_SpecifiedMode, mode!!.getName())
                }
            } catch (e: WebClientResponseException.Unauthorized) {
                if (param.isMyself) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
                }
            }
        }

        val image: ByteArray
        val e5Param = getScore4PanelE5(user, score!!, "S", beatmapApiService)

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
