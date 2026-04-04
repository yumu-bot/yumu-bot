package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod.Companion.filterMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_PAGE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.regex.Matcher

@Service("GROUP_LEADER_BOARD")
class GroupLeaderBoardService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val botContainer: BotContainer,
    private val scoreDao: ScoreDao,
    private val dao: ServiceCallStatisticsDao,
    private val bindDao: BindDao,
): MessageService<GroupLeaderBoardService.GroupLeaderBoardParam> {

    private fun List<LazerScore>.applyMicroUser() {
        val userIDs = this.map { it.userID }.toSet()

        val users = userApiService.getUsers(userIDs, isVariant = false, isBackground = false).associateBy { it.userID }

        this.onEach { score ->
            users[score.userID]?.let {
                score.user = it
            }
        }
    }

    data class GroupLeaderBoardParam(
        val user: OsuUser?,
        val beatmap: Beatmap,
        val mode: OsuMode,
        val scores: List<LazerScore>,

        val group: Long,
        val page: Int = 1,

        @field:JsonProperty("max_page")
        val maxPage: Int = 1,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GroupLeaderBoardParam>
    ): Boolean {
        val matcher = Instruction.GROUP_LEADER_BOARD.matcher(messageText)

        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: GroupLeaderBoardParam
    ): ServiceCallStatistic {
        val image = imageService.getPanel(param, "A16")

        event.reply(image)

        return ServiceCallStatistic.build(event, beatmapID = param.beatmap.beatmapID, mode = param.mode).apply {
            this.setParam(mapOf(
                "groups" to listOf(event.subject.contactID)
            ))
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): GroupLeaderBoardParam {
        if (event.subject !is Group) {
            throw UnsupportedOperationException.NotGroup()
        }

        val mode = InstructionUtil.getMode(matcher, bindDao.getGroupModeConfig(event)).data

        val id = matcher.group(FLAG_BID)?.toLongOrNull()

        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1

        val beatmapID = id ?: dao.getLastBeatmapID(event.subject.contactID, null,
            LocalDateTime.now().minusHours(24L)) ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val beatmap = beatmapApiService.getBeatmap(beatmapID)

        val m = OsuMode.getConvertableMode(mode, beatmap.mode)

        val mods = InstructionUtil.getMod(matcher)

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

//        val members = arrayListOf(1340691940L)

        if (members.isEmpty()) {
            log.warn("群内排行榜：机器人 ${bot.selfId} 获取 ${event.subject.contactID} 群聊玩家为空。")
            throw TipsException("群内排行榜：群成员列表是空的。")
        }

        val userIDs = bindDao.getAllQQBindUser(members).map { it.uid }

        val scores = scoreDao.getBeatmapScores(userIDs, beatmapID, m).ifEmpty {
            throw NoSuchElementException.GroupBeatmapScore(beatmap.previewName)
        }

        val filtered = scores.filterMod(mods,
            {
                throw NoSuchElementException.GroupBeatmapScoreFiltered(beatmap.previewName)
            })

        beatmapApiService.applyBeatmapExtendForSameScore(filtered, beatmap)
        calculateApiService.applyPPToScoresWithSameBeatmap(filtered)

        val (split, currentPage, maxPage) = DataUtil.splitPage(filtered.sortedWith(
            compareBy<LazerScore> { if (it.rank == "F") 1 else 0 }
                .thenByDescending { it.pp }
        ), page, 50)

        split.applyMicroUser()

        val user = runCatching {
            userApiService.getOsuUser(bindDao.getBindFromQQOrNull(event.sender.contactID) ?: return@runCatching null, m)
        }.getOrNull()

        return GroupLeaderBoardParam(user, beatmap, m, split, event.subject.contactID, currentPage, maxPage)
    }
    
    companion object {
        private val log = LoggerFactory.getLogger(javaClass)
    }
}