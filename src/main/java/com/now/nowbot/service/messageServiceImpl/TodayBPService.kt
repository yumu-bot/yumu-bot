package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TodayBPService.TodayBPParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("TODAY_BP")
class TodayBPService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<TodayBPParam>, TencentMessageService<TodayBPParam> {

    data class TodayBPParam(
        val user: OsuUser,
        val mode: OsuMode,
        val scores: Map<Int, LazerScore>,
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
        param.asyncImage()
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("今日最好成绩：发送失败", e)
            throw IllegalStateException.Send("今日最好成绩")
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
        param.asyncImage()
        return QQMsgUtil.getImage(param.getImage())
    }

    private fun getParam(matcher: Matcher, event: MessageEvent): TodayBPParam {
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)
        id.setZeroDay()

        val user: OsuUser
        val bests: List<LazerScore>
        val dayStart: Int
        val dayEnd: Int

        if (id.data != null) {
            dayStart = id.getDayStart()
            dayEnd = id.getDayEnd()

            val async = AsyncMethodExecutor.awaitPairWithCollectionSupplierExecute(
                { userApiService.getOsuUser(id.data!!, mode.data!!) },
                { scoreApiService.getBestScores(id.data!!, mode.data ?: OsuMode.DEFAULT, 0, 200) }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            val range = getUserWithRange(event, matcher, mode, isMyself)
            range.setZeroDay()

            user = range.data ?: throw NoSuchElementException.Player()

            dayStart = range.getDayStart()
            dayEnd = range.getDayEnd()

            bests = scoreApiService.getBestScores(user.userID, mode.data, 0, 200)
        }

        val isToday = (dayStart == 0 && dayEnd == 1)

        val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())
        val dataMap = bests.mapIndexed { i, it ->
            return@mapIndexed if (it.endedTime.isBefore(laterDay) && it.endedTime.isAfter(earlierDay)) {
                i + 1 to it
            } else {
                null
            }
        }.filterNotNull().toMap()

        if (dataMap.isEmpty()) {
            if (!user.isActive) {
                throw NoSuchElementException.PlayerInactive(user.username)
            } else if (isToday) {
                throw NoSuchElementException.TodayBestScore(user.username)
            } else {
                throw NoSuchElementException.PeriodBestScore(user.username)
            }
        }

        return TodayBPParam(user, mode.data!!, dataMap, isToday)
    }

    fun TodayBPParam.asyncImage() {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

    fun TodayBPParam.getImage(): ByteArray {
        val todayMap = scores

        return if (todayMap.size > 1) {

            val ranks = todayMap.map { it.key }
            val scores = todayMap.map { it.value }

            calculateApiService.applyBeatMapChanges(scores)
            calculateApiService.applyStarToScores(scores)

            val body = mapOf(
                "user" to user,
                "scores" to scores,
                "rank" to ranks,
                "panel" to "T"
            )

            imageService.getPanel(body, "A4")
        } else {
            val score: LazerScore = scores.toList().first().second

            val body = ScorePRService.getE5Param(user, score, "T", beatmapApiService, calculateApiService)

            imageService.getPanel(body, "E5")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TodayBPService::class.java)

    }
}
