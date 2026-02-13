package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.max

@Service("RECENT_BEST")
class RecentBestService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val scoreDao: ScoreDao,
    private val infoDao: OsuUserInfoDao,
    private val bindDao: BindDao,
    private val imageService: ImageService,
): MessageService<RecentBestService.RecentBestParam> {

    data class RecentBestParam(
        val user: OsuUser,
        val history: OsuUser?,
        val mode: OsuMode,
        val scores: List<LazerScoreLite>,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<RecentBestParam>
    ): Boolean {
        val matcher = Instruction.RECENT_BEST.matcher(messageText)

        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: RecentBestParam
    ): ServiceCallStatistic? {
        val image = param.getImage()

        event.reply(image)

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = param.scores.map { it.beatmapId }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.currentOsuMode),
        )
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): RecentBestParam {
        val dataMode = InstructionUtil.getMode(matcher)
        val isMyself = AtomicBoolean()
        val dataID = UserIDUtil.getUserIDWithRange(event, matcher, dataMode, isMyself)
        val user: OsuUser

        val minimumPP: Double

        val dayStart: Int
        val dayEnd: Int

        if (dataID.data != null) {
            dayStart = dataID.getDayStart()
            dayEnd = dataID.getDayEnd(7)

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(dataID.data!!, dataMode.data!!) },
                { scoreApiService.getBestScores(dataID.data!!, dataMode.data!!, 0, 100) },
            )

            user = async.first
            minimumPP = max((async.second.lastOrNull()?.pp ?: (user.pp / 20)) / 2, 10.0)

        } else {
            val dataUser = InstructionUtil.getUserWithRange(event, matcher, dataMode, isMyself)
            dataUser.setZeroDay()

            user = dataUser.data ?: throw NoSuchElementException.Player()

            dayStart = dataUser.getDayStart()
            dayEnd = dataUser.getDayEnd(7)

            val bests = scoreApiService.getBestScores(user.userID, dataMode.data!!, 0, 100)

            minimumPP = max((bests.lastOrNull()?.pp ?: (user.pp / 20)) / 2, 10.0)
        }

        val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())

        val mode = dataMode.data ?: user.currentOsuMode

        val scores = scoreDao.getUserRankedScore(user.userID, mode.modeValue, earlierDay, laterDay)

        if (scores.isEmpty()) {
            if (bindDao.getBindUser(user.userID) == null) {
                throw NoSuchElementException.RecentBestNoRecorded(user.username)
            } else {
                throw NoSuchElementException.RecentBest(user.username, mode)
            }
        }

        val filtered = scores.filter { it.pp >= minimumPP }

        if (filtered.isEmpty()) {
            throw NoSuchElementException.RecentBestFiltered(user.username, mode)
        }

        val historyUser = infoDao.getHistoryUser(user)

        return RecentBestParam(user, historyUser, mode, scores)
    }

    private fun List<LazerScoreLite>.extend(): List<LazerScore> {
        val scores = this.map { it.toLazerScore() }

        beatmapApiService.applyBeatmapExtend(scores)
        beatmapApiService.applyVersion(scores)
        calculateApiService.applyBeatmapChanges(scores)
        calculateApiService.applyStarToScores(scores)

        return scores.sortedByDescending {
            return@sortedByDescending when(it.rank) {
                "F" -> -1
                "D" -> 0
                "C" -> 1
                "B" -> 2
                "A" -> 3
                "S" -> 4
                "SH" -> 5
                "SS", "X" -> 6
                "SSH", "XH" -> 7
                else -> -1
            }
        }.sortedByDescending { it.pp }.take(50)
    }

    private fun RecentBestParam.getImage(): ByteArray {
        val ss = scores.extend()

        if (ss.isEmpty()) {
            throw NoSuchElementException.RecentBestFiltered(user.username, mode)
        }

        val body = mapOf(
            "user" to user,
            "history_user" to history,
            "score" to ss,
            "rank" to List(ss.size) { it + 1 },
            "panel" to "RB",
            "compact" to (ss.size > 100),
        )

        val image = imageService.getPanel(body, "A5")

        return image
    }
}