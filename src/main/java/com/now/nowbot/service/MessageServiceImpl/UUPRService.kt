package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.ScoreLegacy
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.UUPRService.UUPRParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("UU_PR")
class UUPRService(
    private val template: RestTemplate,
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val bindDao: BindDao
                 ) : MessageService<UUPRParam> {

    @JvmRecord data class UUPRParam(val user: OsuUser?, val score: Score, val mode: OsuMode?)

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
        if (Objects.isNull(range.data)) {
            return false
        }
        val uid = range.data!!.userID
        val includeFail = StringUtils.hasText(matcher.group("recent"))
        val offset = range.getOffset(0, false)

        val list = scoreApiService.getRecent(uid, mode.data, offset, 1, includeFail)
        if (list.isEmpty())
                throw GeneralTipsException(
                        GeneralTipsException.Type.G_Null_RecentScore, range.data!!.username, mode.data?.name ?: "默认")
        data.value = UUPRParam(range.data, list.first(), mode.data)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: UUPRParam) {
        val from = event.subject
        val score = param.score

        // 单成绩发送
        try {
            getTextOutput(score, from)
        } catch (e: GeneralTipsException) {
            throw e
        } catch (e: Exception) {
            from.sendMessage("最近成绩文字：发送失败，请重试")
            log.error("最近成绩文字：发送失败", e)
        }
    }

    @Throws(GeneralTipsException::class)
    private fun getTextOutput(score: Score, from: Contact) {
        val d = ScoreLegacy.getInstance(score, beatmapApiService)

        val httpEntity = HttpEntity.EMPTY as HttpEntity<Array<Byte>>
        val imgBytes =
                template.exchange(d.url, HttpMethod.GET, httpEntity, ByteArray::class.java).body
        QQMsgUtil.sendImageAndText(from, imgBytes, d.scoreLegacyOutput)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUPRService::class.java)
    }
}
