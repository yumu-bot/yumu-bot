package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService.MapParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.isAvoidance
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Service("MAP")
class MapStatisticsService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<MapParam>, TencentMessageService<MapParam> {

    data class MapParam(val user: OsuUser?, val beatmap: Beatmap, val expected: Expected)

    data class Expected(
        @JvmField val mode: OsuMode,
        @JvmField val accuracy: Double,
        @JvmField val combo: Int,
        @JvmField val misses: Int,
        @JvmField val mods: List<LazerMod>,
        @JvmField val isLazer: Boolean = false,
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
        fun toMap(): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            user?.let { map["user"] = it }
            map["beatmap"] = beatmap
            map["density"] = density
            map["original"] = original
            map["attributes"] = attributes
            map["pp"] = pp
            map["expected"] = expected
            return map
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

        data.value = getParam(matcher, messageText) ?: return false
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MapParam) {
        val image = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw IllegalStateException.Send("谱面信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MapParam? {
        val matcher = OfficialInstruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return null
        }

        return getParam(matcher, messageText)
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

    enum class Filter(@Language("RegExp") val regex: Regex) {
        ACCURACY("$REG_NUMBER_DECIMAL[a%％]|[a%％]$REG_NUMBER_DECIMAL".toRegex()),
        COMBO("$REG_NUMBER_DECIMAL[cx×]|[cx×]$REG_NUMBER_DECIMAL".toRegex()),
        MISS("$REG_NUMBER_MORE[\\-m]|[\\-m]$REG_NUMBER_MORE".toRegex()),
        ANY(REG_NUMBER_DECIMAL.toRegex()),
    }

    private fun getParam(matcher: Matcher, messageText: String): MapParam? {
        val bid = getBid(matcher)
        val conditions = DataUtil.paramMatcher(matcher.group("any"), Filter.entries.map { it.regex })

        val beatmap: Beatmap? = if (bid != 0L) {
            try {
                beatmapApiService.getBeatmap(bid)
            } catch (ignored: Throwable) {
                null
            }
        } else {
            null
        }

        if (beatmap == null) {
            if (isAvoidance(messageText, "！m", "!m")) {
                log.debug("指令退避：M 退避成功")
            }
            return null
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
        } catch (e: Exception) {
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

        val expected = Expected(OsuMode.getConvertableMode(mode, beatmap.mode), accuracy, combo, miss, mods)
        return MapParam(user, beatmap, expected)
    }

    private fun MapParam.getImage(): ByteArray {
        return getPanelE6Image(user, beatmap, expected, beatmapApiService, calculateApiService, imageService)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapStatisticsService::class.java)

        @JvmStatic
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
                original["cs"] = CS!!
                original["ar"] = AR!!
                original["od"] = OD!!
                original["hp"] = HP!!
                original["bpm"] = BPM!!
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

            calculateApiService.applyBeatMapChanges(beatmap, expected.mods)

            return imageService.getPanel(
                PanelE6Param(user, beatmap, density, original, attributes, pp, expected)
                    .toMap(),
                "E6",
            )
        }

        // 等于绘图模块的 calcMap
        // 注意，0 是 if fc，1-6是 fc，7-12是 nc，acc 分别是100 99 98 96 94 92
        private fun getPPList(
            beatmap: Beatmap,
            expected: Expected,
            calculateApiService: OsuCalculateApiService,
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
}
