package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.Match.MatchRound
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.MatchRating.Companion.insertMicroUserToScores
import com.now.nowbot.model.match.MatchRating.RatingParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botException.MatchRoundException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
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

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        val matchID: Int
        val matchIDStr = param.group("matchid")

        if (matchIDStr == null || matchIDStr.isBlank()) {
            try {
                val md = getMarkdownFile("Help/round.md")
                val image = imageService.getPanelA6(md, "help")
                event.reply(image)
                return null
            } catch (e: Exception) {
                throw MatchRoundException(MatchRoundException.Type.MR_Instructions)
            }
        }

        matchID = matchIDStr.toIntOrNull() ?: throw MatchRoundException(MatchRoundException.Type.MR_MatchID_RangeError)

        var keyword = param.group("keyword")
        val hasKeyword = keyword.isNullOrBlank().not()

        val round: Int
        var roundStr = param.group("round")
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
                    return null
                } catch (e: Exception) {
                    throw MatchRoundException(MatchRoundException.Type.MR_Instructions)
                }
            }
        }

        round = (roundStr.toIntOrNull()?.minus(1) ?: -1)

        val mrParam = getParam(matchID, round, keyword)

        val image = try {
            imageService.getPanel(mrParam.toMap(), "F3")
        } catch (e: Exception) {
            log.error("对局信息图片渲染失败：", e)
            throw IllegalStateException.Render("对局信息")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("对局信息数据请求失败", e)
            throw IllegalStateException.Send("对局信息")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mids" to listOf(mrParam.matchRating.match.id),
                "uids" to mrParam.round.scores.map { it.userID },
                "bids" to listOf(mrParam.round.beatmapID),
                "sids" to mrParam.round.beatmap?.beatmapsetID?.let { listOf(it) },
                "modes" to listOf(mrParam.round.mode.modeValue)
            ))
        }
    }

    data class MatchRoundParam(
        val matchRating: MatchRating,
        val round: MatchRound,
        val index: Int,
    ) {
        fun toMap(): Map<String, Any> {

            return mapOf(
                "match" to matchRating,
                "round" to round,
                "index" to index,
                "panel" to "MR"
            )
        }
    }

    @Throws(MatchRoundException::class) fun getParam(matchID: Int, index: Int, keyword: String?): MatchRoundParam {
        var i = index
        val hasKeyword = !keyword.isNullOrBlank()

        val match: Match
        try {
            match = matchApiService.getMatch(matchID.toLong(), 10)
        } catch (e: WebClientResponseException) {
            throw MatchRoundException(MatchRoundException.Type.MR_MatchID_NotFound)
        }

        while (match.firstEventID != match.events.first().eventID) {
            val events: List<Match.MatchEvent> = matchApiService.getMatch(matchID.toLong(), 10).events
            if (events.isEmpty()) throw MatchRoundException(MatchRoundException.Type.MR_Round_Empty)
            match.events.addAll(0, events)
        }

        //获取所有轮的游戏
        val mr = MatchRating(
            match, RatingParam(0, 0, null, 1.0, delete = true, rematch = true), beatmapApiService, calculateApiService
        )
        mr.calculate()
        mr.insertMicroUserToScores()

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

        val round = rounds[i]

        calculateApiService.applyBeatMapChanges(round.beatmap, LazerMod.getModsList(round.mods))
        calculateApiService.applyStarToBeatMap(round.beatmap, round.mode, LazerMod.getModsList(round.mods))

        if (round.scores.size > 2) {
            round.scores = round.scores.sortedByDescending { it.score }
        }

        return MatchRoundParam(mr, round, i)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchRoundService::class.java)

        private fun getRoundIndexFromKeyWord(infoList: List<MatchRound>, keyword: String?): Int {
            val size = infoList.size
            val word: String

            if (keyword.isNullOrBlank().not()) {
                word = keyword.trim { it <= ' ' }.lowercase()
            } else {
                return -1
            }

            for (i in 0..<size) {
                val beatmap: Beatmap?

                try {
                    beatmap = infoList[i].beatmap
                    if (beatmap == null) continue
                } catch (ignored: NullPointerException) {
                    continue
                }

                try {
                    if (beatmap.beatmapset != null && (beatmap.beatmapset!!.title.lowercase()
                            .contains(word) || beatmap.beatmapset!!.artist.lowercase()
                            .contains(word) || beatmap.beatmapset!!.titleUnicode.lowercase()
                            .contains(word) || beatmap.beatmapset!!.artistUnicode.lowercase()
                            .contains(word) || beatmap.beatmapset!!.creator.lowercase()
                            .contains(word) || beatmap.difficultyName.lowercase().contains(word))) {
                        return i
                    }
                } catch (ignored: Exception) { //continue;
                }
            }

            return -1
        }
    }
}

