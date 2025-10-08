package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.skill.SkillType
import com.now.nowbot.model.skill.Skill
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TestTypeService.MapTypeParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.botException.MapMinusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MOD
import org.springframework.stereotype.Service

@Service("TEST_TYPE") class TestTypeService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<MapTypeParam> {
    data class MapTypeParam(val bid: Long, val mode: OsuMode, val rate: Double = 1.0, val mods: List<LazerMod>)

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

        data.value = MapTypeParam(bid, OsuMode.MANIA, rate, modsList)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: MapTypeParam): ServiceCallStatistic? {
        val fileStr: String
        val map: Beatmap

        val isChangedRating = LazerMod.hasStarRatingChange(param.mods)

        try {
            map = beatmapApiService.getBeatmapFromDatabase(param.bid)
            fileStr = beatmapApiService.getBeatmapFileString(param.bid)!!
        } catch (e: Exception) {
            throw MapMinusException(MapMinusException.Type.MM_Map_NotFound)
        }

        if (map.mode.isNotConvertAble(param.mode)) {
            throw MapMinusException(MapMinusException.Type.MM_Function_NotSupported)
        }

        calculateApiService.applyStarToBeatMap(map, param.mode, param.mods)

        val file = try {
            OsuFile.getInstance(fileStr)
        } catch (e: NullPointerException) {
            throw MapMinusException(MapMinusException.Type.MM_Map_FetchFailed)
        }

        val mapMinus = Skill.getInstance(
            file,
            param.mode,
            if (isChangedRating) {
                LazerMod.getModSpeedForStarCalculate(param.mods).toDouble()
            } else {
                param.rate
            },
        )

        // =======================

        val result = SkillType.getType(mapMinus)
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

        return null
    }
}
