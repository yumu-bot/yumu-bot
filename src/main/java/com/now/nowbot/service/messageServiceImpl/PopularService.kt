package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.PopularService.PopularParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.*
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("POPULAR")
class PopularService(
    private val bindDao: BindDao,
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val botContainer: BotContainer,
): MessageService<PopularParam> {

    data class PopularParam(
        val range: CmdRange<Long>,
        val mode: OsuMode,
    )

    data class PopularInfo(
        @JsonProperty("group_id")
        val groupID: Long,

        @JsonProperty("member_count")
        val memberCount: Int,

        @JsonProperty("player_count")
        val playerCount: Int,

        @JsonProperty("score_count")
        val scoreCount: Int,

        @JsonProperty("beatmap_count")
        val beatmapCount: Int,

        @JsonProperty("mode")
        val mode: OsuMode,

        @JsonProperty("start_time")
        val startTime: OffsetDateTime,

        @JsonProperty("end_time")
        val endTime: OffsetDateTime,
    )

    data class PanelTData(
        @JsonProperty("info")
        val info: PopularInfo,

        @JsonProperty("popular")
        val popular: List<PopularBeatmap>,

        @JsonProperty("max_retry")
        val maxRetry: MaxRetry,

        @JsonProperty("mod_attr")
        val modAttr: List<Attr>,

        @JsonProperty("mod_max_percent")
        val modMaxPercent: Double,

        @JsonProperty("pp_attr")
        val ppAttr: List<Attr>,

        @JsonProperty("pp_max_percent")
        val ppMaxPercent: Double,
    )

    data class MaxRetry(
        @JsonProperty("beatmap_id")
        val beatmapID: Long = -1L,
        val count: Int = 0,
        @JsonProperty("user_id")
        val userID: Long = -1L,
    ) {
        @JsonProperty("user")
        var user: MicroUser? = null

        @JsonProperty("beatmap")
        var beatmap: Beatmap? = null

        constructor(beatmapID: Long, count: Int, userID: Long, user: MicroUser?, beatmap: Beatmap?) : this(beatmapID, count, userID) {
            user?.let { u -> this.user = u }
            beatmap?.let { b -> this.beatmap = b }
        }
    }

    data class PopularBeatmap(
        @JsonProperty("beatmap_id")
        val beatmapID: Long = -1L,
        val count: Int = 0,
        val accuracy: Double = 0.0,
        val combo: Int = 0,
        @JsonProperty("player_count")
        val playerCount: Int = 0,
        @JsonProperty("max_retry")
        val maxRetry: MaxRetry = MaxRetry(),
    ) {
        @JsonProperty("beatmap")
        var beatmap: Beatmap? = null

        companion object {
            fun toPopularBeatmap(scores: List<LazerScore>): PopularBeatmap {
                val beatmapID = scores.firstOrNull()?.beatmapID ?: -1L
                val count = scores.size
                val accuracy = scores.map { it.accuracy }.average()
                val combo = scores.map { it.maxCombo }.average().roundToInt()
                val playerCount = scores.map { it.userID }.toSet().size

                val maxGroup = scores.groupBy { it.userID }.toList().map { it.first to it.second.size }

                val maxRetryCount = maxGroup.maxOf { it.second }
                val maxRetryPlayerID = maxGroup.maxByOrNull { it.second }?.first ?: -1L

                return PopularBeatmap(beatmapID, count, accuracy, combo, playerCount, MaxRetry(beatmapID, maxRetryCount, maxRetryPlayerID))
            }
        }
    }

    data class Attr(
        val index: String,
        val count: Int,
        val percent: Double
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<PopularParam>): Boolean {
        val matcher = Instruction.POPULAR.matcher(messageText)

        if (!matcher.find()) return false

        if (event !is GroupMessageEvent) {
            throw UnsupportedOperationException.NotGroup()
        }

        /*
        if (!Permission.isSuperAdmin(event.sender.id)) return false

         */

        /*
        // TODO 临时做的次数限制
        val now = LocalDateTime.now()
        val before = now.minusSeconds(5)
        val result = serviceCallRepository.countBetween(before, now)

        if (result.map { it.service }.count { it.contains("POPULAR".toRegex()) } > 0) {
            throw UnsupportedOperationException("等一会再查询吧...")
        }

         */

        val group = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()
        val ranges = matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

        val mode = CmdUtil.getMode(matcher)

        val groupID: Long
        val start: Int
        val end: Int

        if (ranges == null) {
            if (group == null) {
                groupID = event.subject.id
                start = 0
                end = 2
            } else if (group < 1e6) {
                groupID = event.subject.id
                start = 0
                end = (group.toInt()).clamp(2, Int.MAX_VALUE)
            } else {
                groupID = group
                start = 0
                end = 2
            }
        } else if (ranges.size == 2) {
            groupID = group ?: event.subject.id
            start = 0
            end = ranges.firstOrNull()?.toIntOrNull() ?: 2
        } else {
            groupID = group ?: event.subject.id
            start = ranges.firstOrNull()?.toIntOrNull() ?: 0
            end = ranges.lastOrNull()?.toIntOrNull() ?: 2
        }

        val range = CmdRange(groupID, start, end)
        range.setZeroDay()

        data.value = PopularParam(range, mode.data!!)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: PopularParam) {

        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: BindException) {
            null
        }

        val mode = OsuMode.getMode(param.mode, me?.mode, bindDao.getGroupModeConfig(event))

        val bot = try {
            botContainer.robots[event.bot.botID] ?: throw TipsException("流行谱面：机器人为空")
        } catch (e: Exception) {
            log.info("流行谱面：机器人为空", e)
            throw NoSuchElementException("机器人实例为空。")
        }

        val groupInfo = try {
            bot.getGroupInfo(param.range.data!!, false)?.data ?: throw TipsException("流行谱面：获取群聊信息失败。")
        } catch (e: Exception) {
            log.info("流行谱面：获取群聊信息失败", e)
            throw NoSuchElementException("获取群聊信息失败。")
        }

        if ((groupInfo.memberCount ?: 1200) >= 1200) {
            throw IllegalArgumentException.ExceedException.GroupMembers()
        }

        val members = try {
            bot.getGroupMemberList(param.range.data!!, false)?.data ?: throw TipsException("流行谱面：获取群聊玩家失败。")
        } catch (e: Exception) {
            log.info("流行谱面：获取群聊玩家失败", e)
            throw NoSuchElementException("获取群聊玩家失败。")
        }

        if (members.isEmpty()) {
            throw NoSuchElementException.Group()
        }

        val qqIDs = members.map { member -> member.userId }

        // 存在的玩家
        val users = bindDao.getBindFromQQs(qqIDs)

        val now = OffsetDateTime.now()

        val before = now.minusDays(param.range.getDayStart().toLong()).clamp()
        val after = now.minusDays(param.range.getDayEnd().toLong()).clamp()

        val scores = try {
            scoreDao.getUsersRankedScore(users.map { it.userID }, mode.modeValue, after, before)
        } catch (e: Throwable) {
            log.error("流行谱面：查询错误", e)
            throw NetworkException("查询超时，有可能是天数太多了。")
        }.map { lite -> lite.toLazerScore() }

        if (scores.isEmpty()) {
            throw NoSuchElementException.ScorePeriod()
        }

        val scoreGroupByID = scores.groupBy { ls -> ls.beatmapID }

        val popular = scoreGroupByID
            .map { entry -> PopularBeatmap.toPopularBeatmap(entry.value) }
            .sortedByDescending { it.count }
            .sortedByDescending { it.playerCount }

        val shown = popular.take(5)

        // 全局最大重试：玩家 ID、谱面 ID、最大重试次数
        val maxRetryTriple = scores
            .groupBy { ls -> ls.userID }
            .map { user2Score ->

                val ss = user2Score.value
                    .groupBy { score -> score.beatmapID }
                    .maxBy { beatmapID2Score -> beatmapID2Score.value.size }.toPair()

                Triple(user2Score.key, ss.first, ss.second)
            }
            .maxBy { triple -> triple.third.size }

        // 玩家游玩次数
        /*
        val playerPlay: List<PlayerPlay> = scores
            .groupBy { ls -> ls.userID }
            .map { entry -> PlayerPlay(entry.key, entry.value.size) }
            .sortedByDescending { it.count }
            .take(5)

         */

        val beatmapCount = scores.groupBy { it.beatmapID }.count()

        val info = PopularInfo(param.range.data ?: -1L, members.size, users.size, scores.size, beatmapCount, mode, after, before)

        // 种类
        val modAttr = scores
            .flatMap { ls -> ls.mods }
            .groupBy { mod -> mod.acronym }
            .map { entry ->
                val count = entry.value.size
                val percent = count * 1.0 / scores.size

                Attr(entry.key, count, percent)
            }.sortedByDescending { attr -> attr.count }

        val modMaxPercent = modAttr.maxOfOrNull { it.percent } ?: 0.0

        /*
        val rankAttr = scores.groupBy {
            ls -> ls.rank
        }.map { entry ->
            val count = entry.value.size
            val percent = count * 1.0 / scores.size

            Attr(entry.key, count, percent)
        }.sortedByDescending { attr -> attr.count }

         */

        // pp
        val ppAttr = scores
            .filter { ls -> ls.pp > 0.1 }
            .groupBy { ls -> when(ls.pp.roundToInt()) {
                in (Int.MIN_VALUE ..< 50) -> "0"
                in 50 ..< 100 -> "50"
                in 100 ..< 200 -> "100"
                in 200 ..< 300 -> "200"
                in 300 ..< 400 -> "300"
                in 400 ..< 500 -> "400"
                in 500 ..< 600 -> "500"
                else -> "600"
            } }
            .map { entry ->
                val count = entry.value.size
                val percent = count * 1.0 / scores.size

                Attr(entry.key, count, percent)
            }.sortedByDescending { attr -> attr.count }

        val ppMaxPercent = ppAttr.maxOfOrNull { it.percent } ?: 0.0

        // 获取资源
        val beatmaps = beatmapApiService.getBeatmaps(
            (shown.map { it.beatmapID }.toSet() + maxRetryTriple.second).filter { it > 0L }
        ).associateBy { it.beatmapID }

        val maxRetryPlayers = userApiService.getUsers(
            shown.map { it.maxRetry.userID }.toSet() + maxRetryTriple.first
        ).associateBy { it.userID }

        // 赋值
        val maxRetry = MaxRetry(maxRetryTriple.second, maxRetryTriple.third.size, maxRetryTriple.first,
            maxRetryPlayers[maxRetryTriple.first], beatmaps[maxRetryTriple.second]
        )

        shown.forEach { p ->
            p.beatmap = beatmaps[p.beatmapID]
            p.maxRetry.user = maxRetryPlayers[p.maxRetry.userID]
        }

        val panelTData = PanelTData(info, shown, maxRetry, modAttr, modMaxPercent, ppAttr, ppMaxPercent)

        try {
            event.reply(getImage(panelTData))
        } catch (e: Exception) {
            try {
                event.reply(getText(panelTData))
            } catch (e1: Exception) {
                log.error("流行谱面：发送失败", e)
                throw e
            }
        }
    }

    private fun getImage(panelTData: PanelTData): ByteArray {
        return try {
            imageService.getPanel(panelTData, "T")
        } catch (e: NetworkException.RenderModuleException) {
            log.error("流行谱面：渲染失败", e)
            throw e
        }
    }

    private fun getText(panelTData: PanelTData): String {
        val info = panelTData.info
        val shown = panelTData.popular

        val sb = StringBuilder("""
            群聊：${info.groupID}
            群聊人数：${info.memberCount}
            绑定人数：${info.playerCount}
            可获取的成绩数：${info.scoreCount}
        """.trimIndent())

        for (i in 1..min(5, shown.size)) {
            val p = shown[i - 1]
            val b = p.beatmap

            sb.append("\n#$i ${b?.previewName ?: p.beatmapID}\n  ${p.count} plays, ${p.playerCount} players, ${String.format("%.2f", p.accuracy * 100.0)}%, ${p.combo}x")
        }

        return sb.toString()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PopularService::class.java)

        private fun OffsetDateTime.clamp(min: OffsetDateTime = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8)), max: OffsetDateTime = OffsetDateTime.now()): OffsetDateTime {
            return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(
                    this.toEpochSecond().clamp(min.toEpochSecond(), max.toEpochSecond())
                ),
                ZoneId.ofOffset("UTC", ZoneOffset.ofHours(8)))
        }

        private fun Long.clamp(min: Long = 1, max: Long = 50): Long {
            return min(max(this, min), max)
        }

        private fun Int.clamp(min: Int = 1, max: Int = 50): Int {
            return min(max(this, min), max)
        }
    }
}