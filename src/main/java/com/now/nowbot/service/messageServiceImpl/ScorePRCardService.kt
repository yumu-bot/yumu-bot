package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRCardService.PRCardParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("PR_CARD")
class ScorePRCardService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService

    ) : MessageService<PRCardParam>, TencentMessageService<PRCardParam> {
    @JvmRecord
    data class PRCardParam(val score: LazerScore)


    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<PRCardParam>): Boolean {
        val matcher = Instruction.PR_CARD.matcher(messageText)
        if (!matcher.find()) return false
        val score: LazerScore?

        val mode = getMode(matcher)
        val range = getUserWithRange(event, matcher, mode, AtomicBoolean())
        range.setZeroToRange100()

        val offset = range.getOffset(0, false)
        val scores = if (matcher.group("recent").isNullOrBlank().not()) {
            scoreApiService.getPassedScore(range.data!!.userID, mode.data, offset, 1)
        } else if (matcher.group("pass").isNullOrBlank().not()) {
            scoreApiService.getRecentScore(range.data!!.userID, mode.data, offset, 1)
        } else {
            throw IllegalStateException.ClassCast("迷你")
        }
        if (range.data == null) throw NoSuchElementException.Player()

        score = if (scores.isNotEmpty()) {
            scores.first()
        } else {
            throw NoSuchElementException.RecentScore(range.data!!.username, mode.data!!)
        }

        calculateApiService.applyPPToScore(score)
        calculateApiService.applyBeatMapChanges(score)
        calculateApiService.applyStarToScore(score)

        data.value = PRCardParam(score)
        return true
    }


    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PRCardParam) {
        val score = param.score

        val message = getMessageChain(score)
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("迷你成绩面板：发送失败", e)
            throw IllegalStateException.Send("迷你")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PRCardParam? {
        var matcher: Matcher
        val isRecent: Boolean
        when {
            OfficialInstruction.PR_CARD
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = false
            }

            OfficialInstruction.RE_CARD
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = true
            }

            else -> return null
        }

        val mode = getMode(matcher)
        val range = getUserWithRange(event, matcher, mode, AtomicBoolean())

        val offset = range.getOffset(0, false)

        val scores = if (isRecent) {
            scoreApiService.getRecentScore(range.data!!.userID, mode.data, offset, 1)
        } else {
            scoreApiService.getPassedScore(range.data!!.userID, mode.data, offset, 1)
        }

        if (scores.isEmpty()) {
            throw NoSuchElementException.RecentScore(range.data!!.username, mode.data!!)
        }

        return PRCardParam(scores.first())
    }

    override fun reply(event: MessageEvent, param: PRCardParam): MessageChain? {
        return getMessageChain(param.score)
    }

    private fun getMessageChain(score: LazerScore): MessageChain {
        try {
            beatmapApiService.applyBeatmapExtend(score)
        } catch (e: Exception) {
            throw IllegalStateException.Fetch("谱面")
        }

        val image = imageService.getPanelGamma(score)

        return MessageChain(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRCardService::class.java)
    }
}