package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.CATCH
import com.now.nowbot.model.enums.OsuMode.CATCH_RELAX
import com.now.nowbot.model.enums.OsuMode.MANIA
import com.now.nowbot.model.enums.OsuMode.OSU
import com.now.nowbot.model.enums.OsuMode.OSU_AUTOPILOT
import com.now.nowbot.model.enums.OsuMode.OSU_RELAX
import com.now.nowbot.model.enums.OsuMode.TAIKO
import com.now.nowbot.model.enums.OsuMode.TAIKO_RELAX
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MatchRecentService.MatchRecentParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
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
        val count: Int? = null,
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

        // 注意，这里的 FLAG_MATCHID 不一定就是 MATCH ID
        val nameStr = matcher.group(FLAG_NAME)?.trim() ?: ""
        val name2Str = matcher.group(FLAG_MATCHID)?.trim() ?: ""

        val userID = (matcher.group(FLAG_UID)?.trim() ?: "").toLongOrNull()

        val qq = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id
        }

        val (maybeMatchID, name, count) = parse2Text(nameStr, name2Str)

        val matchID = maybeMatchID ?: dao.getLastMatchID(
            groupID = event.subject.id,
            from = LocalDateTime.now().minusHours(24L)
        ) ?: throw IllegalArgumentException.WrongException.MatchID()

        return if (userID != null) {
            val isMyself = bindDao.getBindFromQQOrNull(event.sender.id)?.userID == userID

            MatchRecentParam(null, null, userID, matchID, count, isMyself)
        } else if (!name.isNullOrBlank()) {
            val myID = bindDao.getOsuID(name)
            val isMyself = bindDao.getBindFromQQOrNull(event.sender.id)?.userID == myID && myID != null

            MatchRecentParam(name, null, null, matchID, count, isMyself)
        } else {
            val isMyself = qq == event.sender.id

            MatchRecentParam(null, qq, null, matchID, count, isMyself)
        }
    }

    // 获取玩家名、比赛编号、页码
    private fun parse2Text(text1: String, text2: String): Triple<Long?, String?, Int?> {

        val t1: String
        val t2: String

        val matcher = REG_NUMBER_WITH_1_2.find(text1.trim())

        if (matcher != null && text2.isBlank()) {
            t1 = matcher.groupValues[1].trim()
            t2 = matcher.groupValues[2].trim()
        } else {
            t1 = text1.trim()
            t2 = text2.trim()
        }

        val t179 = t1.matches(REG_NUMBER_7_9)
        val t112 = t1.matches(REG_NUMBER_1_2)

        val t279 = t2.matches(REG_NUMBER_7_9)
        val t212 = t2.matches(REG_NUMBER_1_2)

        val matchID: Long? = if (t179) {
            t1.toLong()
        } else if (t279) {
            t2.toLong()
        } else null

        val count: Int? = if (t112) {
            t1.toInt()
        } else if (t212) {
            t2.toInt()
        } else null

        val name: String? = if (!t179 && !t112 && text1.isNotBlank()) {
            t1
        } else if (!t279 && !t212 && text2.isNotBlank()) {
            t2
        } else null

        return Triple(matchID, name, count)
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
                s.maximumStatistics = getMaximumStatistics(s.mode, s.statistics)
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
        }

        if (playerScores.isEmpty()) {
            throw NoSuchElementException.RecentMatchScore(user.username, matchID)
        }

        val filteredScores = playerScores.drop((count ?: 1) - 1).take(if (count != null) 1 else 50)

        if (filteredScores.isEmpty()) {
            throw NoSuchElementException.RecentMatchScoreFiltered(user.username, matchID)
        }

        val body = if (filteredScores.size == 1) {
            val score = filteredScores.first()

            AsyncMethodExecutor.awaitTripleCallableExecute(
                // 缺的东西太多
                { beatmapApiService.applyBeatmapExtendFromAPI(score) },
                { calculateApiService.applyStarToScore(score) },
                { calculateApiService.applyPPToScore(score) }
            )

            val e5 = ScorePRService.getE5Param(
                user, null, score, "MR", beatmapApiService, calculateApiService
            ).toMap().toMutableMap()

            e5["match"] = match.statistics.matchID

            e5
        } else {
            AsyncMethodExecutor.awaitTripleCallableExecute(
                { beatmapApiService.applyBeatmapExtend(filteredScores) },
                { calculateApiService.applyStarToScores(filteredScores) },
                { calculateApiService.applyPPToScores(filteredScores) }
            )

            mapOf(
                "user" to user,
                "history_user" to null,
                "match" to match.statistics.matchID,
                "scores" to filteredScores,
                "rank" to List(filteredScores.size) { index ->
                    index + 1
                },
                "panel" to "MR"
            )
        }

        val image: ByteArray = try {
            if (filteredScores.size == 1) {
                imageService.getPanel(body, "E5")
            } else {
                imageService.getPanel(body, "A4")
            }
        } catch (e: Exception) {
            log.error("比赛最近成绩：渲染失败", e)
            throw IllegalStateException.Render("比赛最近成绩")
        }

        return image
    }

    private fun getMaximumStatistics(mode: OsuMode, stat: LazerStatistics): LazerStatistics {
        return when (mode) {
            OSU, OSU_RELAX, OSU_AUTOPILOT -> LazerStatistics(
                great = stat.great + stat.ok + stat.meh + stat.miss,
                largeBonus = stat.largeBonus,
                smallBonus = stat.smallBonus,
                sliderTailHit = stat.sliderTailHit,
                legacyComboIncrease = stat.legacyComboIncrease
            )

            TAIKO, TAIKO_RELAX -> LazerStatistics(
                great = stat.great + stat.ok + stat.meh,
                largeBonus = stat.largeBonus,
                smallBonus = stat.smallBonus
            )

            CATCH, CATCH_RELAX -> LazerStatistics(
                great = stat.great + stat.miss,
                largeTickHit = stat.largeTickHit + stat.largeTickMiss,
                smallTickHit = stat.smallTickHit + stat.smallTickMiss,
                largeBonus = stat.largeTickMiss,
            )

            MANIA -> LazerStatistics(
                perfect = stat.perfect + stat.great + stat.good + stat.ok + stat.meh + stat.miss,
                legacyComboIncrease = stat.legacyComboIncrease
            )

            else -> LazerStatistics()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MatchRecentService::class.java)

        private val REG_NUMBER_7_9 = Regex("\\d{7,9}")
        private val REG_NUMBER_1_2 = Regex("\\d{1,2}")
        private val REG_NUMBER_WITH_1_2 = Regex("(.+)\\D+(\\d{1,2})")
    }
}
