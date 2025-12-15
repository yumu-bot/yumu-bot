package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.PPPlus
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.PerformancePlusService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import com.yumu.core.constants.log
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

//@Service("PP_PLUS_MAP")
class PPPlusMapService(
    private val performancePlusService: PerformancePlusService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<PPPlusMapService.PPPlusParam> {

    data class PPPlusParam(
        val bid: Long,
        val mods: List<LazerMod>,
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<PPPlusParam>): Boolean {
        val matcher = Instruction.PP_PLUS_MAP.matcher(messageText)
        if (!matcher.find()) return false

        val bidStr = matcher.group(FLAG_BID)
        if (bidStr.isNullOrBlank()) {
            return false
        }
        val bid = bidStr.toLongOrNull()
            ?: dao.getLastBeatmapID(event.subject.id, null, LocalDateTime.now().minusHours(24L))
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val mods = LazerMod.getModsList(matcher.group(FLAG_MOD))
        data.value = PPPlusParam(bid, mods)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: PPPlusParam): ServiceCallStatistic {
        val map = try {
            beatmapApiService.getBeatmapFromDatabase(param.bid)
        } catch (_: Exception) {
            throw NoSuchElementException.Beatmap(param.bid)
        }

        // 不支持其他模式
        if (map.mode != OsuMode.OSU) {
            throw UnsupportedOperationException.OnlyStandard()
        }
        val pp = try {
            performancePlusService.getMapPerformancePlus(param.bid, param.mods)!!
        } catch (e: Exception) {
            if (e is WebClientResponseException) {
                log.error { e.responseBodyAsString }
            } else {
                log.error { e.message }
            }
            throw IllegalStateException.Fetch("PP+")
        }

        map.addPPPlus(pp, param.mods)
        val dataMap = mapOf(
            "isUser" to false,
            "me" to map,
            "my" to pp,
            "isVs" to false
        )

        val image = imageService.getPanel(dataMap, "B3")
        event.reply(image)

        return ServiceCallStatistic.build(event, beatmapID = map.beatmapID, beatmapsetID = map.beatmapsetID)
    }

    private fun Beatmap.addPPPlus(pp: PPPlus, mods: List<LazerMod>) {
        starRating = pp.difficulty?.total ?: 0.0
        if (mods.isNotEmpty()) {
            cs = CalculateApiImpl.applyCS(cs!!, mods)
            ar = CalculateApiImpl.applyAR(ar!!, mods)
            od = CalculateApiImpl.applyOD(od!!, mods, OsuMode.OSU)
            hp = CalculateApiImpl.applyHP(hp!!, mods)
        }
    }
}