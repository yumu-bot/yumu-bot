package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.PerformancePlusDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("PP_PLUS_MAP")
class PPPlusMapService(
    private val plusDao: PerformancePlusDao,
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
            ?: dao.getLastBeatmapID(event)
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val mods = matcher.group(FLAG_MOD).toLazerMods()
        data.value = PPPlusParam(bid, mods)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: PPPlusParam): ServiceCallStatistic {
        val map = beatmapApiService.getBeatmap(param.bid)

        // 不支持其他模式
        if (map.mode != OsuMode.OSU) {
            throw UnsupportedOperationException.OnlyStandard()
        }

        val pp = try {
            plusDao.getBeatmapPerformancePlusMax(map, param.mods)
        } catch (e: Exception) {
            log.error(e.message)
            throw IllegalStateException.Fetch("表现分加（谱面）")
        } ?: throw NoSuchElementException.BeatmapDownload(map.previewName)

        map.applyDimensions(param.mods)

        val dataMap = mapOf(
            "isUser" to false,
            "me" to map,
            "my" to pp,
            "isVs" to false
        )

        val image = imageService.getPanel(dataMap, "B3")
        event.replyAsync(image)

        return ServiceCallStatistic.build(event, beatmapID = map.beatmapID, beatmapsetID = map.beatmapsetID)
    }

    private fun Beatmap.applyDimensions(mods: List<LazerMod>) {
        mods.takeIf { it.isNotEmpty() }?.let { nonEmptyMods ->
            cs = cs?.let { BeatmapUtil.applyCS(it, nonEmptyMods, mode) }
            ar = ar?.let { BeatmapUtil.applyAR(it, nonEmptyMods) }
            od = od?.let { BeatmapUtil.applyOD(it, nonEmptyMods, mode) }
            hp = hp?.let { BeatmapUtil.applyHP(it, nonEmptyMods) }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPPlusMapService::class.java)
    }
}