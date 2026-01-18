package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MatchRecentService.MatchRecentParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MATCHID
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_UID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.regex.Matcher

@Service("MATCH_RECENT")
class MatchRecentService(
    private val matchApiService: OsuMatchApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val dao: ServiceCallStatisticsDao,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<MatchRecentParam> {

    data class MatchRecentParam(
        val name: String?,
        val qq: Long?,
        val userID: Long?,
        val matchID: Long,
        val isMyself: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<MatchRecentParam>
    ): Boolean {
        val matcher = Instruction.MATCH_RECENT.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: MatchRecentParam): ServiceCallStatistic? {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("比赛最近成绩：发送失败", e)
            throw IllegalStateException.Send("比赛最近成绩")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mids" to listOf(param.matchID)
            ))
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): MatchRecentParam {
        val beforeMatchID = dao.getLastMatchID(
            groupID = event.subject.id,
            from = LocalDateTime.now().minusHours(24L)
        )

        // 注意，这里的 FLAG_MATCHID 不一定就是 MATCH ID
        val nameStr = matcher.group(FLAG_NAME)?.trim() ?: ""
        val name2Str = matcher.group(FLAG_MATCHID)?.trim() ?: ""

        val uid = (matcher.group(FLAG_UID)?.trim() ?: "").toLongOrNull()

        val qq = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id
        }

        return if (uid != null) {
            if (nameStr.matches(REG_NUMBER_7_9)) {
                MatchRecentParam(null, null, uid, nameStr.toLong())
            } else if (name2Str.matches(REG_NUMBER_7_9)) {
                MatchRecentParam(null, null, uid, name2Str.toLong())
            } else if (beforeMatchID != null) {
                MatchRecentParam(null, null, uid, beforeMatchID)
            } else {
                throw NoSuchElementException.Match()
            }
        } else if (qq == event.sender.id) {
            if (nameStr.isNotEmpty()) {
                if (nameStr.matches(REG_NUMBER_7_9)) {
                    if (name2Str.isNotEmpty()) {
                        MatchRecentParam(name2Str, null, null, nameStr.toLong())
                    } else {
                        MatchRecentParam(null, qq, null, nameStr.toLong(), true)
                    }
                } else if (name2Str.matches(REG_NUMBER_7_9)) {
                    MatchRecentParam(nameStr, null, null, name2Str.toLong())
                } else if (beforeMatchID != null) {
                    MatchRecentParam(nameStr, null, null, beforeMatchID, true)
                } else {
                    throw NoSuchElementException.Match()
                }
            } else {
                if (name2Str.matches(REG_NUMBER_7_9)) {
                    MatchRecentParam(null, qq, null, name2Str.toLong(), true)
                } else if (beforeMatchID != null) {
                    MatchRecentParam(null, qq, null, beforeMatchID, true)
                } else {
                    throw NoSuchElementException.Match()
                }
            }
        } else {
            if (nameStr.matches(REG_NUMBER_7_9)) {
                MatchRecentParam(null, qq, null, nameStr.toLong())
            } else if (name2Str.matches(REG_NUMBER_7_9)) {
                MatchRecentParam(null, qq, null, name2Str.toLong())
            } else if (beforeMatchID != null) {
                MatchRecentParam(null, qq, null, beforeMatchID)
            } else {
                throw NoSuchElementException.Match()
            }
        }
    }

    private fun MatchRecentParam.getImage(): ByteArray {
        val match = matchApiService.getMatch(matchID)

        val rounds = match.events.mapNotNull { event ->
            event.round
        }

        val scores = rounds.flatMap { round ->
            val ss = round.scores
            val b = round.beatmap ?: Beatmap(beatmapID = round.beatmapID)

            ss.forEach { s ->
                s.beatmap = b
                b.beatmapset?.let { s.beatmapset = it }
                s.beatmapID = round.beatmapID
            }

            ss
        }

        val mode = rounds.flatMap { it.scores }
            .groupingBy { it.mode }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: OsuMode.DEFAULT

        val user = if (this.userID != null) {
            userApiService.getOsuUser(this.userID, mode)
        } else if (this.name != null) {
            userApiService.getOsuUser(this.name, mode)
        } else {
            val bindUser = bindDao.getBindFromQQ(this.qq!!)

            userApiService.getOsuUser(bindUser.userID, OsuMode.getMode(mode, bindUser.mode))
        }

        val playerScores = scores.filter { s ->
            s.userID == user.userID
        }.take(50)

        if (playerScores.isEmpty()) {
            throw NoSuchElementException.RecentMatchScore(user.username, matchID)
        }

        val body = if (playerScores.size == 1) {
            val e5 = ScorePRService.getE5Param(
                user, null, playerScores[0], "MR", beatmapApiService, calculateApiService
            ).toMap().toMutableMap()

            e5["match"] = match.statistics.matchID

            e5.toMap()
        } else {
            beatmapApiService.applyBeatmapExtend(playerScores)

            AsyncMethodExecutor.awaitPairCallableExecute(
                { calculateApiService.applyStarToScores(playerScores) },
                { calculateApiService.applyPPToScores(playerScores) }
            )

            mapOf(
                "user" to user,
                "history_user" to null,
                "match" to match.statistics.matchID,
                "scores" to playerScores,
                "rank" to List(playerScores.size) { index ->
                    index + 1
                },
                "panel" to "MR"
            )
        }

        val image: ByteArray = try {
            if (playerScores.size == 1) {
                imageService.getPanel(body, "E5")
            } else {
                imageService.getPanel(body, "A4")
            }
        } catch (e: Exception) {
            log.error("比赛最近成绩：渲染失败", e)
            throw IllegalStateException.Fetch("比赛最近成绩")
        }

        return image
    }

    companion object {
        private val log = LoggerFactory.getLogger(MatchRecentService::class.java)

        private val REG_NUMBER_7_9 = Regex("\\d{7,9}")
    }
}
