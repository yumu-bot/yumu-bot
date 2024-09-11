package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.ScorePRCardService.PRCardParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.MiniCardException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("PR_CARD")
class ScorePRCardService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,

    ) : MessageService<PRCardParam>, TencentMessageService<PRCardParam> {
    @JvmRecord
    data class PRCardParam(val score: Score)


    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<PRCardParam?>): Boolean {
        val matcher2 = Instruction.DEPRECATED_YMX.matcher(messageText)
        if (matcher2.find()) throw MiniCardException(MiniCardException.Type.MINI_Deprecated_X)

        val matcher = Instruction.PR_CARD.matcher(messageText)
        if (!matcher.find()) return false
        val score: Score?

        val mode = getMode(matcher)
        val range = getUserWithRange(event, matcher, mode, AtomicBoolean())

        val offset = range.getOffset(0, false)
        val scores = if (StringUtils.hasText(matcher.group("recent"))) {
            scoreApiService.getRecent(range.data!!.userID, mode.data, offset, 1)
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            scoreApiService.getRecentIncludingFail(range.data!!.userID, mode.data, offset, 1)
        } else {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Classification, "迷你")
        }
        if (range.data == null) throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)

        score = if (scores.isNotEmpty()) {
            scores.first()
        } else {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_RecentScore, range.data!!.username, mode.data?.name?: "默认")
        }

        data.value = PRCardParam(score)

        return true
    }


    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PRCardParam) {
        val from = event.subject

        val score = param.score
        val message = getMessageChain(score)
        try {
            from.sendMessage(message)
        } catch (e: Exception) {
            log.error("迷你成绩面板：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "迷你")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PRCardParam? {
        var matcher: Matcher
        val isRecentAll: Boolean
        when {
            OfficialInstruction.PR_CARD
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecentAll = false
            }

            OfficialInstruction.RE_CARD
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecentAll = true
            }

            else -> return null
        }

        val score: Score

        val mode = getMode(matcher)
        val range = getUserWithRange(event, matcher, mode, AtomicBoolean())

        val offset = range.getOffset(0, false)

        val scores = if (isRecentAll) {
            scoreApiService.getRecentIncludingFail(range.data!!.userID, mode.data, offset, 1)
        } else {
            scoreApiService.getRecent(range.data!!.userID, mode.data, offset, 1)
        }

        if (range.data == null) throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)

        if (scores.isNotEmpty()) {
            score = scores.first()
        } else {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_RecentScore, range.data!!.username, mode.data?.name ?: "默认")
        }

        return PRCardParam(score)
    }

    override fun reply(event: MessageEvent, param: PRCardParam): MessageChain? {
        return getMessageChain(param.score)
    }

    private fun getMessageChain(score: Score): MessageChain {
        try {
            beatmapApiService.applyBeatMapExtend(score)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_BeatMap)
        }

        val image: ByteArray

        try {
            image = imageService.getPanelGamma(score)
        } catch (e: Exception) {
            log.error("迷你成绩面板：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "迷你")
        }

        return QQMsgUtil.getImage(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRCardService::class.java)
    }
}