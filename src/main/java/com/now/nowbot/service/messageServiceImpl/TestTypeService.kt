package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.mapminus.PPMinus4Type
import com.now.nowbot.model.mapminus.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TestTypeService.MapTypeParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.serviceException.MapMinusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MOD
import org.springframework.stereotype.Service

@Service("TEST_TYPE") class TestTypeService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<MapTypeParam> {
    data class MapTypeParam(val bid: Long, val rate: Double = 1.0, val modsList: List<LazerMod>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MapTypeParam>,
    ): Boolean {
        val m = Instruction.TEST_TYPE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val modsList: List<LazerMod> = LazerMod.getModsList(m.group(FLAG_MOD))

        val bid = try {
            m.group("bid").toLong()
        } catch (e: NumberFormatException) {
            throw MapMinusException(MapMinusException.Type.MM_Bid_Error)
        }

        val rate = if (m.group("rate").isNullOrBlank().not()) {
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

        data.value = MapTypeParam(bid, rate, modsList)
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: MapTypeParam) {
        val fileStr: String
        val beatMap: BeatMap
        val mode: OsuMode

        val isChangedRating = LazerMod.hasStarRatingChange(param.modsList)

        try {

            beatMap = beatmapApiService.getBeatMapFromDataBase(param.bid)
            mode = OsuMode.getMode(beatMap.modeInt)

            if (isChangedRating) {
                val star = calculateApiService.getStar(param.bid, beatMap.mode, param.modsList)
                beatMap.starRating = star
            }
            fileStr = beatmapApiService.getBeatMapFileString(param.bid)!!
        } catch (e: Exception) {
            throw MapMinusException(MapMinusException.Type.MM_Map_NotFound)
        }

        val file = try {
            when (mode) {
                OsuMode.MANIA -> OsuFile.getInstance(fileStr)
                else -> throw MapMinusException(
                    MapMinusException.Type.MM_Function_NotSupported
                )
            }
        } catch (e: NullPointerException) {
            throw MapMinusException(MapMinusException.Type.MM_Map_FetchFailed)
        }

        val mapMinus = PPMinus4.getInstance(
            file,
            if (isChangedRating) {
                LazerMod.getModSpeedForStarCalculate(param.modsList).toDouble()
            } else {
                param.rate
            },
        )

        // =======================

        val result = PPMinus4Type.getType(mapMinus)
        val sb = StringBuilder("\n")

        var i = 1
        for (e in result) {
            sb.append("#${i}:").append(" ").append(String.format("%.2f", e.value * 100)).append("%").append(" ")
                .append("[${e.key.chinese}]").append("\n")

            i++

            if (i >= 6) break
        }

        sb.removeSuffix("\n")

        event.reply(
            MessageChain.MessageChainBuilder().addText("这张图可能是？？：\n").addText(sb.toString()).build()
        )
    }
}
