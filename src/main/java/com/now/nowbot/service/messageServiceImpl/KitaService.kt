package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.serviceException.KitaException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Matcher

@Service("KITA")
class KitaService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<Matcher> {

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val matcher2 = Instruction.DEPRECATED_YMK.matcher(messageText)
        if (matcher2.find()) throw KitaException(KitaException.Type.KITA_Deprecated_K)

        val m = Instruction.KITA.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val bid: Long
        val mod: String
        val position: Short
        val beatMap: BeatMap
        val hasBG = matcher.group("noBG") == null
        val BIDstr: String =
            matcher.group(FLAG_BID) ?: throw KitaException(KitaException.Type.KITA_Parameter_NoBid)

        try {
            bid = BIDstr.toLong()
        } catch (e: NumberFormatException) {
            throw KitaException(KitaException.Type.KITA_Parameter_BidError)
        }

        if (matcher.group(FLAG_MOD) == null) {
            mod = "NM"
            position = 1
        } else {
            try {
                val modStr = matcher.group("mod").uppercase(Locale.getDefault())
                mod = modStr.substring(0, 2)
                position = modStr.substring(2).toShort()
            } catch (e: NumberFormatException) {
                throw KitaException(KitaException.Type.KITA_Parameter_ModError)
            }
        }

        val round =
            if (matcher.group("round") == null) {
                "Unknown"
            } else {
                try {
                    matcher.group("round")
                } catch (e: NumberFormatException) {
                    throw KitaException(KitaException.Type.KITA_Parameter_RoundError)
                }
            }

        try {
            beatMap = beatmapApiService.getBeatMapFromDataBase(bid)
        } catch (e: Exception) {
            throw KitaException(KitaException.Type.KITA_Map_FetchFailed)
        }

        if (hasBG) {
            try {
                val image = imageService.getPanelDelta(beatMap, round, mod, position, hasBG)
                event.reply(image)
            } catch (e: Exception) {
                log.error("KITA", e)
                throw KitaException(KitaException.Type.KITA_Send_Error)
            }
        } else {
            val group = event.subject
            if (group is Group) {
                try {
                    val image = imageService.getPanelDelta(beatMap, round, mod, position, hasBG)
                    group.sendFile(image, "${matcher.group("bid")}${' '}${mod}${position}.png")
                } catch (e: Exception) {
                    log.error("KITA-X", e)
                    throw KitaException(KitaException.Type.KITA_Send_Error)
                }
            } else {
                throw KitaException(KitaException.Type.KITA_Send_NotGroup)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(KitaService::class.java)
    }
}
