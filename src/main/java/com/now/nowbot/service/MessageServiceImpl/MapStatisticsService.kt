package com.now.nowbot.service.MessageServiceImpl
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Statistics
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
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.BindException
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.isAvoidance
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rosu.Rosu
import rosu.parameter.JniScore
import rosu.result.JniResult
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

@Service("MAP")
class MapStatisticsService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao
) : MessageService<MapParam>, TencentMessageService<MapParam> {

    @JvmRecord 
    data class MapParam (
        @JvmField val user: OsuUser?,
        @JvmField val beatmap: BeatMap,
        @JvmField val expected: Expected
    )
    
    @JvmRecord 
    data class Expected (
        @JvmField val mode : OsuMode,
        @JvmField val accuracy : Double,
        @JvmField val combo : Int,
        @JvmField val misses : Int,
        @JvmField val mods : List<String>
    )

    @JvmRecord
    data class PanelE6Param(
        @JvmField val user: OsuUser?,
        @JvmField val beatmap: BeatMap,
        @JvmField val density: IntArray,
        @JvmField val original: Map<String, Any>,
        @JvmField val attributes: JniResult,
        @JvmField val pp: List<Double>,
        @JvmField val expected: Expected
    )

    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<MapParam>): Boolean {
        val matcher = Instruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val bid = getBid(matcher)
        var beatMap: BeatMap? = null

        if (bid != 0L) {
            beatMap = beatmapApiService.getBeatMapInfo(bid)
        }
        
        if (beatMap == null) {
            if (isAvoidance(messageText, "！m", "!m")) {
                log.debug(String.format("指令退避：M 退避成功，被退避的玩家：%s", event.sender.name))
            }
            return false
        }

        var user: OsuUser?
        var mode = OsuMode.getMode(matcher.group("mode"))
        
        try {
            val bind = bindDao.getUserFromQQ(event.sender.id)
            user = userApiService.getPlayerInfo(bind, mode)
        } catch (e : BindException) {
            user = null
        }
        var combo: Int
        
        var accuracy = try {
            matcher.group("accuracy").toDouble()
        } catch (e : RuntimeException) {
            1.0
        }
        
        try  {
            combo = matcher.group("combo").toInt()
        } catch (e : RuntimeException) {
            try {
                val rate = matcher.group("combo").toDouble()
                combo = if (rate >= 0.0 && rate <= 1.0) {
                    Math.toIntExact(Math.round(
                    Objects.requireNonNullElse(beatMap.maxCombo, 0) * rate))
                } else {
                    0
                }
            } catch (e1 : RuntimeException) {
                combo = 0
            }
        }
        
        var miss = try  {
            matcher.group("miss").toInt()
        } catch (e : RuntimeException) {
            0
        }
        
        var mods = try  {
            OsuMod.getModsAbbrList(matcher.group("mod")).stream().distinct().toList()
        } catch (e : RuntimeException) {
            ArrayList()
        }

        // 标准化 acc 和 combo
        val maxCombo = beatMap.maxCombo
        
        if (maxCombo != null) {
            combo = if (combo <= 0) {
                maxCombo
            } else {
                min(combo.toDouble(), maxCombo.toDouble()).toInt()
            }
        }
        
        if (combo < 0) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamCombo)
        }
        if (accuracy > 1.0 && accuracy <= 100.0) {
            accuracy /= 100.0
        } else if (accuracy > 100.0 && accuracy <= 10000.0) {
            accuracy /= 10000.0
        } else if (accuracy <= 0.0 || accuracy > 10000.0) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamAccuracy)
        }

        //只有转谱才能赋予游戏模式
        val beatMapMode = beatMap.osuMode
        
        if (beatMapMode != OsuMode.OSU || OsuMode.isDefaultOrNull(mode)) {
            mode = beatMapMode
        }
        
        val expected = Expected(mode, accuracy, combo, miss, mods)
        
        data.value = MapParam(user, beatMap, expected)
        return true
    }
    
    @Throws(Throwable::class)
    override fun HandleMessage(event : MessageEvent, param : MapParam) {
        val from = event.subject
        val image: ByteArray
        
        try {
            // image = imageService.getPanelE2(param.user, param.beatMap, param.expected)
            image = imageService.getPanelE6(
                getExpectedScore4PanelE6(param.user, param.beatmap, param.expected, beatmapApiService)
            )
        } catch (e : Exception) {
            log.error("谱面信息：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "谱面信息")
        }
        
        try {
            from?.sendImage(image)
        } catch (e : Exception) {
            log.error("谱面信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MapParam? {
        val matcher = Instruction.MAP.matcher(messageText)
        if (!matcher.find()) {
            return null
        }

        val bid = getBid(matcher)
        var beatMap: BeatMap? = null

        if (bid != 0L) {
            beatMap = beatmapApiService.getBeatMapInfo(bid)
        }

        if (beatMap == null) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
        }

        var user: OsuUser?
        var mode = OsuMode.getMode(matcher.group("mode"))

        try {
            val bind = bindDao.getUserFromQQ(event.sender.id)
            user = userApiService.getPlayerInfo(bind, mode)
        } catch (e : BindException) {
            user = null
        }
        var combo: Int

        var accuracy = try {
            matcher.group("accuracy").toDouble()
        } catch (e : RuntimeException) {
            1.0
        }

        try {
            combo = matcher.group("combo").toInt()
        } catch (e : RuntimeException) {
            try {
                val rate = matcher.group("combo").toDouble()
                combo = if (rate >= 0.0 && rate <= 1.0) {
                    Math.toIntExact(Math.round(
                    Objects.requireNonNullElse(beatMap.maxCombo, 0) * rate))
                } else {
                    0
                }
            } catch (e1 : RuntimeException) {
                combo = 0
            }
        }

        var miss = try  {
            matcher.group("miss").toInt()
        } catch (e : RuntimeException) {
            0
        }

        var mods = try  {
            OsuMod.getModsAbbrList(matcher.group("mod")).stream().distinct().toList()
        } catch (e : RuntimeException) {
            ArrayList()
        }

        // 标准化 acc 和 combo
        val maxCombo = beatMap.maxCombo

        if (maxCombo != null) {
            combo = if (combo <= 0) {
                maxCombo
            } else {
                min(combo.toDouble(), maxCombo.toDouble()).toInt()
            }
        }

        if (combo < 0) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamCombo)
        }
        if (accuracy > 1.0 && accuracy <= 100.0) {
            accuracy /= 100.0
        } else if (accuracy > 100.0 && accuracy <= 10000.0) {
            accuracy /= 10000.0
        } else if (accuracy <= 0.0 || accuracy > 10000.0) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamAccuracy)
        }

        //只有转谱才能赋予游戏模式
        val beatMapMode = beatMap.osuMode

        if (beatMapMode != OsuMode.OSU || OsuMode.isDefaultOrNull(mode)) {
            mode = beatMapMode
        }

        val expected = Expected(mode, accuracy, combo, miss, mods)

        return MapParam(user, beatMap, expected)
    }

    override fun reply(event: MessageEvent, param: MapParam): MessageChain? {
        val image: ByteArray

        try {
            // image = imageService.getPanelE2(param.user, param.beatMap, param.expected)
            image = imageService.getPanelE6(
                getExpectedScore4PanelE6(param.user, param.beatmap, param.expected, beatmapApiService)
            )
        } catch (e : Exception) {
            log.error("谱面信息：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "谱面信息")
        }

        try {
            return QQMsgUtil.getImage(image)
        } catch (e : Exception) {
            log.error("谱面信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息")
        }
    }

    companion object {
        private val log : Logger = LoggerFactory.getLogger(MapStatisticsService::class.java)

        @JvmStatic
        @Throws(Exception::class)
        fun getExpectedScore4PanelE6(user: OsuUser?, beatmap: BeatMap, expected: Expected, beatmapApiService: OsuBeatmapApiService): PanelE6Param {

            val original = HashMap<String, Any>(6)
            original["cs"] = beatmap.cs
            original["ar"] = beatmap.ar
            original["od"] = beatmap.od
            original["hp"] = beatmap.hp
            original["bpm"] = beatmap.bpm
            original["drain"] = beatmap.hitLength
            original["total"] = beatmap.totalLength

            beatmapApiService.applySRAndPP(beatmap, expected)

            val pp = getPPList(beatmap, expected, beatmapApiService);
            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val attributes = beatmapApiService.getPP(beatmap, expected)

            return PanelE6Param(user, beatmap, density, original, attributes, pp, expected)
        }

        // 等于绘图模块的 calcMap
        // 注意，0 是 iffc，1-6是fc，7-12是nc，acc分别是100 99 98 96 94 92
        @JvmStatic
        @Throws(Exception::class)
        fun getPPList(beatmap: BeatMap, expected: Expected, beatmapApiService: OsuBeatmapApiService) : List<Double> {
            val result = mutableListOf<Double>()
            val accArray : DoubleArray = doubleArrayOf(1.0, 0.99, 0.98, 0.96, 0.94, 0.92)
            val file = beatmapApiService.getBeatMapFile(beatmap.beatMapID).toByteArray(Charsets.UTF_8);

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

            // 解决了
            for (i in 0..5) {
                scoreNC.accuracy = accArray[i]
                result.add(Rosu.calculate(file, scoreNC).pp)
            }

            return result
        }
    }
}

