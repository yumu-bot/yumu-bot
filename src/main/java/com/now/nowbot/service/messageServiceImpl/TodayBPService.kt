package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TodayBPService.TodayBPParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("TODAY_BP")
class TodayBPService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<TodayBPParam>, TencentMessageService<TodayBPParam> {

    data class TodayBPParam(
        val user: OsuUser,
        val mode: OsuMode,
        val scores: Map<Int, LazerScore>,
        val isMyself: Boolean,
        val isToday: Boolean
    )

    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<TodayBPParam>): Boolean {
        val matcher = Instruction.TODAY_BP.matcher(messageText)
        return if (matcher.find()) {
            data.value = getParam(matcher, event)
            true
        } else {
            false
        }
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: TodayBPParam) {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("今日最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "今日最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): TodayBPParam? {
        val matcher = OfficialInstruction.TODAY_BP.matcher(messageText)
        return if (matcher.find()) {
            getParam(matcher, event)
        } else {
            null
        }
    }

    override fun reply(event: MessageEvent, param: TodayBPParam): MessageChain? {
        return QQMsgUtil.getImage(param.getImage())
    }

    private fun getParam(matcher: Matcher, event: MessageEvent): TodayBPParam {
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()
        val range = getUserWithRange(event, matcher, mode, isMyself)
        range.setZeroDay()
        val user = range.data ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
        val dayStart = range.getDayStart()
        val dayEnd = range.getDayEnd()
        val isToday = (dayStart == 0 && dayEnd == 1)

        val bpList: List<LazerScore>
        try {
            bpList = scoreApiService.getBestScores(user.userID, mode.data, 0, 100)
        } catch (e: WebClientResponseException.Forbidden) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user.username)
        } catch (e: WebClientResponseException.NotFound) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.username)
        } catch (e: WebClientResponseException) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
        } catch (e: Exception) {
            log.error("今日最好成绩：获取失败！", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "今日最好成绩")
        }
        val laterDay = java.time.OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = java.time.OffsetDateTime.now().minusDays(dayEnd.toLong())
        val dataMap = TreeMap<Int, LazerScore>()

        bpList.forEach(
            ContextUtil.consumerWithIndex { s: LazerScore, index: Int ->
                if (s.endedTime.isBefore(laterDay) && s.endedTime.isAfter(earlierDay)) {
                    dataMap[index] = s
                }
            }
        )
        return TodayBPParam(user, mode.data!!, dataMap, isMyself.get(), isToday)
    }

    fun TodayBPParam.getImage(): ByteArray {
        val todayMap = scores

        if (CollectionUtils.isEmpty(todayMap)) {
            if (!user.active) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerInactive, user.username)
            } else if (isToday) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Empty_TodayBP, user.username, mode)
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Empty_PeriodBP, user.username, mode)
            }
        }

        val ranks = mutableListOf<Int>()
        val scores = mutableListOf<LazerScore>()
        for ((key, value) in todayMap) {
            ranks.add(key + 1)
            scores.add(value)
        }

        calculateApiService.applyStarToScores(scores)

        return try {
            imageService.getPanelA4(user, scores, ranks, "T")
        } catch (e: Exception) {
            log.error("今日最好成绩：图片渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "今日最好成绩")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TodayBPService::class.java)
    }
}
