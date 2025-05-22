package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.GetPoolService.GetPoolParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.MapPoolException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("GET_POOL")
class GetPoolService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService
) : MessageService<GetPoolParam> {

    @JvmRecord
    data class GetPoolParam(val map: Map<String, List<Long>>?, val name: String?, val mode: OsuMode)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<GetPoolParam>
    ): Boolean {
        val matcher = Instruction.GET_POOL.matcher(messageText)
        if (!matcher.find()) return false

        val dataStr: String? = matcher.group("data")
        val nameStr: String? = matcher.group("name")

        if (dataStr.isNullOrBlank()) {
            throw MapPoolException(MapPoolException.Type.GP_Instructions)
        }

        val dataMap = parseDataString(dataStr)
        val mode = getMode(matcher, getFirstMapMode(dataStr)).data!!

        data.value = GetPoolParam(dataMap, nameStr, mode)
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: GetPoolParam) {
        val mapPool = MapPoolDto(param.name, param.mode, param.map, beatmapApiService, calculateApiService)

        if (mapPool.modPools.isEmpty()) throw MapPoolException(MapPoolException.Type.GP_Map_Empty)

        val body = mapOf(
            "pool" to mapPool, "mode" to param.mode.shortName
        )

        val image = imageService.getPanel(body, "H")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("生成图池：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "生成图池")
        }
    }

    fun getFirstMapMode(dataStr: String): OsuMode {
        val dataStrs = dataStr.trim().split("[\"\\s,，\\-|:]+".toRegex())
        if (dataStrs.isEmpty()) return OsuMode.DEFAULT

        val bid = dataStrs.firstNotNullOfOrNull { it.toLongOrNull() } ?: return OsuMode.DEFAULT

        return try {
            beatmapApiService.getBeatMapFromDataBase(bid).mode
        } catch (e: Exception) {
            OsuMode.DEFAULT
        }

    }

    fun parseDataString(dataStr: String): Map<String, List<Long>>? {
        val dataStrs = dataStr.trim().split("[\"\\s,，\\-|:]+".toRegex())
        if (dataStrs.isEmpty()) return null

        val output = LinkedHashMap<String, List<Long>>()

        var mods: String? = ""
        val ids: MutableList<Long?> = ArrayList()

        var status = 0 //0：收取 Mod 状态，1：收取 BID 状态，2：无需收取，直接输出。

        for (i in dataStrs.indices) {
            val s = dataStrs[i]
            if (s.isBlank()) continue

            var mod: String? = null
            var v: Long? = s.toLongOrNull()

            try {
                v = s.toLong()
            } catch (e: NumberFormatException) {
                mod = s
            }

            when (status) {
                0 -> {
                    if (!mod.isNullOrBlank()) {
                        mods = mod
                        mod = null
                        status = 1
                    } else throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParseMissingMap, s, i.toString())
                }

                1 -> {
                    if (!mod.isNullOrBlank()) {
                        if (ids.isEmpty()) {
                            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParseMissingMap, s, i.toString())
                        } else {
                            status = 2
                        }
                    } else {
                        ids.add(v)
                    }
                }
            }

            if (status == 2 || i == dataStrs.size - 1) {
                output[mods!!] = ids.filterNotNull()
                ids.clear()
                mods = null
                status = 0

                if (!mod.isNullOrBlank()) {
                    mods = mod
                    status = 1
                }
            }
        }

        return output
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GetPoolService::class.java)
    }
}
