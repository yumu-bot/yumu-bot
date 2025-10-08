package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.LeaderBoardService.LeaderBoardParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.*
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min

@Service("LEADER_BOARD")
class LeaderBoardService(
    private val bindDao: BindDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
) : MessageService<LeaderBoardParam> {

    data class LeaderBoardParam(
        val bid: Long?,
        val type: String?,
        val range: ClosedRange<Int>,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isLegacy: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<LeaderBoardParam>,
    ): Boolean {
        val m1 = Instruction.LEADER_BOARD.matcher(messageText)
        val m2 = Instruction.LEGACY_LEADER_BOARD.matcher(messageText)

        val matcher: Matcher
        val isLegacy: Boolean

        if (m1.find()) {
            matcher = m1
            isLegacy = false
        } else if (m2.find()) {
            matcher = m2
            isLegacy = true
        } else return false

        val bid = matcher.group(FLAG_BID)?.toLongOrNull()

        val rangeStr: String? = matcher.group(FLAG_RANGE)

        val range = if (rangeStr.isNullOrBlank()) {
            1..50
        } else if (rangeStr.contains(REG_HYPHEN.toRegex())) {
            val split = rangeStr.trim().removePrefix(REG_HASH).split(REG_HYPHEN.toRegex()).map { it.trim() }

            val start = split.firstOrNull()?.toIntOrNull() ?: 1
            val end = split.lastOrNull()?.toIntOrNull() ?: 50

            start.clamp()..end.clamp()
        } else {
            val start = rangeStr.trim().removePrefix(REG_HASH).trim().toIntOrNull() ?: 1

            start.clamp()..start.clamp()
        }

        if (range.isEmpty()) {
            throw IllegalArgumentException.WrongException.Range()
        }

        val mode = OsuMode.getMode(matcher.group(FLAG_MODE))

        val type = getType(matcher.group(FLAG_TYPE))

        val mods = LazerMod.getModsList(matcher.group(FLAG_MOD))

        data.value = LeaderBoardParam(bid, type, range, mode, mods, isLegacy)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: LeaderBoardParam) {
        val bindUser = try {
            bindDao.getBindFromQQ(event.sender.id, true)
        } catch (e: BindException) {
            null
        }

        val beatmap: Beatmap =
            if (param.bid == null) {
                if (bindUser == null) {
                    throw IllegalArgumentException.WrongException.BeatmapID()
                }

                val score = scoreApiService.getRecentScore(bindUser, param.mode, 0, 1).firstOrNull()
                    ?: throw NoSuchElementException.RecentScore(bindUser.username, param.mode)

                beatmapApiService.getBeatmap(score.beatmapID)
            } else {
                try {
                    beatmapApiService.getBeatmap(param.bid)
                } catch (e: NetworkException.BeatmapException.NotFound) {
                    beatmapApiService.getBeatmapset(param.bid).getTopDiff()!!
                }
            }

        if (!beatmap.hasLeaderBoard) {
            throw NoSuchElementException.Leaderboard()
        }

        val mode = OsuMode.getConvertableMode(param.mode, beatmap.mode)

        if (bindUser?.isAuthorized != true && (param.mods.isNotEmpty() || param.type != "global")) {
            throw UnsupportedOperationException.NotOauthBind()
        }

        val scores: List<LazerScore> = try {
            scoreApiService.getLeaderBoardScore(bindUser, beatmap.beatmapID, mode, param.mods, param.type, param.isLegacy)
        } catch (e: NetworkException.ScoreException.UnprocessableEntity) {
            throw UnsupportedOperationException.NotSupporter()
        } catch (e: NetworkException.ScoreException.Unauthorized) {
            log.error("谱面排行榜：玩家掉绑", e)
            throw UnsupportedOperationException.NotOauthBind()
        } catch (e: Exception) {
            log.error("谱面排行榜：获取失败", e)
            throw IllegalStateException.Fetch("谱面排行榜")
        }

        if (scores.isEmpty())
            throw NoSuchElementException.LeaderboardScore()

        val start = param.range.start.clamp(max = scores.size)
        val end = param.range.endInclusive.clamp(max = scores.size)

        val ss = scores.take(end).drop(start - 1)

        val image = if (ss.isEmpty()) {
            throw IllegalArgumentException.WrongException.Range()
        } else if (ss.size == 1) {
            // 单成绩模式
            val score: LazerScore = ss.first()

            val user = try {
                userApiService.getOsuUser(score.userID, score.mode)
            } catch (e: Exception) {
                OsuUser(score.user)
            }

            val e5Param = ScorePRService.getE5Param(user, score, beatmap, start, "L", beatmapApiService, calculateApiService)

            imageService.getPanel(e5Param.toMap(), "E5")
        } else {
            // 多成绩模式
            calculateApiService.applyPPToScores(ss)

            ss.forEach { it.beatmap = beatmap }

            val body = mapOf(
                "beatmap" to beatmap,
                "scores" to ss,
                "start" to start,
                "is_legacy" to param.isLegacy
            )

            imageService.getPanel(body, "A3")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面排行：发送失败", e)
            throw IllegalStateException.Send("谱面排行")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LeaderBoardService::class.java)

        private fun Int.clamp(min: Int = 1, max: Int = 50): Int {
            return min(max(this, min), max)
        }

        private fun getType(string: String?): String {
            return when(string?.lowercase()) {
                "country", "countries", "c" -> "country"
                "friend", "friends", "f" -> "friend"
                else -> "global"
            }
        }
    }
}
