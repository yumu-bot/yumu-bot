package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.*
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("POPULAR")
class PopularService(
    private val bindDao: BindDao,
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val botContainer: BotContainer
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
                end = 1
            }
        } else if (ranges.size == 1) {
            groupID = group ?: event.subject.id
            start = 0
            end = ranges.firstOrNull()?.toIntOrNull() ?: 1
        } else {
            groupID = group ?: event.subject.id
            start = ranges.firstOrNull()?.toIntOrNull() ?: 0
            end = ranges.lastOrNull()?.toIntOrNull() ?: 1
        }

        data.value = CmdRange(groupID, start, end)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: CmdRange<Long>) {
        val t = StopWatch()

        t.start("group")

        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: BindException) {
            null
        }

        val mode = OsuMode.getMode(me?.mode, bindDao.getGroupModeConfig(event))

        val bot = try {
            botContainer.robots[event.bot.botID] ?: throw TipsException("流行谱面：机器人为空")
        } catch (e: Exception) {
            log.info("流行谱面：机器人为空", e)
            throw NoSuchElementException("机器人实例为空。")
        }

        /*
        val bot = event.bot ?: run {
            log.info("流行谱面：机器人为空")
            throw NoSuchElementException("机器人实例为空。")
        }

         */

        val memberData = try {
            bot.getGroupMemberList(param.data!!, true) ?: throw TipsException("流行谱面：获取群聊信息失败。")
        } catch (e: Exception) {
            log.info("流行谱面：获取群聊信息失败", e)
            throw NoSuchElementException("获取群聊信息失败。")
        }

        val members = try {
            memberData.data ?: throw TipsException("流行谱面：获取群聊玩家失败")
        } catch (e: Exception) {
            log.info("流行谱面：获取群聊玩家失败", e)
            throw NoSuchElementException("获取群聊玩家失败。")
        }

        val qqIDs = members.map { member -> member.userId }

        t.stop()
        t.start("user")

        // 存在的玩家
        val users = bindDao.getBindFromQQs(qqIDs)

        val now = OffsetDateTime.now()

        val before = now.minusDays(param.getDayStart().toLong()).clamp()
        val after = now.minusDays(param.getDayEnd().toLong()).clamp()

        t.stop()
        t.start("score")

        /*
        val scoreChunk = try {
            AsyncMethodExecutor.awaitCallableExecute({
                users.map {
                    scoreDao.scoreRepository.getUserRankedScore(it.userID, mode.modeValue, after, before)
                }}, Duration.ofSeconds(30L + users.size / 30)
            )
        } catch (e: Throwable) {
            log.error("流行谱面：查询错误", e)
            throw NetworkException("查询超时，有可能是天数太多了。")
        }

         */

        val scores = try {
            scoreDao.getUsersRankedScore(users.map { it.userID }, mode.modeValue, after, before)
        } catch (e: Throwable) {
            log.error("流行谱面：查询错误", e)
            throw NetworkException("查询超时，有可能是天数太多了。")
        }

        t.stop()
        t.start("popular")

        val scoreGroup = scores
            .map { lite -> lite.toLazerScore() }
            .groupBy { ls -> ls.beatmapID }

        val popular = scoreGroup
            .map { entry -> PopularBeatmap.toPopularBeatmap(entry.value) }
            .sortedByDescending { it.count }
            .sortedByDescending { it.player }

        t.stop()
        t.start("beatmap")

        val beatmaps = beatmapApiService.getBeatmaps(popular.take(5).map { it.beatmapID }).associateBy { it.beatmapID }

        val sb = StringBuilder("""
            群聊：${param.data}
            群聊人数：${members.size}
            绑定人数：${users.size}
            可获取的成绩数：${scores.size}
        """.trimIndent())

        for (i in 1..min(5, popular.size)) {
            val p = popular[i - 1]
            val b = beatmaps[p.beatmapID]

            sb.append("\n#$i ${b?.previewName ?: p.beatmapID}\n  ${p.count} plays, ${p.player} players, ${String.format("%.2f", p.accuracy * 100.0)}%, ${p.combo}x")
        }

        /*
        val image = imageService.getPanelA6(sb.toString())
        event.reply(image)

         */
        event.reply(sb.toString())

        t.stop()
        log.info(t.prettyPrint())
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