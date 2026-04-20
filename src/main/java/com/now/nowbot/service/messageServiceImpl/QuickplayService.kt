package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.MatchRating.Companion.applyDTMod
import com.now.nowbot.model.match.MatchRating.Companion.insertMicroUserToScores
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
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

        val image: ByteArray = imageService.getPanel(mr, "F")

        event.reply(image)

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

        val user = if (u.currentOsuMode.modeValue != modeByte) {
            userApiService.getOsuUser(u.userID, OsuMode.getMode(modeByte))
        } else {
            u
        }

        val match = matchApiService.getRoom(selectedRoomID).eventsToMatch()

        if (match.events.isEmpty()) {
            throw NoSuchElementException.QuickplayEvent()
        }

        return QuickplayParam(user, match)
    }

    /**
     * 需要从 events 接口拿来
     */
    fun Room.eventsToMatch(): Match {
        val roundIDs = this.events.mapNotNull { it.itemID }.toSet()
        val itemMap = this.items.associateBy { i -> i.listID }

        val its = roundIDs.mapNotNull { r -> itemMap[r] }

        val playedUserSet = its.mapNotNull { it.scores?.map { s -> s.userID } }.flatten().sorted().toSet()
        val teamVS = playedUserSet.size == 2

        return Match(
            statistics = Match.MatchStat(this.roomInfo.roomID, this.roomInfo.startedTime, this.roomInfo.endedTime, this.roomInfo.name),
            events = its.map { i ->

                val beatmap = this.beatmaps.find { bs -> bs.beatmapID == i.beatmapID } ?: Beatmap(beatmapID = i.beatmapID)

                val round = Match.MatchRound(
                    i.listID, beatmap, i.beatmapID, i.createdTime, i.playedTime, beatmap.modeInt ?: 0,
                    mods = i.allowedMods.map { it.acronym },
                    scores = i.scores.orEmpty().onEach { s ->

                        // 给 room 手动赋红蓝
                        val slot = playedUserSet.indexOf(s.userID)
                        val team = if (slot != -1 && teamVS) {
                            if (slot % 2 == 0) {
                                "blue"
                            } else {
                                "red"
                            }
                        } else {
                            "none"
                        }

                        s.playerStat = LazerScore.MatchScorePlayerStat(slot.toByte(), team, s.passed)
                    },
                    teamType = if (teamVS) "team-vs" else i.details?.teams ?: "head-to-head",
                    scoringType = "score-v2",
                )

                Match.MatchEvent(
                    i.listID,
                    Match.MatchEventDetail(
                        i.details?.roomType ?: "ranked_play", i.details?.roomType
                    ),
                    i.createdTime, i.ownerID,
                    round = round,
                )
            },
            players = this.users,
            firstEventID = this.firstEventID,
            latestEventID = this.latestEventID
        )
    }
}