package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.mapminus.PPMinus3
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.serviceException.MapMinusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service("MAP_MINUS")
class MapMinusService(
        private val beatmapApiService: OsuBeatmapApiService,
        private val imageService: ImageService,
) : MessageService<MapMinusService.MapMinusParam> {

    data class MapMinusParam(val bid: Long, val rate: Double = 1.0, val modsList: List<LazerMod>)

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<MapMinusParam>,
    ): Boolean {
        val m = Instruction.MAP_MINUS.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val modsList: List<LazerMod> = LazerMod.getModsList(m.group(FLAG_MOD))

        val bid =
                try {
                    m.group("bid").toLong()
                } catch (e: NumberFormatException) {
                    throw MapMinusException(MapMinusException.Type.MM_Bid_Error)
                }

        val rate =
                if (StringUtils.hasText(m.group("rate"))) {
                    try {
                        m.group("rate").toDouble()
                    } catch (e: NumberFormatException) {
                        throw MapMinusException(MapMinusException.Type.MM_Rate_Error)
                    }
                } else {
                    1.0
                }

        if (rate < 0.1) throw MapMinusException(MapMinusException.Type.MM_Rate_TooSmall)
        if (rate > 5.0) throw MapMinusException(MapMinusException.Type.MM_Rate_TooLarge)

        data.value = MapMinusParam(bid, rate, modsList)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MapMinusParam) {
        val fileStr: String
        val beatMap: BeatMap
        val mode: OsuMode

        val isChangedRating = LazerMod.hasStarRatingChange(param.modsList)

        try {

            beatMap = beatmapApiService.getBeatMapFromDataBase(param.bid)
            mode = OsuMode.getMode(beatMap.modeInt)

            beatmapApiService.applySRAndPP(beatMap, beatMap.mode, param.modsList)
            fileStr = beatmapApiService.getBeatMapFileString(param.bid)
        } catch (e: Exception) {
            throw MapMinusException(MapMinusException.Type.MM_Map_NotFound)
        }

        val file =
                try {
                    when (mode) {
                        OsuMode.MANIA -> OsuFile.getInstance(fileStr)
                        else ->
                                throw MapMinusException(
                                        MapMinusException.Type.MM_Function_NotSupported
                                )
                    }
                } catch (e: NullPointerException) {
                    throw MapMinusException(MapMinusException.Type.MM_Map_FetchFailed)
                }

        val mapMinus =
                PPMinus3.getInstance(
                        file,
                        if (isChangedRating) {
                            LazerMod.getModSpeedForStarCalculate(param.modsList)
                        } else {
                            param.rate
                        },
                )

        val image: ByteArray

        try {
            image = imageService.getPanelB2(beatMap, mapMinus)
        } catch (e: Exception) {
            log.error("谱面 Minus：渲染失败", e)
            throw MapMinusException(MapMinusException.Type.MM_Render_Error)
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面 Minus：发送失败", e)
            throw MapMinusException(MapMinusException.Type.MM_Send_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapMinusService::class.java)
    }
}
