package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.PPPlus
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.PerformancePlusService
import com.now.nowbot.throwable.ServiceException.PPPlusException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import org.springframework.stereotype.Service

@Service("PP_PLUS_MAP")
class PPPlusMapService(
    private val performancePlusService: PerformancePlusService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<PPPlusMapService.Param> {

    data class Param(
        val bid: Long,
        val mods: Int,
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Param?>): Boolean {
        val matcher = Instruction.PP_PLUS_MAP.matcher(messageText!!)
        if (!matcher.find()) return false

        val bidStr = matcher.group(FLAG_BID)
        if (bidStr.isNullOrBlank()) {
            return false
        }
        val bid = bidStr.toLong()

        val modsInt = matcher.group(FLAG_MOD).let {
            if (it.isNullOrBlank()) 0 else OsuMod.getModsValue(it)
        }
        data.value = Param(bid, modsInt)
        return true
    }

    override fun HandleMessage(event: MessageEvent, data: Param) {
        val map = try {
            beatmapApiService.getBeatMapInfoFromDataBase(data.bid)
        } catch (e: Exception) {
            throw PPPlusException(PPPlusException.Type.PL_Map_NotFound)
        }
        // 不支持其他模式
        if (OsuMode.getMode(map.getMode()) != OsuMode.OSU) {
            throw PPPlusException(PPPlusException.Type.PL_Function_NotSupported)
        }
        val pp = try {
            performancePlusService.getMapPerformancePlus(data.bid, data.mods)
        } catch (e: Exception) {
            throw PPPlusException(PPPlusException.Type.PL_Fetch_APIConnectFailed)
        }

        map.addPPPlus(pp, data.mods)
        val dataMap = mutableMapOf<String, Any>()
        dataMap["isUser"] = false
        dataMap["me"] = map
        dataMap["my"] = pp
        val image = imageService.getPanelB3(dataMap)
        event.subject.sendImage(image)
    }


    private fun BeatMap.addPPPlus(pp: PPPlus, mods: Int) {
        starRating = pp.difficulty.total?.toFloat() ?: 0f
        if (mods != 0) {
            cs = DataUtil.CS(cs, mods)
            ar = DataUtil.AR(ar, mods)
            od = DataUtil.OD(od, mods)
        }
    }
}