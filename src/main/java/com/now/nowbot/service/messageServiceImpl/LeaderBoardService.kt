package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.IDType
import com.now.nowbot.model.enums.IDType.*
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.LeaderBoardService.LeaderBoardParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.*
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.collections.drop
import kotlin.collections.ifEmpty

@Service("LEADER_BOARD")
class LeaderBoardService(
    private val bindDao: BindDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val botContainer: BotContainer,
    private val dao: ServiceCallStatisticsDao,
    private val scoreDao: ScoreDao
) : MessageService<LeaderBoardParam> {

    data class LeaderBoardParam(
        val bindUser: BindUser?,
        val beatmap: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isLegacy: Boolean = false,
        val isSSPanel: Boolean = false
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

        data.value = getParam(event, matcher, isLegacy)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: LeaderBoardParam): ServiceCallStatistic? {

        val image = if (param.scores.isEmpty()) {
            throw IllegalArgumentException.WrongException.Range()
        } else if (param.isSSPanel) {

            val user = userApiService.getOsuUser(param.bindUser!!.userID, param.mode)

            val body = mapOf(
                "user" to user,

                "rank" to List(param.scores.size) { i -> i + 1},
                "scores" to param.scores,
                "panel" to "SS",
                "compact" to (param.scores.size > 100)
            )

            imageService.getPanel(body, "A5")
        } else if (param.scores.size == 1) {
            // 单成绩模式
            val score: LazerScore = param.scores.first()

            val user = score.user.toOsuUser()

            val e5Param = ScorePRService.getE5ParamForFilteredScore(user, null, score, "L", beatmapApiService, calculateApiService)

            imageService.getPanel(e5Param.toMap(), "E5")
        } else {
            // 多成绩模式

            val body = mapOf(
                "user" to param.bindUser,
                "beatmap" to param.beatmap,
                "scores" to param.scores,
                "start" to (param.scores.firstOrNull()?.ranking ?: 1),
                "is_legacy" to param.isLegacy
            )

            imageService.getPanel(body, "A3")
        }

        try {
            event.replyAsync(image)
        } catch (e: Exception) {
            log.error("谱面排行：发送失败", e)
            throw IllegalStateException.Send("谱面排行")
        }

        return ServiceCallStatistic.builds(event,
            beatmapIDs = listOf(param.beatmap.beatmapID),
            beatmapsetIDs = listOf(param.beatmap.beatmapsetID),
            userIDs = param.scores.map { it.userID },
            modes = listOf(param.mode),
        )
    }



    private fun getParam(event: MessageEvent, matcher: Matcher, isLegacy: Boolean): LeaderBoardParam {
        val (inputType, inputID) = IDType.parse(matcher.group(FLAG_TYPE), matcher.group(FLAG_ID))

        val rangeStr: String = matcher.group(FLAG_PAGE) ?: ""

        val selected = rangeStr.trim()
            .replace(REG_HASH.toRegex(), "")
            .toIntOrNull()

        val range: IntRange = if (selected != null) {
            val s = selected.coerceIn(1, 50)

            s..s
        } else {
            1..50
        }

        val inputMode = OsuMode.getMode(matcher.group(FLAG_MODE), bindDao.getGroupModeConfig(event))

        val type = getType(matcher.group(FLAG_TYPE2))

        val mods = LazerMod.getModsList(matcher.group(FLAG_MOD))

        val beatmap: Beatmap = if (inputID != null) {
            when (inputType) {
                BeatmapID -> runCatching {
                    beatmapApiService.getBeatmap(inputID)
                }.recoverCatching {
                    beatmapApiService.getBeatmapset(inputID).getTopDiff()!!
                }.getOrThrow()

                BeatmapsetID -> runCatching {
                    beatmapApiService.getBeatmapset(inputID).getTopDiff()!!
                }.recoverCatching {
                    beatmapApiService.getBeatmap(inputID)
                }.getOrThrow()
            }
        } else {
            val beforeBeatmapID = dao.getLastBeatmapID(event)
                ?: throw IllegalArgumentException.WrongException.BeatmapID()

            beatmapApiService.getBeatmap(beforeBeatmapID)
        }

        val isSSPanel: Boolean
        
        val mode = OsuMode.getConvertableMode(inputMode, beatmap.mode)

        calculateApiService.applyStarToBeatmap(beatmap, mode, mods)

        val bindUser = bindDao.getBindFromQQOrNull(event.sender.contactID)

        val any = matcher.group(FLAG_ANY)

        val conditions = DataUtil.getConditions(any, ScoreFilter.entries.map { it.regex })

        val scores: List<LazerScore> = if (!beatmap.hasLeaderBoard) {
            val beatmapID = beatmap.beatmapID

            val specials = if (event.subject is Group) {
                event.replyAndRecallAsync("目标谱面没有榜。正在为您查询数据库里，群内的谱面排行榜。")

                val bot = botContainer.robots[event.bot?.botID]
                    ?: run {
                        log.warn("群内排行榜：机器人 ${event.bot?.botID} 为空。")
                        throw TipsException("群内排行榜：机器人为空")
                    }

                val members = bot.getGroupMemberList(event.subject.contactID, false)?.data
                    ?.mapNotNull { it.userId }
                    ?: run {
                        log.warn("群内排行榜：机器人 ${bot.selfId} 获取 ${event.subject.contactID} 群聊玩家失败。")
                        throw TipsException("群内排行榜：机器人获取群成员失败。")
                    }

                if (members.isEmpty()) {
                    log.warn("群内排行榜：机器人 ${bot.selfId} 获取 ${event.subject.contactID} 群聊玩家为空。")
                    throw TipsException("群内排行榜：群成员列表是空的。")
                }

                val userIDs = bindDao.getAllQQBindUser(members).map { it.uid }

                isSSPanel = false

                scoreDao.getBeatmapScores(userIDs, beatmapID, mode).ifEmpty {
                    throw NoSuchElementException.GroupBeatmapScore(beatmap.previewName)
                }
            } else {
                if (bindUser == null) {
                    throw BindException.NotBindException.YouNotBind()
                }

                event.replyAndRecallAsync("目标谱面没有榜。正在为您查询您自己的成绩。")

                isSSPanel = true

                val userScores = scoreDao.getBeatmapScores(bindUser.userID, beatmap, mode)
                beatmapApiService.applyVersion(userScores)

                userScores
            }

            beatmapApiService.applyBeatmapExtendForSameScore(specials, beatmap)
            calculateApiService.applyPPToScoresWithSameBeatmap(specials)

            val filteredScores = ScoreFilter.filterScores(
                List(specials.size) { it + 1 }.zip(specials).onEach {
                        (i, score) -> score.ranking = i + 1
                }.toMap(),
                conditions).values.toList().ifEmpty {
                throw NoSuchElementException.GroupBeatmapScoreFiltered(beatmap.previewName)
            }

            filteredScores
        } else {
            isSSPanel = false

            if (bindUser?.isTokenAvailable == null && (mods.isNotEmpty() || type != "global")) {
                if (bindUser?.hasToken == true) {
                    throw UnsupportedOperationException.ExpiredOauthBind()
                } else {
                    throw UnsupportedOperationException.NoOauthBind()
                }
            }

            runCatching {
                val scores = scoreApiService.getLeaderBoardScore(bindUser, beatmap.beatmapID, mode, mods, type, isLegacy)

                beatmapApiService.applyBeatmapExtendForSameScore(scores, beatmap)
                calculateApiService.applyPPToScoresWithSameBeatmap(scores)

                val filteredScores = ScoreFilter.filterScores(
                    List(scores.size) { it + 1 }.zip(scores)
                        .onEach {
                            (i, score) -> score.ranking = i + 1
                        }.toMap(),
                    conditions
                ).values.toList().ifEmpty {
                    throw NoSuchElementException.LeaderboardScoreFiltered(beatmap.previewName)
                }

                filteredScores
            }.onFailure { e ->
                when(e) {
                    is NetworkException.ScoreException.UnprocessableEntity ->
                        throw UnsupportedOperationException.NotSupporter()

                    is NetworkException.ScoreException.Unauthorized -> {
                        log.error("谱面排行榜：玩家掉绑", e)
                        if (bindUser?.hasToken == true) {
                            throw UnsupportedOperationException.ExpiredOauthBind()
                        } else {
                            throw UnsupportedOperationException.NoOauthBind()
                        }
                    }

                    else -> {
                        log.error("谱面排行榜：获取失败", e)
                        throw IllegalStateException.Fetch("谱面排行榜")
                    }
                }
            }.getOrThrow()
        }

        if (scores.isEmpty()) {
            if (type == "global") {
                throw NoSuchElementException.LeaderboardScore()
            } else {
                throw NoSuchElementException.LeaderboardScoreType(type)
            }
        }

        val start = range.first.coerceIn(1, scores.size)
        val end = range.last.coerceIn(1, scores.size)

        val ss = scores.drop(start - 1).take(end - start + 1)

        return LeaderBoardParam(bindUser, beatmap, ss, mode, mods, isLegacy, isSSPanel)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LeaderBoardService::class.java)

        private fun getType(string: String?): String {
            return when(string?.lowercase()) {
                "country", "countries", "c" -> "country"
                "friend", "friends", "f" -> "friend"
                "team", "clan", "t" -> "team"
                else -> "global"
            }
        }
    }
}
