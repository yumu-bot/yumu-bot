package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.json.RosuPerformance
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
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.isAvoidance
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.min
import kotlin.math.round

@Service("MAP")
class MapStatisticsService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<MapParam>, TencentMessageService<MapParam> {

    data class MapParam(val user: OsuUser?, val beatmap: BeatMap, val expected: Expected)

    data class Expected(
        @JvmField val mode: OsuMode,
        @JvmField val accuracy: Double,
        @JvmField val combo: Int,
        @JvmField val misses: Int,
        @JvmField val mods: List<String>,
        @JvmField val isLazer: Boolean = false,
    )

    data class PanelE6Param(
        val user: OsuUser?,
        val beatmap: BeatMap,
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
        val image =
            try {
                param.getImage()
            } catch (e: Exception) {
                log.error("谱面信息：渲染失败", e)
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Malfunction_Render,
                    "谱面信息",
                )
            }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息")
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
        val image =
            try {
                param.getImage()
            } catch (e: Exception) {
                log.error("谱面信息：渲染失败", e)
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Malfunction_Render,
                    "谱面信息",
                )
            }

        try {
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息")
        }
    }

    private fun getParam(matcher: Matcher, messageText: String): MapParam? {
        val bid = getBid(matcher)
        var beatMap: BeatMap? = null

        if (bid != 0L) {
            beatMap = beatmapApiService.getBeatMap(bid)
        }

        if (beatMap == null) {
            if (isAvoidance(messageText, "！m", "!m")) {
                log.debug("指令退避：M 退避成功")
            }
            return null
        }

        var mode = OsuMode.getMode(matcher.group("mode"))

        val user: OsuUser? = try {
            if (beatMap.mapperID > 0L) {
                userApiService.getPlayerInfo(beatMap.mapperID, mode)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        var combo: Int

        var accuracy =
            try {
                matcher.group("accuracy").toDouble()
            } catch (e: RuntimeException) {
                1.0
            }

        combo =
            try {
                matcher.group("combo").toInt()
            } catch (e: RuntimeException) {
                try {
                    val rate = matcher.group("combo").toDouble()
                    if (rate in 0.0..1.0) {
                        round(beatMap.maxCombo!! * rate).toInt()
                    } else {
                        0
                    }
                } catch (e1: RuntimeException) {
                    0
                }
            }

        val miss =
            try {
                matcher.group("miss").toInt()
            } catch (e: RuntimeException) {
                0
            }

        val mods =
            try {
                OsuMod.splitModAcronyms(matcher.group("mod")).distinct()
            } catch (e: RuntimeException) {
                emptyList()
            }

        // 标准化 acc 和 combo
        val maxCombo = beatMap.maxCombo

        if (maxCombo != null) {
            combo =
                if (combo <= 0) {
                    maxCombo
                } else {
                    min(combo.toDouble(), maxCombo.toDouble()).toInt()
                }
        }

        if (combo < 0) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamCombo)
        }
        accuracy =
            when {
                accuracy <= 1.0 -> accuracy
                accuracy <= 100.0 -> accuracy / 100.0
                accuracy <= 10000.0 -> accuracy / 10000.0
                else -> {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamAccuracy)
                }
            }

        // 只有转谱才能赋予游戏模式
        val beatMapMode = beatMap.mode

        if (beatMapMode != OsuMode.OSU || OsuMode.isDefaultOrNull(mode)) {
            mode = beatMapMode
        }

        val expected = Expected(mode, accuracy, combo, miss, mods)
        return MapParam(user, beatMap, expected)
    }

    private fun MapParam.getImage(): ByteArray {
        return getPanelE6Image(user, beatmap, expected, beatmapApiService, calculateApiService, imageService)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapStatisticsService::class.java)

        @JvmStatic
        fun getPanelE6Image(
            user: OsuUser?,
            beatmap: BeatMap,
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

            val mods = LazerMod.getModsList(expected.mods)

            val attributes = calculateApiService.getAccPP(
                beatmap.beatMapID,
                expected.mode,
                mods,
                expected.combo,
                expected.misses,
                expected.isLazer,
                expected.accuracy * 100,
            )

            beatmap.starRating = attributes.stars ?: beatmap.starRating

            calculateApiService.applyBeatMapChanges(beatmap, mods)

            return imageService.getPanel(
                PanelE6Param(user, beatmap, density, original, attributes, pp, expected)
                    .toMap(),
                "E6",
            )
        }

        // 等于绘图模块的 calcMap
        // 注意，0 是 if fc，1-6是 fc，7-12是 nc，acc 分别是100 99 98 96 94 92
        private fun getPPList(
            beatmap: BeatMap,
            expected: Expected,
            calculateApiService: OsuCalculateApiService,
        ): List<Double> {
            val result = mutableListOf<Double>()
            val accArray: DoubleArray = doubleArrayOf(100.0, 99.0, 98.0, 96.0, 94.0, 92.0)

            val maxCombo = beatmap.maxCombo ?: expected.combo
            val mode = expected.mode
            val mods = if (expected.mods.isEmpty()) {
                null
            } else {
                LazerMod.getModsList(expected.mods)
            }
            val isLazer = expected.isLazer

            result.add(
                calculateApiService.getAccPP(
                    beatmapID = beatmap.beatMapID,
                    mode = mode,
                    mods = mods,
                    maxCombo = maxCombo,
                    misses = null,
                    isLazer = isLazer,
                    accuracy = expected.accuracy * 100,
                ).pp
            )


            val fcPP = calculateApiService.getAccPPList(
                beatmapID = beatmap.beatMapID,
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
                        beatmapID = beatmap.beatMapID,
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
