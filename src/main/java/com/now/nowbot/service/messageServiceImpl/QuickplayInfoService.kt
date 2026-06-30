package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.multiplayer.RoomStatistics
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.web.QuickplayLeaderboardItem
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.regex.Matcher

@Service("QUICK_PLAY_INFO")
class QuickplayInfoService(
    private val userApiService: OsuUserApiService,
    private val matchApiService: OsuMatchApiService,
    private val imageService: ImageService,
    private val bindDao: BindDao
): MessageService<QuickplayInfoService.QuickplayInfoParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<QuickplayInfoParam>
    ): Boolean {
        val m = Instruction.QUICK_PLAY_INFO.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = getParam(event, m)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: QuickplayInfoParam
    ): ServiceCallStatistic? {
        event.replyAsync(imageService.getPanel(param.toMap(), "S")) { e ->
            log.warn("快速匹配：渲染失败", e)
            event.replyAsync(param.getTextMessage())
        }

        return ServiceCallStatistic.building(event)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): QuickplayInfoParam {
        val mode = InstructionUtil.getMode(matcher, bindDao.getGroupModeConfig(event))
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val variant: Byte = when (mode.data) {
            OsuMode.MANIA -> {
                4
            }
            OsuMode.MANIA_7K -> {
                7
            }
            else -> {
                0
            }
        }

        val infos: List<RoomInfo>

        val user: OsuUser

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPair(
                { userApiService.getOsuUser(id, mode.data ?: OsuMode.DEFAULT) },
                { userApiService.getQuickplay(id).rooms }
            )

            user = async.first
            infos = async.second
        } else {
            user = InstructionUtil.getUserWithoutRange(event, matcher, mode)
            infos = userApiService.getQuickplay(user.userID).rooms
        }

        val m = OsuMode.getMode(mode.data, user.currentOsuMode)

        val targetInfos = infos
            .filter {
                val b = it.currentPlaylistItem?.beatmap
                val difficulty = b?.difficultyName

                when(variant) {
                    4.toByte() -> difficulty?.contains("4K", ignoreCase = true) == true
                    7.toByte() -> difficulty?.contains("7K", ignoreCase = true) == true
                    else -> b?.mode == m
                }
            }
            .take(50)

        val current = user.matchmakingStats.filter { it.pool.variantID == variant }.maxByOrNull { it.poolID } ?: throw NoSuchElementException.RankedPlay()

        if (targetInfos.size > 25) {
            event.replyAndRecallAsync("""
                当前信息太多，需要一点时间来获取...
            """.trimIndent())
        }

        val (maxPage, surrounding) = getSurrounding(current.rank, user.userID, user.currentOsuMode, current.poolID)

        val rooms = matchApiService.getRooms(targetInfos, 25)

        return QuickplayInfoParam(user, current, surrounding, rooms, variant, maxPage)
    }

    private fun List<RoomStatistics>.getPlayerRating(userID: Long): Double {
        val allPlayersMQHistory = mutableMapOf<Long, MutableList<Double>>()

        val roomScores = this.flatMap { ts ->
            ts.rounds.mapNotNull { rs ->
                val users = rs.scores
                val my = users.firstOrNull { it.user.userID == userID } ?: return@mapNotNull null

                val otherScores = users.filter { it.user.userID != userID }.map { it.score }
                if (otherScores.isEmpty()) return@mapNotNull null
                val fieldAvg = otherScores.average()

                val roundTotal = users.sumOf { it.score }
                if (roundTotal > 0L) {
                    val roundAvg = roundTotal.toDouble() / users.size
                    for (u in users) {
                        allPlayersMQHistory.getOrPut(u.user.userID) { mutableListOf() }.add(u.score / roundAvg)
                    }
                }

                RoomScoreToRating(my.score, fieldAvg.toLong())
            }
        }

        if (roomScores.isEmpty()) return 0.0

        val trueMinimalMQ = allPlayersMQHistory.values.minOfOrNull { it.average() } ?: 0.0

        val rawRatings = roomScores.map { rs ->
            if (rs.myScore == 0L && rs.fieldAvgScore == 0L) 0.0
            else (rs.myScore * 2.0) / (rs.myScore + rs.fieldAvgScore)
        }
        val effectiveRating = rawRatings.average()

        return ((effectiveRating - trueMinimalMQ) / (1.0 - trueMinimalMQ)).coerceAtLeast(0.0)
    }

    data class RoomScoreToRating(val myScore: Long, val fieldAvgScore: Long)

    private fun List<RoomStatistics>.getPlayerStatistics(userID: Long): QuickplayStatistics {

        val mine = this.flatMap { it.rounds.mapNotNull{ rs -> rs.scores.firstOrNull { u -> u.user.userID == userID } } }

        val combo = mine.map { it.combo }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        val accuracy = mine.map { it.accuracy }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val star = this.flatMap { it.rounds.mapNotNull { rs -> rs.starRating } }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val count = this.flatMap { it.rounds.map { rs -> rs.beatmapID } }.toSet().count()

        return QuickplayStatistics(
            playTime = this.sumOf { it.duration },
            mapCount = count,
            averageCombo = combo,
            averageStarRating = star,
            averageAccuracy = accuracy,
        )
    }

    private fun QuickplayInfoParam.toMap(): Map<String, Any> {
        val roomStats = rooms.map { room -> RoomStatistics(room) }.sortedByDescending { it.roomID }

        val rating = roomStats.getPlayerRating(user.userID)
        val stats = roomStats.getPlayerStatistics(user.userID)

        return mapOf(
            "user" to user,
            "mode" to user.currentOsuMode,
            "stats" to stats,
            "recently" to roomStats.take(4),
            "surrounding" to surrounding,
            "rating" to rating,
            "variant" to variant,
            "total_players" to ((maxPage - 1) * 50).coerceAtLeast(0),
        )
    }

    private fun QuickplayInfoParam.getTextMessage(): String {
        val mm = user.matchmakingStats.sortedByDescending { it.poolID }

        return mm.joinToString("\n---\n") { m ->
            val p = m.pool

            val active = if (p.active) "当前赛季" else "历史赛季"
            val provisional = if (m.provisional) "(临时)" else ""
            val winRate = m.firstPlacements * 100.0 / m.plays.coerceAtLeast(1)
            val variant = if (p.variantID > 0) {
               " ${p.variantID}K"
            } else ""

            """
                ${p.name}: $active $provisional (${OsuMode.getMode(p.rulesetID).fullName}${variant})
                排名：#${m.rank}
                段位分：${m.rating}
                胜场：${m.firstPlacements} / ${m.plays} (${"%.0f".format(winRate)}%)
                积分：${m.totalPoints}
            """.trimIndent()
        }
    }

    private fun getSurrounding(targetRank: Int, targetID: Long, mode: OsuMode, season: Int): Pair<Int, List<QuickplayLeaderboardItem>> {
        if (targetRank < 1) return 1 to emptyList()

        val assumedPage = (targetRank - 1) / 50 + 1

        val pageRange = (assumedPage - 1).coerceAtLeast(1)..(assumedPage + 1)

        val leaderboards = AsyncMethodExecutor.awaitList(
            pageRange.map { page ->
                Callable { userApiService.getQuickplayLeaderboard(page, mode, season) }
            }
        )

        val totalPage = leaderboards.maxOfOrNull { it.first } ?: 1
        val leaderboard = leaderboards.flatMap { it.second }.distinctBy { it.userID }.sortedBy { it.absoluteRank }

        val size = leaderboard.size

        val index = leaderboard.indexOfFirst { it.userID == targetID }

        return if (index != -1) {
            totalPage to leaderboard.drop((index - 3).coerceAtLeast(0)).take(7)
        } else {
            val mid = (size / 2 - 3).coerceAtLeast(0)
            totalPage to leaderboard.drop(mid).take(7)
        }
    }

    data class QuickplayInfoParam(
        val user: OsuUser,
        val matchmakingStats: OsuUser.MatchmakingStats,
        val surrounding: List<QuickplayLeaderboardItem>,
        val rooms: List<Room>,
        val variant: Byte = 0,
        val maxPage: Int = 1,
    )

    data class QuickplayStatistics(
        @field:JsonProperty("time")
        val playTime: Int,

        @field:JsonProperty("count")
        val mapCount: Int,

        @field:JsonProperty("combo")
        val averageCombo: Int,

        @field:JsonProperty("difficulty")
        val averageStarRating: Double,

        @field:JsonProperty("accuracy")
        val averageAccuracy: Double,
    )

    companion object {
        private val log = LoggerFactory.getLogger(QuickplayInfoService::class.java)
    }
}