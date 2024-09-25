package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMod
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
import java.util.regex.Matcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service("MAP_MINUS")
class MapMinusService(
        private val beatmapApiService: OsuBeatmapApiService,
        private val imageService: ImageService,
) : MessageService<Matcher> {

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<Matcher>
    ): Boolean {
        val m = Instruction.MAP_MINUS.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val bid: Long
        var rate = 1.0
        val mode: OsuMode
        val fileStr: String
        val beatMap: BeatMap

        val modsList: List<OsuMod> = OsuMod.getModsList(matcher.group(FLAG_MOD))
        val isChangedRating = OsuMod.hasChangeRating(modsList)

        try {
            bid = matcher.group("bid").toLong()
        } catch (e: NumberFormatException) {
            throw MapMinusException(MapMinusException.Type.MM_Bid_Error)
        }

        if (StringUtils.hasText(matcher.group("rate"))) {
            try {
                rate = matcher.group("rate").toDouble()
            } catch (e: NumberFormatException) {
                throw MapMinusException(MapMinusException.Type.MM_Rate_Error)
            }
        }

        if (rate < 0.1) throw MapMinusException(MapMinusException.Type.MM_Rate_TooSmall)
        if (rate > 5.0) throw MapMinusException(MapMinusException.Type.MM_Rate_TooLarge)

        try {
            beatMap = beatmapApiService.getBeatMapFromDataBase(bid)
            mode = OsuMode.getMode(beatMap.modeInt)
            val mods = OsuMod.getModsValue(modsList)

            beatmapApiService.applySRAndPP(beatMap, beatMap.osuMode, mods)
            fileStr = beatmapApiService.getBeatMapFileString(bid)
        } catch (e: Exception) {
            throw MapMinusException(MapMinusException.Type.MM_Map_NotFound)
        }

        val file: OsuFile
        try {
            when (mode) {
                OsuMode.MANIA -> file = OsuFile.getInstance(fileStr)
                else -> throw MapMinusException(MapMinusException.Type.MM_Function_NotSupported)
            }
        } catch (e: NullPointerException) {
            throw MapMinusException(MapMinusException.Type.MM_Map_FetchFailed)
        }

        val mapMinus =
                PPMinus3.getInstance(
                        file, if (isChangedRating) OsuMod.getModsClockRate(modsList) else rate)

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
