package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.OsuUser
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
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.ServiceException.MiniCardException
import com.now.nowbot.throwable.ServiceException.ScoreException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import jakarta.annotation.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.max

@Service("PR_CARD")
class ScorePRCardService(
    var bindDao: BindDao? = null,
    var userApiService: OsuUserApiService? = null,
    var imageService: ImageService? = null,
    var scoreApiService: OsuScoreApiService? = null,
    var beatmapApiService: OsuBeatmapApiService? = null,

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

        val offset = max(0.0, range.getValue(0, true).toDouble()).toInt()
        val scores = if (StringUtils.hasText(matcher.group("recent"))) {
            scoreApiService!!.getRecent(range.data!!.userID, mode.data, offset, 1)
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            scoreApiService!!.getRecentIncludingFail(range.data!!.userID, mode.data, offset, 1)
        } else {
            throw MiniCardException(MiniCardException.Type.MINI_Classification_Error)
        }
        if (range.data == null) throw ScoreException(ScoreException.Type.SCORE_Player_NotFound)

        score = if (scores.isNotEmpty()) {
            scores.first()
        } else {
            throw ScoreException(ScoreException.Type.SCORE_Recent_NotFound, range.data!!.username)
        }

        data.setValue(PRCardParam(score))

        return true
    }


    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PRCardParam) {
        val from = event.subject

        val score = param.score

        try {
            from.sendMessage(getMessageChain(score))
        } catch (e: Exception) {
            log.error("迷你成绩面板：发送失败", e)
            throw MiniCardException(MiniCardException.Type.MINI_Send_Error)
        }
    }

    override fun isHandle(event: MessageEvent, messageText: String): PRCardParam? {
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

        val offset = max(0, range.getValue(0, true))

        val scores = if (isRecentAll) {
            scoreApiService!!.getRecentIncludingFail(range.data!!.userID, mode.data, offset, 1)
        } else {
            scoreApiService!!.getRecent(range.data!!.userID, mode.data, offset, 1)
        }

        if (range.data == null) throw ScoreException(ScoreException.Type.SCORE_Player_NotFound)

        if (scores.isNotEmpty()) score = scores.first() else throw ScoreException(ScoreException.Type.SCORE_Recent_NotFound, range.data!!.username)

        return PRCardParam(score)
    }

    override fun getReply(event: MessageEvent, data: PRCardParam): MessageChain? {
        return getMessageChain(data.score)
    }

    private fun getMessageChain(score: Score): MessageChain {
        try {
            beatmapApiService!!.applyBeatMapExtend(score)
        } catch (e: Exception) {
            throw MiniCardException(MiniCardException.Type.MINI_Map_FetchError)
        }

        val image: ByteArray

        try {
            image = imageService!!.getPanelGamma(score)
        } catch (e: Exception) {
            log.error("迷你成绩面板：渲染失败", e)
            throw MiniCardException(MiniCardException.Type.MINI_Render_Error)
        }

        return QQMsgUtil.getImage(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRCardService::class.java)
    }
}