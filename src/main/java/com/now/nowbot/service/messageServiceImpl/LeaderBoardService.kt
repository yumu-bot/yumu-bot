package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.serviceException.LeaderBoardException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.regex.Matcher

@Service("LEADER_BOARD")
class LeaderBoardService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
) : MessageService<Matcher> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.LEADER_BOARD.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val bid: Long
        var range: Int
        val BIDstr = matcher.group(FLAG_BID)

        if (BIDstr.isNullOrBlank())
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_NoBid)

        try {
            bid = BIDstr.toLong()
        } catch (e: NumberFormatException) {
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_BidError)
        }

        if (matcher.group("range") == null) {
            range = 50
        } else {
            try {
                range = matcher.group("range").toInt()
            } catch (e: NumberFormatException) {
                throw LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_RangeError)
            }

            if (range < 1) range = 1 else if (range > 50) range = 50
        }

        val mode: OsuMode
        val scores: List<LazerScore?>
        val beatMap: BeatMap
        val isRanked: Boolean

        try {
            beatMap = beatmapApiService.getBeatMapFromDataBase(bid)
            isRanked = beatMap.hasLeaderBoard
        } catch (e: WebClientResponseException.NotFound) {
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Map_NotFound)
        } catch (e: Exception) {
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Map_FetchFailed)
        }

        try {
            mode = OsuMode.getMode(matcher.group("mode"), beatMap.mode)
            scores = scoreApiService.getLeaderBoardScore(bid, mode)
        } catch (e: Exception) {
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Score_FetchFailed)
        }

        if (!isRanked) {
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Map_NotRanked)
        }

        if (scores.isEmpty())
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Score_NotFound)
        if (range > scores.size) range = scores.size

        val ss = scores.take(range)

        calculateApiService.applyPPToScores(ss)

        try {
            val body = mapOf(
                "beatmap" to beatMap,
                "scores" to ss.map { it.beatMap = beatMap; it })

            val image = imageService.getPanel(body, "A3")

            event.reply(image)
        } catch (e: Exception) {
            log.error("排行榜", e)
            throw LeaderBoardException(LeaderBoardException.Type.LIST_Send_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LeaderBoardService::class.java)
    }
}
