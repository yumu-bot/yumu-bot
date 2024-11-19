package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.PPPlus
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.PerformancePlusService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl
import com.now.nowbot.throwable.serviceException.PPPlusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import com.yumu.core.constants.log
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("PP_PLUS_MAP")
class PPPlusMapService(
    private val performancePlusService: PerformancePlusService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<PPPlusMapService.PPPlusParam> {

    data class PPPlusParam(
        val bid: Long,
        val mods: List<LazerMod>,
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<PPPlusParam?>): Boolean {
        val matcher = Instruction.PP_PLUS_MAP.matcher(messageText)
        if (!matcher.find()) return false

        val bidStr = matcher.group(FLAG_BID)
        if (bidStr.isNullOrBlank()) {
            return false
        }
        val bid = bidStr.toLong()

        val mods = LazerMod.getModsList(matcher.group(FLAG_MOD))
        data.value = PPPlusParam(bid, mods)
        return true
    }

    override fun HandleMessage(event: MessageEvent, data: PPPlusParam) {
        val map = try {
            beatmapApiService.getBeatMapFromDataBase(data.bid)
        } catch (e: Exception) {
            throw PPPlusException(PPPlusException.Type.PL_Map_NotFound)
        }
        // 不支持其他模式
        if (map.mode != OsuMode.OSU) {
            throw PPPlusException(PPPlusException.Type.PL_Function_NotSupported)
        }
        val pp = try {
            performancePlusService.getMapPerformancePlus(data.bid, data.mods)
        } catch (e: Exception) {
            if (e is WebClientResponseException) {
                log.error { e.responseBodyAsString }
            } else {
                log.error { e.message }
            }
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


    private fun BeatMap.addPPPlus(pp: PPPlus, mods: List<LazerMod>) {
        starRating = pp.difficulty.total ?: 0.0
        if (mods.isNotEmpty()) {
            CS = CalculateApiImpl.applyCS(CS!!, mods)
            AR = CalculateApiImpl.applyAR(AR!!, mods)
            OD = CalculateApiImpl.applyOD(OD!!, mods)
            HP = CalculateApiImpl.applyOD(HP!!, mods)
        }
    }
}