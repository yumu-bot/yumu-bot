package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.takeIfConvertable
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService.MapFilter.*
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_MOD
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import com.now.nowbot.util.command.REG_OPERATOR_WITH_SPACE
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.regex.Matcher
import kotlin.collections.lastOrNull

@Service("MAP")
class MapStatisticsService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao,
    // private val bindDao: BindDao
) : MessageService<MapStatisticsService.MapStatisticsParam>, TencentMessageService<MapStatisticsService.MapStatisticsParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MapStatisticsParam>
    ): Boolean {
        val matcher = Instruction.MAP.matcher(messageText)
        val matcher2 = Instruction.MAP_LAZER.matcher(messageText)
        if (matcher.find()) {
            data.value = getParam(event, matcher, isLazer = false)
            return true
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, isLazer = true)
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: MapStatisticsParam
    ): ServiceCallStatistic? {
        val panel = param.getPanelRParam()

        val image = imageService.getPanel(panel, "R")

        event.replyAsync(image)

        return ServiceCallStatistic.build(
            event,
            beatmapID = param.beatmap.beatmapID,
            beatmapsetID = param.beatmapset.beatmapsetID,
            userID = null,
            mode = param.expected.mode
        )
    }

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): MapStatisticsParam? {

        val matcher = OfficialInstruction.MAP.matcher(messageText)
        val matcher2 = OfficialInstruction.MAP_LAZER.matcher(messageText)
        return if (matcher.find()) {
            getParam(event, matcher, isLazer = false)
        } else if (matcher2.find()) {
            getParam(event, matcher2, isLazer = true)
        } else null
    }

    override fun reply(
        event: MessageEvent,
        param: MapStatisticsParam
    ): MessageChain? {
        val panel = param.getPanelRParam()

        val image = imageService.getPanel(panel, "R")

        return MessageChain(image)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isLazer: Boolean = false): MapStatisticsParam {
        // val groupMode = bindDao.getGroupModeConfig(event)

        val (beatmapset, beatmap) = beatmapApiService.getBeatmapsetAndTopBeatmapFromAnyID(matcher) { dao.getLastBeatmapID(event) }

        val any: String? = matcher.group(FLAG_ANY)

        val (accuracy, combo, misses) = parseAccuracyAndCombo(any)

        val bc = beatmap.maxCombo ?: 0

        val c = if (combo in 0.0..1.0) {
            (bc * combo).toInt()
        } else {
            if (bc > 0) {
                combo.toInt().coerceIn(0..bc)
            } else {
                combo.toInt()
            }
        }

        val mods = LazerMod.getModsList(matcher.group(FLAG_MOD))
        val mode = matcher.group(FLAG_MODE).toOsuMode()
            .takeIfConvertable(beatmap)

        val expected = Expected(mode, accuracy, c, misses, mods, beatmap.lazerOnly || isLazer)

        return MapStatisticsParam(beatmapset, beatmap, expected)
    }

    enum class MapFilter(@param:Language("RegExp") val regex: Regex) {
        ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|ac?c?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)[%％]?".toRegex()),
        COMBO("(combo|连击|cb?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL[xX]?)".toRegex()),
        MISS("(m(is)?s|[msx0]|不可|红|失误|漏击)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),
    }

    private fun parseAccuracyAndCombo(input: String?, before: String? = null): Triple<Double, Double, Int> {
        val conditions = DataUtil.getConditions(input, MapFilter.entries.map { it.regex })

        var accuracy: Double? = null
        var combo: Double? = null
        var misses: Int? = null

        if (conditions.isNotEmpty()) {
            conditions.mapIndexed { index, cond ->
                val type = MapFilter.entries[index]
                val value = cond.firstOrNull()?.split(REG_OPERATOR_WITH_SPACE.toRegex())?.lastOrNull() ?: ""

                when(type) {
                    ACCURACY -> accuracy = when(val rd = value.toDoubleOrNull()) {
                        null -> null
                        in 0.0..1.0 -> rd
                        in 1.0..100.0 -> rd / 100.0
                        in 100.0..1000.0 -> rd / 1000.0
                        in 1000.0..10000.0 -> rd / 10000.0
                        else -> ((rd % 10) / 10).coerceIn(0.0, 1.0)
                    }
                    COMBO -> combo = value.toDoubleOrNull()
                    MISS -> misses = value.toIntOrNull()
                }
            }
        }

        val reconstruct = if (!before.isNullOrBlank() && (before.toIntOrNull() ?: 100) < 100) {
            before + " " + (input ?: "")
        } else {
            input ?: ""
        }

        val kvRegex = "\\w+${REG_EQUAL}\\S+".toRegex()

        val remain = reconstruct.replace(kvRegex, "")
            .trim()
            .split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        remain.forEach { r ->
            if (accuracy == null) {
                when(val rd = r.toDoubleOrNull()) {
                    null -> {}
                    in 0.0..1.0 -> accuracy = rd
                    in 1.0..100.0 -> accuracy = rd / 100.0
                    in 100.0..1000.0 -> accuracy = rd / 1000.0
                    in 1000.0..10000.0 -> accuracy = rd / 10000.0
                    else -> accuracy = ((rd % 10) / 10).coerceIn(0.0, 1.0)
                }

                return@forEach
            }

            if (combo == null) {
                val rd = r.toDoubleOrNull()

                combo = when (rd) {
                    null -> combo
                    in 0.0..1.0 -> rd
                    else -> rd
                }

                return@forEach
            }

            if (misses == null) {
                val rd = r.toIntOrNull()

                misses = rd
                return@forEach
            }
        }

        return Triple(accuracy ?: 1.0, combo ?: 1.0, misses ?: 0)
    }

    fun MapStatisticsParam.getPanelRParam(): PanelRParam {
        val ppList = getPPList(beatmap, expected, calculateApiService)

        beatmapset.beatmaps = beatmapset.beatmaps?.let { fullList ->
            val sortedList = fullList.sortedWith(
                compareBy<Beatmap> { it.mode.modeValue }.thenBy { it.starRating }
            )

            val centerIndex = sortedList.indexOfFirst { it.beatmapID == beatmap.beatmapID }

            if (centerIndex != -1) {
                var start = (centerIndex - 4).coerceAtLeast(0)

                // 2. 敲定终点：从起点往后取8个跨度（保证总数为9），最大不能超过列表尾部
                val end = (start + 8).coerceAtMost(sortedList.lastIndex)

                // 3. 最终修正起点：从终点往前倒推8个跨度（处理“后面不足向前面借”的情况）
                start = (end - 8).coerceAtLeast(0)

                val views = sortedList.subList(start, end + 1)

                views.forEach { view ->
                    BeatmapUtil.applyBeatmapChanges(view, expected.mods)
                }

                AsyncMethodExecutor.awaitList(
                    views.map { v ->
                        Callable { calculateApiService.applyStarToBeatmap(v,
                            OsuMode.getConvertableMode(expected.mode, v.mode), expected.mods
                        ) }
                    }
                )

                // 插入标签 beatmapApiService.extendBeatmapTag(view)
            }

            sortedList
        }

        // BeatmapUtil.applyBeatmapChanges(beatmap, expected.mods)

        return PanelRParam(beatmapset, beatmap, expected, ppList, beatmap.originalDetails?.toMap() ?: emptyMap())
    }

    data class MapStatisticsParam(val beatmapset: Beatmapset, val beatmap: Beatmap, val expected: Expected)

    data class PanelRParam(
        val beatmapset: Beatmapset,
        val beatmap: Beatmap,
        val expected: Expected,

        @field:JsonProperty("pp_list")
        val ppList: List<Double>,

        val original: Map<String, Any>,
    )

    data class Expected(
        val mode: OsuMode,
        val accuracy: Double,
        val combo: Int,
        val misses: Int,
        val mods: List<LazerMod>,

        @field:JsonProperty("is_lazer")
        val isLazer: Boolean = false,

        @field:JsonProperty("clock_rate")
        val clockRate: Double? = null,
    )

    companion object {
        // 等于绘图模块的 calcMap
        // 注意，0 是 if fc，1-6是 fc，7-12是 nc，acc 分别是100 99 98 96 94 92
        fun getPPList(
            beatmap: Beatmap,
            expected: Expected,
            calculateApiService: OsuCalculateApiService
        ): List<Double> {
            val result = mutableListOf<Double>()
            val accArray: DoubleArray = doubleArrayOf(1.0, 0.99, 0.98, 0.96, 0.94, 0.92)

            val combo = expected.combo
            val mode = expected.mode
            val mods = expected.mods
            val isLazer = expected.isLazer

            result.add(
                calculateApiService.getAccPP(
                    beatmapID = beatmap.beatmapID,
                    mode = mode,
                    mods = mods,
                    combo = combo,
                    misses = null,
                    isLazer = isLazer,
                    accuracy = expected.accuracy,
                    clockRate = expected.clockRate,
                )
            )

            val fcPP = calculateApiService.getAccPPList(
                beatmapID = beatmap.beatmapID,
                mode = mode,
                mods = mods,
                combo = combo,
                misses = null,
                isLazer = isLazer,
                accuracy = accArray,
                clockRate = expected.clockRate,
            )
            result.addAll(fcPP)
            if (expected.misses > 0) {
                result.addAll(
                    calculateApiService.getAccPPList(
                        beatmapID = beatmap.beatmapID,
                        mode = mode,
                        mods = mods,
                        combo = combo,
                        misses = expected.misses,
                        isLazer = isLazer,
                        accuracy = accArray,
                    )
                )
            } else {
                result.addAll(fcPP)
            }
            return result
        }
    }
}
