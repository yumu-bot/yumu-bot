package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("POPULAR")
class PopularService(
    private val bindDao: BindDao,
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService
): MessageService<CmdRange<Long>> {

    data class PopularBeatmap(
        val beatmapID: Long = -1L,
        val count: Int = 0,
        val accuracy: Double = 0.0,
        val combo: Int = 0,
        val player: Int = 0,
    ) {
        companion object {
            fun toPopularBeatmap(scores: List<LazerScore>): PopularBeatmap {
                val beatmapID = scores.firstOrNull()?.beatmapID ?: -1L
                val count = scores.size
                val accuracy = scores.map { it.accuracy }.average()
                val combo = scores.map { it.maxCombo }.average().roundToInt()
                val player = scores.map { it.userID }.toSet().size

                return PopularBeatmap(beatmapID, count, accuracy, combo, player)
            }
        }
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<CmdRange<Long>>): Boolean {
        val matcher = Instruction.POPULAR.matcher(messageText)

        if (!matcher.find()) return false

        if (event !is GroupMessageEvent) {
            throw UnsupportedOperationException.NotGroup()
        }

        if (!Permission.isSuperAdmin(event.sender.id)) return false

        val group = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()
        val ranges = matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

        val groupID: Long
        val start: Int
        val end: Int

        if (ranges == null) {
            if (group == null || group < 1e6) {
                groupID = event.subject.id
                start = 0
                end = (group?.toInt() ?: event.subject.id.toInt()).clamp(1, Int.MAX_VALUE)
            } else {
                groupID = group
                start = 0
                end = 7
            }
        } else if (ranges.size == 1) {
            groupID = event.subject.id
            start = 0
            end = ranges.firstOrNull()?.toIntOrNull() ?: 7
        } else {
            groupID = event.subject.id
            start = ranges.firstOrNull()?.toIntOrNull() ?: 0
            end = ranges.lastOrNull()?.toIntOrNull() ?: 7
        }

        data.value = CmdRange(groupID, start, end)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: CmdRange<Long>) {
        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: BindException) {
            null
        }

        val group = event.bot.groups.associateBy { it.id }[param.data!!]
            ?: throw NoSuchElementException.Group()

        val mode = OsuMode.getMode(me?.mode, bindDao.getGroupModeConfig(event))

        val qqIDs = group.allUser.map { it.id }

        // 记录的玩家
        val qqUsers = bindDao.getAllQQBindUser(qqIDs)

        // 存在的玩家
        val users = qqUsers.map { bindDao.getBindFromQQ(it.qid) }

        val now = OffsetDateTime.now()

        val before = now.minusDays(param.getDayStart().toLong()).clamp()
        val after = now.minusDays(param.getDayEnd().toLong()).clamp()

        val scoreChunk = users.map {
            scoreDao.scoreRepository.getUserRankedScore(it.userID, mode.modeValue, after, before)
        }

        val scoreGroup = scoreChunk.asSequence()
            .flatten()
            .groupBy { it.beatmapId }
            .mapValues { entry -> entry.value.map { s -> s.toLazerScore() } }

        val popular = scoreGroup
            .map { entry -> PopularBeatmap.toPopularBeatmap(entry.value) }
            .sortedBy { it.count }

        // 目前不清楚如果遇到了不存在的谱面该怎么解决
        val beatmaps = AsyncMethodExecutor.awaitCallableExecute(
            { popular.take(5).associate { it.beatmapID to beatmapApiService.getBeatmapFromDatabase(it.beatmapID) } }
        )

        val sb = StringBuilder("""
            群聊：${group.id}
            群聊人数：${group.allUser.size}
            绑定人数：${users.size}
            可获取的成绩数：${scoreChunk.flatten().size}
        """.trimIndent())

        for (i in 1..min(5, popular.size)) {
            val p = popular[i - 1]
            val b = beatmaps[p.beatmapID]

            sb.append("\n#$i ${b?.previewName ?: p.beatmapID}\n  ${p.count} plays, ${p.player} players, ${String.format("%.2f", p.accuracy)}%, ${p.combo}x")
        }

        event.reply(sb.toString())
    }

    companion object {

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