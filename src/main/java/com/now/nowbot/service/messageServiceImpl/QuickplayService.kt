package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.web.Quickplay
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionObject
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.MatchUtil.toMatch
import com.now.nowbot.util.UserIDUtil
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("QUICK_PLAY")
class QuickplayService(
    private val userApiService: OsuUserApiService,
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService
): MessageService<QuickplayService.QuickplayParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<QuickplayParam>
    ): Boolean {
        val matcher = Instruction.QUICK_PLAY.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: QuickplayParam
    ): ServiceCallStatistic? {

        val mr = MatchRating(
            param.match, MatchRating.RatingParam(), beatmapApiService, calculateApiService
        )

        mr.calculate()
        mr.insertMicroUserToScores()
        mr.applyDTMod()

        val image: ByteArray = imageService.getPanel(mapOf(
            "match" to mr,
            "panel" to "RP"
        ), "F")

        event.replyAsync(image)

        return null
    }

    data class QuickplayParam(val user: OsuUser, val match: Match)

    private fun getParam(event: MessageEvent, matcher: Matcher): QuickplayParam {
        val data = UserIDUtil.getUserIDWithRange(event, matcher, InstructionObject())
        val id = data.data

        val index: Int

        val u: OsuUser
        val quickplay: Quickplay

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPair(
                { userApiService.getOsuUser(id) },
                { userApiService.getQuickplay(id) }
            )

            u = async.first
            quickplay = async.second
            index = ((data.start ?: 1) - 1).coerceAtLeast(0)
        } else {
            val data2 = InstructionUtil.getUserWithRange(event, matcher, InstructionObject(), AtomicBoolean())

            u = data2.data!!

            quickplay = userApiService.getQuickplay(u.userID)
            index = ((data2.start ?: 1) - 1).coerceAtLeast(0)
        }

        val selectedRoomID = quickplay.rooms.getOrNull(index)?.roomID ?: if (index == 0) {
            throw NoSuchElementException.Quickplay()
        } else {
            throw NoSuchElementException.QuickplaySelected()
        }

        val modeByte = quickplay.rooms.getOrNull(index)?.currentPlaylistItem?.rulesetID ?: (-1).toByte()

        val user = if (u.mode.modeValue != modeByte) {
            userApiService.getOsuUser(u.userID, modeByte.toOsuMode())
        } else {
            u
        }

        val match = matchApiService.getRoom(selectedRoomID, fetchAllEvents = true).toMatch()

        if (match.events.isEmpty()) {
            throw NoSuchElementException.QuickplayEvent()
        }

        return QuickplayParam(user, match)
    }
}