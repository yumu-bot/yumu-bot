package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.ValueMod
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.Attr
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.InstructionUtil.getUserWithoutRange
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.collections.component1
import kotlin.collections.component2
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.Mapper
import kotlin.math.roundToInt

@Service("UU_BA")
class UUBAService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
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
            event.reply(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("最好成绩分析（文字版）")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
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
        val mode = getMode(matcher)

        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id, mode.data!!) },
                {
                    val ss = scoreApiService.getBestScores(id, mode.data!!)

                    calculateApiService.applyBeatMapChanges(ss)
                    calculateApiService.applyStarToScores(ss)

                    ss
                }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            user = getUserWithoutRange(event, matcher, mode, isMyself)
            bests = scoreApiService.getBestScores(user.userID, mode.data)

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)
        }

        val mapperIDs = bests.flatMap { it.beatmap.mapperIDs }.toSet()

        val async2 = AsyncMethodExecutor.awaitPairCallableExecute(
            { beatmapApiService.extendBeatmapInScore(bests) },
            { userApiService.getUsers(mapperIDs) },
        )

        return BAParam(user, async2.first, isMyself.get(), async2.second, 2)

    }

    companion object {

        /**
         * 重写自己的获取
         */
        private fun <T> List<LazerScore>.sortCount2(
            username: String,
            sortedByDescending: (LazerScore) -> T
        ): List<Triple<Int, Number, LazerScore>> where T : Number, T : Comparable<T> {
            if (this.isEmpty()) throw NoSuchElementException.BestScore(username)

            val sorted: List<Triple<Int, Number, LazerScore>> = this
                .mapIndexed { index, score ->
                    val value = sortedByDescending(score)
                    Triple(index + 1, value, score)
                }
                .sortedByDescending { it.second }  // 这里可以直接比较，因为 T 是 Comparable<T>

            val max = sorted.first()
            val mid = sorted[sorted.size / 2]
            val min = sorted.last()

            return listOf(
                Triple(-1, sorted.map { it.second.toString().toDouble() }.average(), LazerScore()),
                max, mid, min,
            )
        }


        fun BAParam.getText(): String {
            val name = this.user.username
            val mode = this.user.currentOsuMode.fullName

            val length = this.bests.sortCount2(name) { score -> score.beatmap.totalLength }
            val combo = this.bests.sortCount2(name) { score -> score.maxCombo }
            val star = this.bests.sortCount2(name) { score -> score.beatmap.starRating }
            val bpm = this.bests.sortCount2(name) { score -> score.beatmap.bpm }

            val mapperMap = bests
                .associateWith { it.beatmap.mapperIDs }
                .flatMap { (score, mappers) ->
                    mappers.map { mapper -> mapper to score }
                }.groupBy({ it.first }, { it.second })

            val mapperUserInfoMap = mappers.associateBy { it.userID }

            val mapperList = mapperMap.map { entry -> entry.key
                val microUser = mapperUserInfoMap[entry.key]

                Mapper(
                    avatarUrl = microUser?.avatarUrl ?: "https://a.ppy.sh/${entry.key}",
                    username = microUser?.username ?: "UID: ${entry.key}",
                    mapCount = entry.value.size,
                    ppCount = entry.value.sumOf { it.pp }.toFloat(),
                    // ppCount = entry.value.sumOf { it.weight?.pp ?: 0.0 }.toFloat(),
                )
            }.sortedByDescending { it.ppCount }

            val modsPPMap: MultiValueMap<String, Double> = LinkedMultiValueMap()

            bests.map { best ->
                val m = best.mods.filter {
                    if (it is ValueMod) {
                        it.value != 0
                    } else {
                        true
                    }
                }

                if (m.isNotEmpty()) {
                    m.forEach {
                        modsPPMap.add(it.acronym, best.weight!!.pp)
                    }
                }
            }

            val modsAttr: List<Attr> = run {
                val modsAttrTmp: MutableList<Attr> = ArrayList(modsPPMap.size)
                modsPPMap.forEach { (mod: String, value: MutableList<Double?>) ->
                    val attr = Attr(
                        mod, value.filterNotNull().size, value.filterNotNull().sum(), value.filterNotNull().average()
                    )
                    modsAttrTmp.add(attr)
                }

                modsAttrTmp.sortedByDescending { it.ppCount }
            }

            val l = length.toResult { it.toInt().secondsToTime() }
            val c = combo.toResult(suffix = "x") { it.toInt().toString() }
            val r = star.toResult(suffix = "*") { it.toDouble().to2DigitString() }
            val m = bpm.toResult { it.toDouble().to2DigitString() }

            return """
                $name: $mode
                ---
                [length]: ${l.first}
                ${l.second}
                ---
                [combo]: ${c.first}
                ${c.second}
                ---
                [star]: ${r.first}
                ${r.second}
                ---
                [bpm]: ${m.first}
                ${m.second}
            """.trimIndent() + "\n---\n[mappers]:\n" + mapperList.mapperToLine(5) + "\n---\n[mods]:\n" + modsAttr.attrToLine(5)
        }

        /**
         * 第一行和第二行的内容，第一行的是平均，第二行是最大中位最小
         * @param suffix 后缀，会添加到所有地方
         * @param function 格式化 triple 第二个值，用于时间的显示
         */
        private fun <T> List<Triple<Int, T, LazerScore>>.toResult(
            suffix: String = "",
            function: (T) -> String = { it.toString() }
        ): Pair<String, String> {
            val average = "${function.invoke(this[0].second)}${suffix}"
            val max = this[1].toLine(suffix, function)
            val mid = this[2].toLine(suffix, function)
            val min = this[3].toLine(suffix, function)

            val first = "average: $average"

            val second = "max: $max, mid: $mid, min: $min"

            return first to second
        }

        private fun <T> Triple<Int, T, LazerScore>.toLine(
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

        private fun List<Attr>.attrToLine(take: Int = -1): String {
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
