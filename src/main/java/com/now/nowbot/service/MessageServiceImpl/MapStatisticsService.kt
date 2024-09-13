package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.MapStatisticsService.MapParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.BindException
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.isAvoidance
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import java.util.*
import java.util.regex.Matcher
import kotlin.math.min
import kotlin.math.round
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rosu.Rosu
import rosu.parameter.JniScore
import rosu.result.JniResult

@Service("MAP")
class MapStatisticsService(
        private val imageService: ImageService,
        private val beatmapApiService: OsuBeatmapApiService,
        private val userApiService: OsuUserApiService,
        private val bindDao: BindDao
) : MessageService<MapParam>, TencentMessageService<MapParam> {

    data class MapParam(val user: OsuUser?, val beatmap: BeatMap, val expected: Expected)

    data class Expected(
            @JvmField val mode: OsuMode,
            @JvmField val accuracy: Double,
            @JvmField val combo: Int,
            @JvmField val misses: Int,
            @JvmField val mods: List<String>
    )

    data class PanelE6Param(
            val user: OsuUser?,
            val beatmap: BeatMap,
            val density: IntArray,
            val original: Map<String, Any>,
            val attributes: JniResult,
            val pp: List<Double>,
            val expected: Expected
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
            data: DataValue<MapParam>
    ): Boolean {
        val matcher = Instruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value =
                getParam(matcher, event.sender.id, messageText) ?: return false
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MapParam) {
        val from = event.subject
        val image =
                try {
                    param.getImage()
                } catch (e: Exception) {
                    log.error("谱面信息：渲染失败", e)
                    throw GeneralTipsException(
                            GeneralTipsException.Type.G_Malfunction_Render, "谱面信息")
                }

        try {
            from?.sendImage(image)
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

        return getParam(matcher, event.sender.id, messageText)
    }

    override fun reply(event: MessageEvent, param: MapParam): MessageChain? {
        val image =
                try {
                    param.getImage()
                } catch (e: Exception) {
                    log.error("谱面信息：渲染失败", e)
                    throw GeneralTipsException(
                            GeneralTipsException.Type.G_Malfunction_Render, "谱面信息")
                }

        try {
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("谱面信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息")
        }
    }

    private fun getParam(
            matcher: Matcher,
            userID: Long,
            messageText: String,
    ): MapParam? {
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
        var user: OsuUser?

        try {
            val bind = bindDao.getUserFromQQ(userID)
            user = userApiService.getPlayerInfo(bind, mode)
        } catch (e: BindException) {
            user = null
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
                            round(beatMap.maxCombo * rate).toInt()
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
                    OsuMod.getModsAbbrList(matcher.group("mod")).distinct()
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
        val beatMapMode = beatMap.osuMode

        if (beatMapMode != OsuMode.OSU || OsuMode.isDefaultOrNull(mode)) {
            mode = beatMapMode
        }

        val expected = Expected(mode, accuracy, combo, miss, mods)
        return MapParam(user, beatMap, expected)
    }

    private fun MapParam.getImage(): ByteArray {
        return getPanelE6Image(user, beatmap, expected, beatmapApiService, imageService)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapStatisticsService::class.java)

        @JvmStatic
        fun getPanelE6Image(
                user: OsuUser?,
                beatmap: BeatMap,
                expected: Expected,
                beatmapApiService: OsuBeatmapApiService,
                imageService: ImageService,
        ): ByteArray {

            val original = mutableMapOf<String, Any>()

            with(beatmap) {
                original["cs"] = cs
                original["ar"] = ar
                original["od"] = od
                original["hp"] = hp
                original["bpm"] = bpm
                original["drain"] = hitLength
                original["total"] = totalLength
            }

            beatmapApiService.applySRAndPP(beatmap, expected)

            val pp = getPPList(beatmap, expected, beatmapApiService)
            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val attributes = beatmapApiService.getPP(beatmap, expected)

            return imageService.getPanelE6(
                    PanelE6Param(user, beatmap, density, original, attributes, pp, expected))
        }

        // 等于绘图模块的 calcMap
        // 注意，0 是 iffc，1-6是fc，7-12是nc，acc分别是100 99 98 96 94 92
        private fun getPPList(
                beatmap: BeatMap,
                expected: Expected,
                beatmapApiService: OsuBeatmapApiService
        ): List<Double> {
            val result = mutableListOf<Double>()
            val accArray: DoubleArray = doubleArrayOf(1.0, 0.99, 0.98, 0.96, 0.94, 0.92)
            val file =
                    beatmapApiService.getBeatMapFileStr(beatmap.beatMapID).toByteArray(Charsets.UTF_8)

            var scoreFC = JniScore()
            scoreFC.mode = expected.mode.toRosuMode()
            scoreFC.mods = OsuMod.getModsValueFromAbbrList(expected.mods)
            scoreFC.accuracy = expected.accuracy
            scoreFC.combo = beatmap.maxCombo
            scoreFC.misses = 0

            result.add(Rosu.calculate(file, scoreFC).pp)

            for (i in 0..5) {
                scoreFC.accuracy = accArray[i]
                result.add(Rosu.calculate(file, scoreFC).pp)
            }

            val scoreNC = JniScore()
            scoreNC.mode = expected.mode.toRosuMode()
            scoreNC.mods = OsuMod.getModsValueFromAbbrList(expected.mods)
            scoreNC.accuracy = expected.accuracy
            scoreNC.combo = expected.combo
            scoreNC.misses = expected.misses

            for (i in 0..5) {
                scoreNC.accuracy = accArray[i]
                result.add(Rosu.calculate(file, scoreNC).pp)
            }

            return result
        }
    }
}
