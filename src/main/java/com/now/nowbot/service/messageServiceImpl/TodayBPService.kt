package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Covers.Companion.CoverType
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
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.InstructionUtil.getUserWithRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("TODAY_BP")
class TodayBPService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val infoDao: OsuUserInfoDao,
) : MessageService<TodayBPParam>, TencentMessageService<TodayBPParam> {

    data class TodayBPParam(
        val user: OsuUser,
        val historyUser: OsuUser? = null,
        val mode: OsuMode,
        val scores: Map<Int, LazerScore>,
        val isToday: Boolean,
        val isCompact: Boolean = false
    )

    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<TodayBPParam>): Boolean {
        val matcher = Instruction.TODAY_BP.matcher(messageText)
        return if (matcher.find()) {
            data.value = getParam(matcher, event, isCompact = false)
            true
        } else {
            false
        }
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: TodayBPParam): ServiceCallStatistic? {
        param.asyncImage()
        val message = param.getMessageChain()

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("今日最好成绩：发送失败", e)
            throw IllegalStateException.Send("今日最好成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.mode)
        )
    }

    override fun accept(event: MessageEvent, messageText: String): TodayBPParam? {
        val matcher = OfficialInstruction.TODAY_BP.matcher(messageText)
        return if (matcher.find()) {
            getParam(matcher, event, isCompact = true)
        } else {
            null
        }
    }

    override fun reply(event: MessageEvent, param: TodayBPParam): MessageChain? {
        param.asyncImage()
        return param.getMessageChain()
    }

    private fun getParam(matcher: Matcher, event: MessageEvent, isCompact: Boolean): TodayBPParam {
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself, maximum = 999)
        id.setZeroDay()

        val user: OsuUser
        val bests: List<LazerScore>
        val dayStart: Int
        val dayEnd: Int

        if (id.data != null) {
            dayStart = id.getDayStart()
            dayEnd = id.getDayEnd()

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id.data!!, mode.data!!) },
                { scoreApiService.getBestScores(id.data!!, mode.data ?: OsuMode.DEFAULT) }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            val range = getUserWithRange(event, matcher, mode, isMyself)
            range.setZeroDay()

            user = range.data ?: throw NoSuchElementException.Player()

            dayStart = range.getDayStart()
            dayEnd = range.getDayEnd()

            bests = scoreApiService.getBestScores(user.userID, mode.data)
        }

        val isToday = (dayStart == 0 && dayEnd == 1)

        val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())
        val dataMap = bests.mapIndexed { i, it ->
            return@mapIndexed if (it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isBefore(laterDay)
                && it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isAfter(earlierDay)) {
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

        val historyUser = infoDao.getHistoryUser(user)

        return TodayBPParam(user, historyUser, mode.data!!, dataMap, isToday, isCompact)
    }

    fun TodayBPParam.asyncImage() {
        scoreApiService.asyncDownloadBackgroundFromScores(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

    fun TodayBPParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1) {

                val ranks = scores.map { it.key }
                val ss = scores.map { it.value }

                AsyncMethodExecutor.awaitPairCallableExecute(
                    { calculateApiService.applyBeatMapChanges(ss) },
                    { calculateApiService.applyStarToScores(ss) }
                )

                val body = mapOf(
                    "user" to user,
                    "scores" to ss,
                    "rank" to ranks,
                    "panel" to "T",
                    "compact" to (isCompact && scores.size >= 10)
                )

                MessageChain(imageService.getPanel(body, "A4"))
            } else {
                val pair = scores.toList().first()

                val score: LazerScore = pair.second
                score.ranking = pair.first

                val body = ScorePRService.getE5Param(user, historyUser, score, "T", beatmapApiService, calculateApiService)

                MessageChain(imageService.getPanel(body, "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }
    private fun TodayBPParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val list = scores.toList().take(5)
            val ss = list.map { it.second }

            AsyncMethodExecutor.awaitPairCallableExecute (
                { beatmapApiService.applyBeatmapExtend(ss) },
                { calculateApiService.applyPPToScores(ss) },
            )

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            getUUScores(user, list, covers)
        } else {

            val s = scores.toList().first().second

            val cover = scoreApiService.getCover(s, CoverType.COVER)

            AsyncMethodExecutor.awaitPairCallableExecute (
                { beatmapApiService.applyBeatmapExtend(s) },
                { calculateApiService.applyPPToScore(s) },
            )

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TodayBPService::class.java)

    }
}
