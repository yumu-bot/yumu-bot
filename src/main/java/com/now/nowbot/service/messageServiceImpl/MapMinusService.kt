package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.skill.SkillType
import com.now.nowbot.model.skill.Skill
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.serviceException.MapMinusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("MAP_MINUS") class MapMinusService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
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

        data.value = getMapMinusParam(m)
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: MapMinusParam) {
        val image = getMapMinusImage(param, beatmapApiService, calculateApiService, imageService)

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱面 Minus：发送失败", e)
            throw MapMinusException(MapMinusException.Type.MM_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MapMinusParam? {
        val m2 = OfficialInstruction.MAP_MINUS.matcher(messageText)
        if (!m2.find()) {
            return null
        }

        return getMapMinusParam(m2)
    }

    override fun reply(event: MessageEvent, param: MapMinusParam): MessageChain? {
        val image = getMapMinusImage(param, beatmapApiService, calculateApiService, imageService)
        return MessageChainBuilder().addImage(image).build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapMinusService::class.java)

        private fun getMapMinusParam(matcher: Matcher): MapMinusParam {
            val modsList: List<LazerMod> = LazerMod.getModsList(matcher.group(FLAG_MOD))

            if (matcher.group("bid") == null) throw MapMinusException(MapMinusException.Type.MM_Bid_Error)

            val bid = try {
                matcher.group("bid").toLong()
            } catch (e: NumberFormatException) {
                throw MapMinusException(MapMinusException.Type.MM_Bid_Error)
            }

            val rate = if (matcher.group("rate").isNullOrBlank().not()) {
                try {
                    matcher.group("rate").toDouble()
                } catch (e: NumberFormatException) {
                    throw MapMinusException(MapMinusException.Type.MM_Rate_Error)
                }
            } else {
                1.0
            }

            if (rate < 0.1) throw MapMinusException(MapMinusException.Type.MM_Rate_TooSmall)
            if (rate > 5.0) throw MapMinusException(MapMinusException.Type.MM_Rate_TooLarge)

            return MapMinusParam(bid, OsuMode.MANIA, rate, modsList)
        }

        private fun getMapMinusImage(
            param: MapMinusParam,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
            imageService: ImageService,
        ): ByteArray {
            val fileStr: String
            val map: Beatmap
            val isChangedRating = LazerMod.hasStarRatingChange(param.mods)

            try {

                map = beatmapApiService.getBeatMap(param.bid)

                if (isChangedRating) {
                    map.starRating = calculateApiService.getBeatMapStarRating(map.beatmapID, map.mode, param.mods)
                }
                fileStr = beatmapApiService.getBeatMapFileString(param.bid)!!
            } catch (e: Exception) {
                throw MapMinusException(MapMinusException.Type.MM_Map_NotFound)
            }

            if (map.mode.isNotConvertAble(param.mode)) {
                throw MapMinusException(MapMinusException.Type.MM_Function_NotSupported)
            }

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

            val type = SkillType.getType(mapMinus)

            val image: ByteArray

            try {
                val body = mapOf(
                    "beatmap" to map,
                    "map_minus" to mapMinus,
                    "type" to type.keys.first().name,
                    "type_percent" to type.values.first()
                )

                image = imageService.getPanel(body, "B2")
            } catch (e: Exception) {
                log.error("谱面 Minus：渲染失败", e)
                throw MapMinusException(MapMinusException.Type.MM_Render_Error)
            }

            return image
        }
    }
}
