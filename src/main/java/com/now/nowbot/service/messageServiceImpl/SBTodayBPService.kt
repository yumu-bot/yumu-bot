package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.TodayBPService.TodayBPParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getSBUserWithRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.UserIDUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SB_TODAY_BP")
class SBTodayBPService(
    private val userApiService: SBUserApiService,
    private val scoreApiService: SBScoreApiService,

    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val osuCalculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,

    ) : MessageService<TodayBPParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TodayBPParam>
    ): Boolean {
        val matcher = Instruction.SB_TODAY_BP.matcher(messageText)
        return if (matcher.find()) {
            data.value = getParam(matcher, event)
            true
        } else {
            false
        }
    }

    override fun HandleMessage(event: MessageEvent, param: TodayBPParam) {
        // param.asyncImage()
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("偏偏要上班今日最好成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班今日最好成绩")
        }
    }
    private fun getParam(matcher: Matcher, event: MessageEvent): TodayBPParam {
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()

        val id = UserIDUtil.getSBUserIDWithRange(event, matcher, mode, isMyself)
        id.setZeroDay()

        val user: OsuUser
        val bests: List<LazerScore>
        val dayStart: Int
        val dayEnd: Int

        if (id.data != null) {
            dayStart = id.getDayStart()
            dayEnd = id.getDayEnd()

            val async = AsyncMethodExecutor.awaitPairWithCollectionSupplierExecute(
                { userApiService.getUser(id.data!!) },
                { scoreApiService.getBestScore(
                    id = id.data!!,
                    mode = mode.data ?: OsuMode.DEFAULT
                ) }
            )

            user = async.first?.toOsuUser(mode.data!!) ?: throw NoSuchElementException.Player(id.data!!.toString())
            bests = async.second.map { it.toLazerScore() }
        } else {
            val range = getSBUserWithRange(event, matcher, mode, isMyself)
            range.setZeroDay()

            user = range.data?.toOsuUser(mode.data)
                ?: throw NoSuchElementException.Player()

            dayStart = range.getDayStart()
            dayEnd = range.getDayEnd()

            bests = scoreApiService.getBestScore(
                id = user.userID,
                mode = mode.data
            ).map { it.toLazerScore() }
        }

        val isToday = (dayStart == 0 && dayEnd == 1)

        val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())
        val dataMap = bests.mapIndexed { i, it ->
            return@mapIndexed if (it.endedTime.plusHours(8L).isBefore(laterDay) && it.endedTime.plusHours(8L).isAfter(earlierDay)) {
                i + 1 to it
            } else {
                null
            }
        }.filterNotNull().toMap()

        if (dataMap.isEmpty()) {
            /*
            if (!user.isActive) {
                throw NoSuchElementException.PlayerInactive(user.username)
            } else
             */
            if (isToday) {
                throw NoSuchElementException.TodayBestScore(user.username)
            } else {
                throw NoSuchElementException.PeriodBestScore(user.username)
            }
        }

        return TodayBPParam(user, mode.data!!, dataMap, isToday)
    }

    /*
    fun TodayBPParam.asyncImage() {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

     */

    fun TodayBPParam.getImage(): ByteArray {
        val todayMap = scores

        return if (todayMap.size > 1) {

            val ranks = todayMap.map { it.key }
            val scores = todayMap.map { it.value }

            AsyncMethodExecutor.awaitRunnableExecute(
                listOf(
                    AsyncMethodExecutor.Runnable { osuCalculateApiService.applyBeatMapChanges(scores) },

                    AsyncMethodExecutor.Runnable { osuCalculateApiService.applyStarToScores(scores) },

                    AsyncMethodExecutor.Runnable { osuBeatmapApiService.applyBeatmapExtendFromDatabase(scores) },
                )
            )

            val body = mapOf(
                "user" to user,
                "scores" to scores,
                "rank" to ranks,
                "panel" to "T"
            )

            imageService.getPanel(body, "A4")
        } else {
            val score: LazerScore = scores.toList().first().second

            val body = ScorePRService.getE5Param(user, score, "T", osuBeatmapApiService, osuCalculateApiService)

            imageService.getPanel(body, "E5")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBTodayBPService::class.java)

    }
}