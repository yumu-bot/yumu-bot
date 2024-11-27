package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.Match.MatchRound
import com.now.nowbot.model.multiplayer.Match.MatchScore
import com.now.nowbot.model.multiplayer.MatchRating
import com.now.nowbot.model.multiplayer.MatchRating.Companion.insertMicroUserToScores
import com.now.nowbot.model.multiplayer.MatchRating.RatingParam
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.serviceException.MRAException
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher

@Service("CSV") class CsvMatchService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<Matcher> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<Matcher>
    ): Boolean {
        val m = Instruction.CSV_MATCH.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = m
        return true
    }

    @CheckPermission(isGroupAdmin = true) @Throws(Throwable::class) override fun HandleMessage(
        event: MessageEvent, matcher: Matcher
    ) {
        val subject = event.subject

        val isMultiple = matcher.group("x") != null
        var id = 0L
        var ids: List<Long>? = null
        val sb = StringBuilder()

        if (isMultiple) {
            try {
                ids = parseDataString(matcher.group("data"))
                event.reply("正在处理系列赛")
                parseCRAs(sb, ids, matchApiService, beatmapApiService, calculateApiService)
            } catch (e: MRAException) {
                throw e
            } catch (e: Exception) {
                log.error("CSV-Series 获取失败")
                throw MRAException(MRAException.Type.RATING_Parameter_MatchIDError)
            }
        } else {
            try {
                id = matcher.group("data").toLong()
                event.reply("正在处理$id")
                parseCRA(sb, id, matchApiService, beatmapApiService, calculateApiService)
            } catch (e: NullPointerException) {
                throw MRAException(MRAException.Type.RATING_Match_NotFound)
            } catch (e: MRAException) {
                throw e
            } catch (e: Exception) {
                log.error("CSV-Round (Rating) 获取失败", e)
                throw MRAException(MRAException.Type.RATING_Parameter_MatchIDError)
            }
        }

        //必须群聊
        if (subject is Group) {
            try {
                if (isMultiple) {
                    if (Objects.nonNull(ids)) {
                        subject.sendFile(
                            sb.toString().toByteArray(StandardCharsets.UTF_8), "${ids!!.first()}s.csv"
                        )
                    }
                } else {
                    subject.sendFile(sb.toString().toByteArray(StandardCharsets.UTF_8), "$id.csv")
                }
            } catch (e: Exception) {
                log.error("比赛评分表：发送失败", e)
                throw MRAException(MRAException.Type.RATING_Send_CRAFailed)
            }
        } else {
            throw MRAException(MRAException.Type.RATING_Send_NotGroup)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CsvMatchService::class.java)

        @Throws(MRAException::class) fun parseCRAs(sb: StringBuilder, matchIDs: List<Long>?, matchApiService: OsuMatchApiService, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService) {
            if (matchIDs.isNullOrEmpty()) throw MRAException(MRAException.Type.RATING_Series_FetchFailed)

            for (matchID in matchIDs) {
                val match: Match

                try {
                    match = matchApiService.getMatchInfo(matchID, 10)
                } catch (e: Exception) {
                    throw MRAException(MRAException.Type.RATING_Series_NotFound, matchID.toString())
                }

                val mr = MatchRating(
                    match, RatingParam(0, 0, null, 1.0, delete = true, rematch = true), beatmapApiService, calculateApiService
                )
                mr.calculate()
                mr.insertMicroUserToScores()

                val rounds = mr.rounds

                //多比赛
                appendMatchStrings(sb, match)
                for (r in rounds) {
                    val scores = r.scores
                    appendRoundStrings(sb, r)
                    for (s in scores) {
                        appendScoreStringsLite(sb, s)
                    }
                }

                //多比赛分隔符
                sb.append('\n')
            }
        }

        @Throws(MRAException::class) fun parseCRA(sb: StringBuilder, matchID: Long, matchApiService: OsuMatchApiService, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService) {
            val match: Match

            try {
                match = matchApiService.getMatchInfo(matchID, 10)
            } catch (e: Exception) {
                throw MRAException(MRAException.Type.RATING_Match_NotFound)
            }

            val mr = MatchRating(
                match, RatingParam(0, 0, null, 1.0, delete = true, rematch = true), beatmapApiService, calculateApiService
            )
            mr.calculate()
            mr.insertMicroUserToScores()
            val rounds = mr.rounds

            for (r in rounds) {
                val scores = r.scores
                appendRoundStrings(sb, r)

                for (s in scores) {
                    appendScoreStrings(sb, s)
                }
            }
        }

        @Throws(MRAException::class) fun parseDataString(dataStr: String?): List<Long>? {
            if (dataStr == null) return null

            val dataStrArray =
                dataStr.trim { it <= ' ' }
                    .split("[\\s,，\\-|:]+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()

            if (dataStr.isBlank() || dataStrArray.isEmpty()) return null

            val matches = mutableListOf<Long>()

            for (s in dataStrArray) {
                val id: Long

                try {
                    id = s.toLong()
                    matches.add(id)
                } catch (e: NumberFormatException) {
                    throw MRAException(MRAException.Type.RATING_Series_NotFound, s)
                }
            }

            return matches
        }


        private fun appendMatchStrings(sb: StringBuilder, match: Match) {
            try {
                sb.append(match.statistics.startTime.format(Date1)).append(',')
                    .append(match.statistics.startTime.format(Date2)).append(',').append(match.statistics.matchID)
                    .append(',').append(match.statistics.name).append(',').append('\n')
            } catch (e: Exception) {
                sb.append(e.message).append('\n') //.append("  error---->")
            }
        }

        private fun appendRoundStrings(sb: StringBuilder, round: MatchRound) {
            try {
                val b: BeatMap?

                if (round.beatMap != null) {
                    b = round.beatMap
                } else {
                    b = BeatMap()
                    b.starRating = 0.0
                    b.totalLength = 0
                    b.beatMapID = -1L
                    b.maxCombo = 0
                }

                sb.append(round.startTime.format(Date1)).append(',').append(round.startTime.format(Date2)).append(',')
                    .append(round.mode).append(',').append(round.scoringType).append(',').append(round.teamType).append(',')
                    .append(b!!.starRating).append(',').append(b.totalLength).append(',')
                    .append(round.mods.join()).append(',').append(b.beatMapID).append(',')
                    .append(b.maxCombo).append('\n')
            } catch (e: Exception) {
                sb.append(e.message).append('\n') //.append("  error---->")
            }
        }

        private fun appendScoreStrings(sb: StringBuilder, score: MatchScore) {
            try {
                sb.append(score.userID).append(',').append(String.format("%4.4f", score.accuracy)).append(',')
                    .append(score.mods.join()).append(',').append(score.score).append(',')
                    .append(score.maxCombo).append(',').append(score.passed).append(',').append(score.perfect).append(',')
                    .append(score.playerStat.slot).append(',').append(score.playerStat.team).append(',')
                    .append(score.playerStat.pass).append("\n")
            } catch (e: Exception) {
                sb.append("<----MP ABORTED---->").append(e.message).append('\n')
            }
        }

        private fun appendScoreStringsLite(sb: StringBuilder, score: MatchScore) {
            try {
                sb.append(score.playerStat.team).append(',').append(score.userID).append(',').append(score.user?.userName)
                    .append(',').append(score.score).append(',').append(score.mods.join())
                    .append(',').append(score.maxCombo).append(',').append(String.format("%4.4f", score.accuracy))
                    .append(',').append("\n")
            } catch (e: Exception) {
                sb.append("<----MP ABORTED---->").append(e.message).append('\n')
            }
        }

        private val Date1: DateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd")
        private val Date2: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        private fun Iterable<String>.join(): String {
            return this.joinToString(separator = "|", prefix = "[", postfix = "]")
        }
    }
}
