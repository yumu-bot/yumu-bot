package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode.Companion.orElse
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.Attribute
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.BeatmapAnalysis
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.Mapper
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.UserIDUtil
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.roundToInt

@Service("UU_BA")
class UUBAService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val bindDao: BindDao,
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<BAParam>
    ): Boolean {

        val matcher = Instruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BAParam): ServiceCallStatistic? {
        val lines = param.getText()

        val panelParam = lines.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val image = imageService.getPanelAlpha(*panelParam)

        try {
            event.replyAsync(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("最好成绩分析（文字版）")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.mode)
    }

    override fun accept(event: MessageEvent, messageText: String): BAParam? {
        val matcher = OfficialInstruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: BAParam): MessageChain? {
        val lines = param.getText()
        return MessageChain(lines)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): BAParam {

        val isMyself = AtomicBoolean(false)
        val mode = InstructionUtil.getMode(matcher)
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        // 1. 使用 if 表达式并结合解构声明，集中处理数据的获取逻辑
        val (user, bests) = if (id != null) {
            val m = mode.data
                .orElse(bindDao.getGroupMode(event))
                .orElse(bindDao.getBindModeFromID(id))

            if (m.isNotDefault) {
                AsyncMethodExecutor.awaitPair(
                    { userApiService.getOsuUser(id, m) },
                    { scoreApiService.getBestScores(id, m) }
                )
            } else {
                val fetchedUser = userApiService.getOsuUser(id)
                val fetchedBests = scoreApiService.getBestScores(id, fetchedUser.mode)
                fetchedUser to fetchedBests
            }
        } else {
            val fetchedUser = InstructionUtil.getUserWithoutRange(event, matcher, mode, isMyself)
            val fetchedBests = scoreApiService.getBestScores(fetchedUser.userID, fetchedUser.mode)
            fetchedUser to fetchedBests
        }

        AsyncMethodExecutor.awaitTriple(
            { BeatmapUtil.applyBeatmapChanges(bests) },
            { calculateApiService.applyStarToScores(bests) },
            { beatmapApiService.applyBeatmapExtend(bests) }
        )

        return BAParam(user, bests, isMyself.get(), emptyList(), 2)

    }

    companion object {
        fun BAParam.getText(): String {
            val map = this.toMap(2)

            @Suppress("UNCHECKED_CAST")
            val length = map["length_attr"] as List<BeatmapAnalysis>

            @Suppress("UNCHECKED_CAST")
            val combo = map["combo_attr"] as? List<BeatmapAnalysis> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val star = map["star_attr"] as? List<BeatmapAnalysis> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val bpm = map["bpm_attr"] as? List<BeatmapAnalysis> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val mappers = map["favorite_mappers"] as? List<Mapper> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val modsAttribute = map["mods_attr"] as? List<Attribute> ?: emptyList()

            val l = length.map { it.ranking to it.length }.toResult { it.secondsToTime() }
            val c = combo.map { it.ranking to it.combo }.toResult(suffix = "x") { it.toString() }
            val r = star.map { it.ranking to it.star }.toResult(suffix = "*") { it.to2DigitString() }
            val m = bpm.map { it.ranking to it.bpm }.toResult { it.toDouble().to2DigitString() }

            return """
                ${user.username}: ${user.mode.fullName}
                ---
                [length]:
                $l
                ---
                [combo]:
                $c
                ---
                [star]:
                $r
                ---
                [bpm]:
                $m
            """.trimIndent() + "\n---\n[mappers]:\n" + mappers.mapperToLine(5) + "\n---\n[mods]:\n" + modsAttribute.attrToLine(5)
        }

        /**
         * 第一行和第二行的内容，第一行的是平均，第二行是最大中位最小
         * @param suffix 后缀，会添加到所有地方
         * @param function 格式化 triple 第二个值，用于时间的显示
         */
        private fun <T> List<Pair<Int, T>>.toResult(
            suffix: String = "",
            function: (T) -> String = { it.toString() }
        ): String {
            val max = this[0].toLine(suffix, function)
            val mid = this[1].toLine(suffix, function)
            val min = this[2].toLine(suffix, function)

            return "max: $max, mid: $mid, min: $min"
        }

        private fun <T> Pair<Int, T>.toLine(
            suffix: String = "",
            function: (T) -> String = { it.toString() },
        ): String {
            return "#${this.first} ${function.invoke(this.second)}${suffix}"
        }

        private fun Int?.secondsToTime(): String {
            if (this == null) return "00:00"

            val minutes = this / 60
            val seconds = this % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        private val df = DecimalFormat("#.##")

        private fun Double.to2DigitString(): String {
            return df.format(this)
        }

        private fun List<Mapper>.mapperToLine(take: Int = -1): String {
            val list = if (take > 0) {
                this.take(take)
            } else {
                this
            }

            if (list.isEmpty()) return "no mappers."

            return list.joinToString("\n") {
                "${it.username}: ${it.mapCount}x ${it.ppCount.roundToInt()}PP"
            }
        }

        private fun List<Attribute>.attrToLine(take: Int = -1): String {
            val list = if (take > 0) {
                this.take(take)
            } else {
                this
            }

            if (list.isEmpty()) return "all no-mod."

            return list.joinToString("\n") {
                "${it.index}: ${it.mapCount}x ${it.ppCount.roundToInt()}PP (${it.percent.to2DigitString()}%)"
            }
        }
    }
}
