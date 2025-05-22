package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.MatchRating
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MatchMapService.MatchMapParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("MATCH_MAP")
class MatchMapService(
    private val imageService: ImageService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val matchApiService: OsuMatchApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<MatchMapParam>, TencentMessageService<MatchMapParam> {

    @JvmRecord data class MatchMapParam(val match: Match, val position: Int)

    @JvmRecord
    data class PanelE7Param(
        @JvmField val match: MatchRating,
        @JvmField val mode: OsuMode,
        @JvmField val mods: List<String>,
        @JvmField val players: List<MicroUser>,
        @JvmField val beatmap: BeatMap,
        @JvmField val density: IntArray,
        @JvmField val original: Map<String, Any>,
    )

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MatchMapParam>,
    ): Boolean {
        // 这个只能通过父服务 MatchListenerService 调用得到
        // return false

        val matcher = Instruction.TEST_MATCH_START.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
        }

        val matchID =
            matcher.group("id").let {
                if (it.isNullOrEmpty()) {
                    0
                } else {
                    it.toLong()
                }
            }
        val match = matchApiService.getMatchInfo(matchID, 10)
        val position =
            matcher.group("round").let {
                if (it.isNullOrEmpty()) {
                    1
                } else {
                    it.toInt()
                }
            }

        data.value = MatchMapParam(match, position)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MatchMapParam) {
        val e7Param = getPanelE7Param(param, beatmapApiService, calculateApiService)
        val image = imageService.getPanel(e7Param, "E7")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("比赛谱面信息：发送失败: ", e)
            event.reply(
                GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛谱面信息").message
            )
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MatchMapParam? {
        return null
    }

    override fun reply(event: MessageEvent, param: MatchMapParam): MessageChain? {
        return null
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchMapService::class.java)

        @Throws(Throwable::class)
        @JvmStatic
        fun getPanelE7Param(
            param: MatchMapParam,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE7Param {
            val mr = MatchRating(
                param.match,
                MatchRating.RatingParam(),
                beatmapApiService,
                calculateApiService
            )
            mr.calculate()

            if (mr.rounds.isEmpty() || param.position > mr.rounds.size)
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_MatchRound)

            val round = mr.rounds[param.position - 1]

            var eventID = 0L
            for (e in param.match.events) {
                if (e.round != null && e.round.roundID == round.roundID) {
                    eventID = e.eventID
                }
            }

            val beatmap = beatmapApiService.getBeatMap(round.beatMapID)

            val mode = OsuMode.correctConvert(round.mode, beatmap.mode)

            val original = DataUtil.getOriginal(beatmap)

            calculateApiService.applyStarToBeatMap(
                beatmap,
                mode,
                LazerMod.getModsList(round.mods)
            )

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)

            val players = DataUtil.getPlayersBeforeRoundStart(param.match, eventID)

            return PanelE7Param(mr, mode, round.mods, players, beatmap, density, original)
        }
    }
}
