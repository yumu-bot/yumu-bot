package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.UUScore
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.UUPRService.UUPRParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("UU_PR")
class UUPRService(
    private val osuApiWebClient: WebClient,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<UUPRParam> {

    @JvmRecord data class UUPRParam(val user: OsuUser?, val score: LazerScore, val mode: OsuMode?)

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<UUPRParam>
    ): Boolean {
        val matcher = Instruction.UU_PR.matcher(messageText)

        if (!matcher.find()) return false

        val mode = getMode(matcher)
        val range = getUserWithRange(event, matcher, mode, AtomicBoolean())
        if (range.data == null) {
            return false
        }
        val uid = range.data!!.userID
        val includeFail = matcher.group("recent").isNullOrBlank().not()
        val offset = range.getOffset(0, false)

        val list = scoreApiService.getScore(uid, mode.data, offset, 1, includeFail)
        if (list.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_RecentScore, range.data!!.username, mode.data?.fullName ?: "默认")
        }
        data.value = UUPRParam(range.data, list.first(), mode.data)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: UUPRParam) {
        val score = param.score

        // 单成绩发送
        try {
            getTextOutput(score, event)
        } catch (e: GeneralTipsException) {
            throw e
        } catch (e: Exception) {
            log.error("最近成绩文字：发送失败", e)
            event.reply("最近成绩文字：发送失败，请重试。")
        }
    }

    @Throws(GeneralTipsException::class)
    private fun getTextOutput(score: LazerScore, event: MessageEvent) {
        val d = UUScore(score, beatmapApiService, calculateApiService)

        val imgBytes =
            osuApiWebClient.get()
                .uri(d.url ?: "")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        event.reply(imgBytes, d.scoreLegacyOutput)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUPRService::class.java)
    }
}
