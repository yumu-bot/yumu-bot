package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
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
        val inputMode = OsuMode.getMode(matcher.group(FLAG_MODE))

        val mode = OsuMode.getConvertableMode(inputMode, beatmap.mode)
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

        return PanelRParam(beatmapset, beatmap, expected, ppList, beatmap.originalDetails.toMap())
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
            val accArray: DoubleArray = doubleArrayOf(100.0, 99.0, 98.0, 96.0, 94.0, 92.0)

            val maxCombo = beatmap.maxCombo ?: expected.combo
            val mode = expected.mode
            val mods = expected.mods.ifEmpty { null }
            val isLazer = expected.isLazer

            result.add(
                calculateApiService.getAccPP(
                    beatmapID = beatmap.beatmapID,
                    mode = mode,
                    mods = mods,
                    maxCombo = maxCombo,
                    misses = null,
                    isLazer = isLazer,
                    accuracy = expected.accuracy * 100,
                ).pp
            )


            val fcPP = calculateApiService.getAccPPList(
                beatmapID = beatmap.beatmapID,
                mode = mode,
                mods = mods,
                maxCombo = maxCombo,
                misses = null,
                isLazer = isLazer,
                accuracy = accArray,
            )
            result.addAll(fcPP)
            if (expected.misses > 0) {
                result.addAll(
                    calculateApiService.getAccPPList(
                        beatmapID = beatmap.beatmapID,
                        mode = mode,
                        mods = mods,
                        maxCombo = maxCombo,
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

    /*


    data class MapParam(val user: OsuUser?, val beatmap: Beatmap, val expected: Expected)

    data class Expected(
        val mode: OsuMode,
        val accuracy: Double,
        val combo: Int,
        val misses: Int,
        val mods: List<LazerMod>,
        val isLazer: Boolean = false,
    )

    data class PanelE6Param(
        val user: OsuUser?,
        val beatmap: Beatmap,
        val density: IntArray,
        val original: Map<String, Any>,
        val attributes: RosuPerformance,
        val pp: List<Double>,
        val expected: Expected,
    ) {
        fun toMap(): Map<String, Any> = mutableMapOf(
            "beatmap" to beatmap,
            "density" to density,
            "original" to original,
            "attributes" to attributes,
            "pp" to pp,
            "expected" to expected
        ).apply {
            user?.let { put("user", it) }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MapParam>,
    ): Boolean {
        val matcher = Instruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: MapParam): ServiceCallStatistic? {
        val image = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw IllegalStateException.Send("谱面信息")
        }

        return ServiceCallStatistic.build(event,
            beatmapID = param.beatmap.beatmapID,
            beatmapsetID = param.beatmap.beatmapsetID,
            mode = param.expected.mode
            )
    }

    override fun accept(event: MessageEvent, messageText: String): MapParam? {
        val matcher = OfficialInstruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return null
        }

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: MapParam): MessageChain? {
        val image = param.getImage()

        try {
            return MessageChain(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw IllegalStateException.Send("谱面信息")
        }
    }

    enum class MapFilter(@param:Language("RegExp") val regex: Regex) {
        ACCURACY("$REG_NUMBER_DECIMAL[a%％]|[a%％]$REG_NUMBER_DECIMAL".toRegex()),
        COMBO("$REG_NUMBER_DECIMAL[cx×]|[cx×]$REG_NUMBER_DECIMAL".toRegex()),
        MISS("$REG_NUMBER_MORE[\\-m]|[\\-m]$REG_NUMBER_MORE".toRegex()),
        ANY(REG_NUMBER_DECIMAL.toRegex()),
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isLazer: Boolean = false): MapParam {
        val conditions = DataUtil.getConditions(matcher.group(FLAG_ANY), MapFilter.entries.map { it.regex })

        val id = getBid(matcher)

        val bid: Long

        if (id == 0L) {
            bid = dao.getLastBeatmapID(
                groupID = event.subject.contactID,
                name = null,
                from = LocalDateTime.now().minusHours(24L)
            ) ?: 0L
        } else {
            bid = id
        }

        val beatmap: Beatmap? = if (bid > 0L) {
            try {
                beatmapApiService.getBeatmap(bid)
            } catch (_: Throwable) {
                null
            }
        } else null

        if (beatmap == null) {
            /*
            if (isAvoidance(messageText, "！m", "!m")) {
                log.debug("指令退避：M 退避成功")
            }
            return null

             */
            throw IllegalArgumentException.WrongException.BeatmapID()
        }

        val mode = OsuMode.getMode(matcher.group("mode"), beatmap.mode)

        val user: OsuUser? = try {
            if (beatmap.mapperIDs.isNotEmpty()) {
                val user = userApiService.getOsuUser(beatmap.mapperIDs.first(), mode)

                if (beatmap.mapperIDs.size > 1) {
                    user.apply { username = "Multiple Mappers" }
                } else {
                    user
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        var accuracyStr = ""
        var comboStr = ""
        var missStr = ""

        conditions.forEachIndexed { index, condition ->
            if (condition.isNotEmpty()) {
                val c = condition.first()

                when (index) {
                    0 -> accuracyStr = c.replace("[a%]".toRegex(), "")
                    1 -> comboStr = c.replace("[cx]".toRegex(), "")
                    2 -> missStr = c.replace("[\\-m]".toRegex(), "")
                    3 -> for (cc in condition) {
                        if (accuracyStr.isBlank()) {
                            accuracyStr = cc
                            continue
                        }

                        if (comboStr.isBlank()) {
                            comboStr = cc
                            continue
                        }

                        if (missStr.isBlank()) {
                            missStr = cc
                            continue
                        }
                    }
                }
            }
        }

        val accuracy = run {
            val acc = accuracyStr.toDoubleOrNull() ?: 1.0

            return@run when(acc) {
                in 0.0..1.0 -> acc
                in 1.0..100.0 -> acc / 100.0
                in 100.0..10000.0 -> acc / 10000.0
                else -> {
                    throw IllegalArgumentException.WrongException.Accuracy()
                }
            }
        }

        val combo = if ((comboStr.toDoubleOrNull() ?: -1.0) in (0.0 - 1e-6)..1.0) {
            val rate = comboStr.toDouble()
            round(beatmap.maxCombo!! * rate).toInt()
        } else {
            val max = beatmap.maxCombo ?: Int.MAX_VALUE
            val maxCombo = beatmap.maxCombo ?: 0

            min(max(comboStr.toIntOrNull() ?: maxCombo, 0), max)
        }

        val miss = missStr.toIntOrNull() ?: 0

        val mods = LazerMod.getModsList(matcher.group("mod"))

        val expected = Expected(OsuMode.getConvertableMode(mode, beatmap.mode), accuracy, combo, miss, mods, isLazer)
        return MapParam(user, beatmap, expected)
    }

    private fun MapParam.getImage(): ByteArray {
        return getPanelE6Image(user, beatmap, expected, beatmapApiService, calculateApiService, imageService)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapStatisticsService::class.java)

        fun getPanelE6Image(
            user: OsuUser?,
            beatmap: Beatmap,
            expected: Expected,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
            imageService: ImageService,
        ): ByteArray {

            val original = mutableMapOf<String, Any>()

            with(beatmap) {
                original["cs"] = cs!!
                original["ar"] = ar!!
                original["od"] = od!!
                original["hp"] = hp!!
                original["bpm"] = bpm
                original["drain"] = hitLength!!
                original["total"] = totalLength
            }


            val pp = getPPList(beatmap, expected, calculateApiService)
            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)

            val attributes = calculateApiService.getAccPP(
                beatmap.beatmapID,
                expected.mode,
                expected.mods,
                expected.combo,
                expected.misses,
                expected.isLazer,
                expected.accuracy * 100,
            )

            beatmap.starRating = attributes.stars ?: beatmap.starRating

            BeatmapUtil.applyBeatmapChanges(beatmap, expected.mods)

            return imageService.getPanel(
                PanelE6Param(user, beatmap, density, original, attributes, pp, expected)
                    .toMap(),
                "E6",
            )
        }


    }

     */
}
