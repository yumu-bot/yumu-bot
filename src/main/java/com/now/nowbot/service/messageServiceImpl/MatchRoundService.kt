package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.Match.MatchRound
import com.now.nowbot.model.multiplayer.MatchRating
import com.now.nowbot.model.multiplayer.MatchRating.RatingParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.serviceException.MatchRoundException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.regex.Matcher

@Service("MATCH_ROUND") class MatchRoundService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService
) : MessageService<Matcher> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.MATCH_ROUND.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val matchID: Int
        val matchIDStr = matcher.group("matchid")

        if (matchIDStr == null || matchIDStr.isBlank()) {
            try {
                val md = getMarkdownFile("Help/round.md")
                val image = imageService.getPanelA6(md, "help")
                event.reply(image)
                return
            } catch (e: Exception) {
                throw MatchRoundException(MatchRoundException.Type.MR_Instructions)
            }
        }

        try {
            matchID = matchIDStr.toInt()
        } catch (e: NumberFormatException) {
            throw MatchRoundException(MatchRoundException.Type.MR_MatchID_RangeError)
        }

        var keyword = matcher.group("keyword")
        val hasKeyword = keyword.isNullOrBlank().not()

        val round: Int
        var roundStr = matcher.group("round")
        val hasRound = roundStr.isNullOrBlank().not()

        if (hasRound) {
            if (hasKeyword) { //这里是把诸如 21st 类的东西全部匹配到 keyword 里
                keyword = roundStr + keyword
                roundStr = "-1"
            }
        } else {
            if (hasKeyword) {
                roundStr = "-1"
            } else {
                try {
                    val md = getMarkdownFile("Help/round.md")
                    val image = imageService.getPanelA6(md, "help")
                    event.reply(image)
                    return
                } catch (e: Exception) {
                    throw MatchRoundException(MatchRoundException.Type.MR_Instructions)
                }
            }
        }

        round = try {
            roundStr!!.toInt() - 1
        } catch (e: NumberFormatException) {
            if (hasKeyword) {
                -1
            } else {
                throw MatchRoundException(MatchRoundException.Type.MR_Round_RangeError)
            }
        }

        val image = getDataImage(matchID, round, keyword)

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("对局信息数据请求失败", e)
            throw MatchRoundException(MatchRoundException.Type.MR_Send_Error)
        }
    }

    @Throws(MatchRoundException::class) fun getDataImage(matchID: Int, index: Int, keyword: String?): ByteArray {
        var i = index
        val hasKeyword = keyword.isNullOrBlank().not()

        val match: Match
        try {
            match = matchApiService.getMatchInfo(matchID.toLong(), 10)
        } catch (e: WebClientResponseException) {
            throw MatchRoundException(MatchRoundException.Type.MR_MatchID_NotFound)
        }

        while (match.firstEventID != match.events.first().eventID) {
            val events: List<Match.MatchEvent> = matchApiService.getMatchInfo(matchID.toLong(), 10).events
            if (events.isEmpty()) throw MatchRoundException(MatchRoundException.Type.MR_Round_Empty)
            match.events.addAll(0, events)
        }

        //获取所有轮的游戏
        val mr = MatchRating(
            match, RatingParam(0, 0, null, 1.0, delete = true, rematch = true), beatmapApiService, calculateApiService
        )
        mr.calculate()

        val rounds = mr.rounds

        if (i < 0 || i > match.events.size) {
            i = if (hasKeyword) {
                getRoundIndexFromKeyWord(rounds, keyword)
            } else {
                try {
                    getRoundIndexFromKeyWord(rounds, i.toString())
                } catch (e: NumberFormatException) {
                    throw MatchRoundException(MatchRoundException.Type.MR_Round_NotFound)
                }
            }
        }

        if (i == -1 && hasKeyword) {
            throw MatchRoundException(MatchRoundException.Type.MR_KeyWord_NotFound)
        }

        val img: ByteArray
        try {
            val body = mapOf(
                "stat" to match.statistics,
                "round" to mr.rounds[i],
                "index" to i,
            )

            img = imageService.getPanel(body, "F2")
        } catch (e: Exception) {
            log.error("对局信息图片渲染失败：", e)
            throw MatchRoundException(MatchRoundException.Type.MR_Fetch_Error)
        }

        return img
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchRoundService::class.java)

        private fun getRoundIndexFromKeyWord(infoList: List<MatchRound>, keyword: String?): Int {
            val size = infoList.size
            val word: String

            if (keyword.isNullOrBlank().not()) {
                word = keyword!!.trim { it <= ' ' }.lowercase()
            } else {
                return -1
            }

            for (i in 0..<size) {
                val beatMap: BeatMap?

                try {
                    beatMap = infoList[i].beatMap
                    if (beatMap == null) continue
                } catch (ignored: NullPointerException) {
                    continue
                }

                try {
                    if (beatMap.beatMapSet != null && (beatMap.beatMapSet!!.title.lowercase()
                            .contains(word) || beatMap.beatMapSet!!.artist.lowercase()
                            .contains(word) || beatMap.beatMapSet!!.titleUnicode.lowercase()
                            .contains(word) || beatMap.beatMapSet!!.artistUnicode.lowercase()
                            .contains(word) || beatMap.beatMapSet!!.creator.lowercase()
                            .contains(word) || beatMap.difficultyName.lowercase().contains(word))) {
                        return i
                    }
                } catch (ignored: Exception) { //continue;
                }
            }

            return -1
        }
    }
}

