package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.floor
import kotlin.math.roundToLong

@Service("CSV_NEWBIE")
class CalculateNewbieCupService(
    private val matchApiService: OsuMatchApiService,
    private val userApiService: OsuUserApiService,
): MessageService<CalculateNewbieCupService.NewbieCupParam> {

    data class NewbieCupParam(
        val matches: List<Match>,
        val sortBy: List<Long>?,
        val accuracy: Long?,
        val mode: OsuMode,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<NewbieCupParam>
    ): Boolean {
        val m = Instruction.CALCULATE_NEWBIE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        if (m.group(FLAG_DATA).isNullOrEmpty()) return false

        val ids = parseLong(m.group(FLAG_DATA))

        if (ids.isEmpty()) return false

        event.reply("正在获取新人群群赛的比赛记录...")

        val matches = getMatches(ids)

        val sorts = parseLong(m.group(FLAG_BID))

        val accuracy = m.group(FLAG_SID)?.toLongOrNull()

        val mode = OsuMode.getMode(m.group(FLAG_MODE), matches.first().events.firstNotNullOfOrNull { it.round }?.mode)

        data.value = NewbieCupParam(matches, sorts, accuracy, mode)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: NewbieCupParam) {
        event.reply("正在获取玩家表现分...")

        val result = getResult(param)

        val str = printResult(result, param.sortBy, param.accuracy)

        val name = param.matches.first().name.split("\\s*$REG_COLON\\s*".toRegex()).firstOrNull()?.trim() ?: "OCNC"

        event.replyFileInGroup(str.toByteArray(Charsets.UTF_8), "$name.csv")
    }

    data class NewbieCupResult(
        val bids: Set<Long>,
        val lines: Map<OsuUser, List<Pair<Long, Double>>>
    )
    // 主计算
    private fun getResult(param: NewbieCupParam): NewbieCupResult {
        val matches = param.matches

        val hasSort = !param.sortBy.isNullOrEmpty()

        val users = matches.flatMap { it.players }.distinctBy { it.userID }

        val rounds = matches.flatMap { it.events }.mapNotNull { it.round }

        val scores = rounds.flatMap { it.scores }

        val filteredUsers = users.filter { u ->
            scores.filter { s -> s.userID == u.userID }.sumOf { it.score } > 0
        }

        val osuUsers = getUsers(users, param.mode)

        val bids = (if (hasSort) {
            param.sortBy!!
        } else {
            rounds.map { it.beatmapID }
        }).toSet()



        val lines = filteredUsers.associate { u ->

            val user = osuUsers[u.userID]!!

            val line = bids.map { id ->
                val beatmapScores = scores.filter { it.userID == u.userID && it.beatmapID == id }

                val maxScore = beatmapScores.maxOfOrNull { it.score } ?: 0L
                val maxAccuracy = beatmapScores.maxOfOrNull { it.accuracy } ?: 0.0

                maxScore to maxAccuracy
            }

            user to line
        }

        return NewbieCupResult(bids, lines)
    }


    private fun getMatches(matchIDs: List<Long>): List<Match> {
        return matchIDs.map {
            try {
                matchApiService.getMatch(it, 10)
            } catch (e: NetworkException.MatchException) {
                throw NetworkException.MatchException("获取 $it 时出现了错误：\n${e.message}")
            }
        }
    }

    private fun getUsers(users: List<MicroUser>, mode: OsuMode): Map<Long, OsuUser> {
        return users.associate { u ->
            val user = try {
                userApiService.getOsuUser(u.userID, mode)
            } catch (e: Exception) {
                OsuUser(u.username, u.userID, pp = -1.0)
            }

            u.userID to user
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CalculateNewbieCupService::class.java)

        private fun getMultiplier(bid: Long, sortBy: List<Long>?, pp: Double): Double {
            if (sortBy.isNullOrEmpty() || pp < 0.0) return 1.0

            val multiplierArray:Array<DoubleArray> = arrayOf(
                doubleArrayOf(1.0, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 0.0),
                doubleArrayOf(0.6, 1.0, 1.2, 1.2, 1.2, 1.2, 1.2, 1.2, 1.2, 1.2, 0.0),
                doubleArrayOf(0.3, 1.0, 1.1, 1.0, 1.0, 1.1, 1.1, 1.1, 1.1, 1.1, 0.0),
                doubleArrayOf(0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5),
                doubleArrayOf(0.0, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 1.0),
                doubleArrayOf(0.1, 0.1, 0.1, 0.1, 0.1, 0.0, 0.1, 0.1, 0.1, 0.1, 0.1),
            )

            val arrayIndex = when(pp) {
                in 0.0 ..< 1000.0 -> 1
                in 1000.0 ..< 1500.0 -> 2
                in 1500.0 ..< 2000.0 -> 3
                in 2000.0 ..< 2500.0 -> 4
                in 2500.0 ..< 3100.0 -> 5
                else -> 6
            } - 1

            val bidIndex = sortBy.indexOf(bid)

            if (bidIndex == -1) {
                return 1.0
            }

            return multiplierArray[arrayIndex][bidIndex]
        }

        private fun parseLong(string: String?): List<Long> {
            if (string.isNullOrEmpty()) return emptyList()

            val strs = string.trim().split(REG_SEPERATOR.toRegex()).dropWhile { it.isBlank() }

            return strs.map {
                it.toLongOrNull() ?: run {
                    log.error("$it 无法转换成数字")
                    throw IllegalArgumentException.WrongException.MatchID()
                }
            }
        }

        private fun calculateAccuracyScore(accuracy: Map<Long, Double>): Map<Long, Long> {
            if (accuracy.size <= 1) return mapOf()

            return accuracy
                .toList()
                .sortedByDescending { it.second }
                .mapIndexed { i: Int, pair: Pair<Long, Double> ->
                    val score = ((accuracy.size - i) * 1000000.0 / (accuracy.size - 1)).roundToLong()

                    pair.first to score
                }
                .toMap()
        }

        private fun printResult(result: NewbieCupResult, sortBy: List<Long>?, accuracy: Long?): String {
            val sb = StringBuilder()

            if (!sortBy.isNullOrEmpty()) {
                val index = if (accuracy != null) {
                    result.bids.indexOf(accuracy)
                } else {
                    5 // 认为 6 号位是 ACC
                }

                val id2Accuracy = result.lines.map { line ->
                    line.key.userID to (line.value.getOrNull(index)?.second ?: 0.0)
                }.toMap()

                val id2AccuracyScore = calculateAccuracyScore(id2Accuracy)

                sb.append("userid,username,pp,").append(
                    result.bids.joinToString(",")
                ).append('\n')
                    .append(",,,").append(
                        List(result.bids.size) { i: Int ->
                            if (i != index) {
                                "score"
                            } else {
                                "accuracy"
                            }
                        }.joinToString(",")
                    ).append(',').append("total").append('\n')

                val multipliedLines = result.lines.map { (user, line) ->
                    val scores = line.mapIndexed { i: Int, pair: Pair<Long, Double> ->
                        val multiplier = getMultiplier(sortBy[i], sortBy, user.pp)

                        val score = if (i != index) {
                            pair.first
                        } else {
                            id2AccuracyScore.getOrDefault(user.userID, -1L)
                        }

                        (score * multiplier).roundToLong()
                    }

                    user to scores
                }.sortedByDescending { it.second.sum() }.toMap()

                multipliedLines.forEach { (user, scores) ->
                    sb.append("${user.userID},${user.username},${floor(user.pp).toLong()},").append(
                        scores.joinToString(",")
                    ).append(',').append(scores.sum()).append('\n')
                }

            } else if (accuracy != null) {
                val index = result.bids.indexOf(accuracy)

                sb.append("userid,username,pp,").append(
                    result.bids.joinToString(",")
                ).append('\n')
                    .append(",,,").append(
                        List(result.bids.size) { i: Int ->
                            if (i != index) {
                                "score"
                            } else {
                                "accuracy"
                            }
                        }.joinToString(",")
                    ).append('\n')

                result.lines.toList()
                    .sortedByDescending { pair -> pair.second.sumOf { score -> score.first } }
                    .toMap()
                    .forEach { (user, line) ->
                        sb.append("${user.userID},${user.username},${floor(user.pp).toLong()},").append(
                            line.mapIndexed { i: Int, pair: Pair<Long, Double> ->
                                if (i != index) {
                                    pair.first
                                } else {
                                    pair.second
                                }
                            }.joinToString(",")
                    ).append('\n')
                }
            } else {
                sb.append("userid,username,pp,").append(
                    result.bids.joinToString(",") { "$it,$it" }
                ).append('\n')
                    .append(",,,").append(
                        result.bids.joinToString(",") { "score,accuracy" }
                    ).append('\n')

                result.lines.toList()
                    .sortedByDescending { pair -> pair.second.sumOf { score -> score.first } }
                    .toMap()
                    .forEach { (user, line) ->
                        sb.append("${user.userID},${user.username},${floor(user.pp).toLong()},").append(
                            line.joinToString(",") { "${it.first},${it.second}" }
                    ).append('\n')
                }
            }

            return sb.toString().trim()
        }
    }
}