package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.dao.SkillDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.model.skill.Skill6
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.regex.Matcher
import kotlin.math.absoluteValue

@Service("MAP_MINUS") class MapMinusService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val skillDao: SkillDao,
    private val dao: ServiceCallStatisticsDao,
) : MessageService<MapMinusService.MapMinusParam>, TencentMessageService<MapMinusService.MapMinusParam> {

    data class MapMinusParam(val bid: Long, val mode: OsuMode, val rate: Double = 1.0, val mods: List<LazerMod>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MapMinusParam>,
    ): Boolean {
        val m = Instruction.MAP_MINUS.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = getMapMinusParam(event, m)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: MapMinusParam): ServiceCallStatistic? {
        val image = getMapMinusImage(param, beatmapApiService, calculateApiService, imageService)

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面 Minus：发送失败", e)
            throw IllegalStateException.Send("谱面 Minus")
        }

        return ServiceCallStatistic.build(event, beatmapID = param.bid, mode = param.mode)
    }

    override fun accept(event: MessageEvent, messageText: String): MapMinusParam? {
        val m = OfficialInstruction.MAP_MINUS.matcher(messageText)
        if (!m.find()) {
            return null
        }

        return getMapMinusParam(event, m)
    }

    override fun reply(event: MessageEvent, param: MapMinusParam): MessageChain? {
        val image = getMapMinusImage(param, beatmapApiService, calculateApiService, imageService)
        return MessageChainBuilder().addImage(image).build()
    }

    private fun getMapMinusParam(event: MessageEvent, matcher: Matcher): MapMinusParam {
        val modsList: List<LazerMod> = LazerMod.getModsList(matcher.group(FLAG_MOD))

        val bid = matcher.group("bid")?.toLongOrNull()
            ?: dao.getLastBeatmapID(event.subject.contactID, name = null, LocalDateTime.now().minusHours(24))
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val rate = matcher.group("rate")?.toDoubleOrNull() ?: 1.0

        if (rate < 0.1) throw IllegalArgumentException.ExceedException.RateTooSmall()
        if (rate > 5.0) throw IllegalArgumentException.ExceedException.RateTooLarge()

        return MapMinusParam(bid, OsuMode.MANIA, rate, modsList)
    }

    private fun getMapMinusImage(
        param: MapMinusParam,
        beatmapApiService: OsuBeatmapApiService,
        calculateApiService: OsuCalculateApiService,
        imageService: ImageService,
    ): ByteArray {
        val fileStr: String
        val map: Beatmap = beatmapApiService.getBeatmap(param.bid)

        calculateApiService.applyStarToBeatmap(map, param.mode, param.mods)

        val isChangedRating = param.mods.isAffectStarRating()

        try {
            fileStr = beatmapApiService.getBeatmapFileString(param.bid)!!
        } catch (_: Exception) {
            throw NoSuchElementException.BeatmapCache(param.bid)
        }

        if (map.mode.isNotConvertAble(param.mode)) {
            throw UnsupportedOperationException.OnlyMania()
        }

        val file = try {
            OsuFile(fileStr)
        } catch (_: NullPointerException) {
            throw IllegalStateException.Fetch("谱面文件")
        }

        val clockRate = if (isChangedRating) {
            LazerMod.getModSpeedForStarCalculate(param.mods).toDouble()
        } else {
            param.rate
        }

        val mapMinus = Skill6(
            file,
            param.mode,
            clockRate,
        )

        // val type = SkillType.getType(mapMinus)

        if ((clockRate - 1.0).absoluteValue <= 1e-4) {
            // skillDao.saveAndUpdateSkill(map, param.mode, mapMinus)
        }

        val body = mapOf(
            "beatmap" to map,
            "map_minus" to mapMinus,
            "type" to "null",//type.keys.first().name,
            "type_percent" to 0//type.values.first()
        )

        return imageService.getPanel(body, "B2")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapMinusService::class.java)
    }
}
