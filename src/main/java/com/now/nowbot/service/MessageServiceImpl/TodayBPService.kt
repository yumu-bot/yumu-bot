package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.TodayBPService.TodayBPParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min

@Service("TODAY_BP")
class TodayBPService(
    var imageService: ImageService? = null,
    var scoreApiService: OsuScoreApiService? = null,
    var beatmapApiService: OsuBeatmapApiService? = null,
) : MessageService<TodayBPParam>, TencentMessageService<TodayBPParam> {

    data class TodayBPParam(
        val user: OsuUser,
        val mode: OsuMode,
        val scores: Map<Int, Score>,
        val isMyself: Boolean
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
        val from = event.subject

        val image = param.getImage()
        try {
            from.sendImage(image)
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

    override fun reply(event: MessageEvent, data: TodayBPParam): MessageChain? {
        return QQMsgUtil.getImage(data.getImage())
    }

    private fun getParam(matcher: Matcher, event: MessageEvent): TodayBPParam {
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()
        val range = getUserWithRange(event, matcher, mode, isMyself)
        val user = range.data ?: throw TipsException("没找到玩家")
        var dayStart = range.getValue(1, false) - 1
        var dayEnd = range.getValue(1, true)
        dayStart = min(0, dayStart)
        dayEnd = max(dayEnd, dayStart + 1)

        val bpList: List<Score>
        try {
            bpList = scoreApiService!!.getBestPerformance(user!!.userID, mode.data, 0, 100)
        } catch (e: WebClientResponseException.Forbidden) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user!!.username)
        } catch (e: WebClientResponseException.NotFound) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user!!.username)
        } catch (e: WebClientResponseException) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
        } catch (e: Exception) {
            log.error("HandleUtil：获取今日最好成绩失败！", e)
            throw TipsException("HandleUtil：获取今日最好成绩失败！")
        }
        val laterDay = LocalDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = LocalDateTime.now().minusDays(dayEnd.toLong())
        val dataMap = TreeMap<Int, Score>()

        bpList.forEach(
            ContextUtil.consumerWithIndex { s: Score, index: Int ->
                if (s.createTimePretty.isBefore(laterDay) && s.createTimePretty.isAfter(earlierDay)) {
                    dataMap[index] = s
                }
            }
        )
        return TodayBPParam(user, mode.data!!, dataMap, isMyself.get())
    }

    fun TodayBPParam.getImage(): ByteArray {
        val todayMap = scores

        if (CollectionUtils.isEmpty(todayMap)) {
            if (!user.active) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerInactive, user.username)
            }
            throw GeneralTipsException(GeneralTipsException.Type.G_Empty_PeriodBP, user.username, mode)
        }

        val ranks = ArrayList<Int>()
        val scores = ArrayList<Score?>()
        for ((key, value) in todayMap) {
            ranks.add(key + 1)
            scores.add(value)
        }

        beatmapApiService!!.applySRAndPP(scores)

        return try {
            imageService!!.getPanelA4(user, scores, ranks)
        } catch (e: Exception) {
            log.error("今日最好成绩：图片渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "今日最好成绩")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TodayBPService::class.java)
    }
}
