package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_MOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
        val m = Instruction.KITA.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: Matcher) {
        val mod: String
        val position: Short
        val beatmap: Beatmap
        val hasBG = param.group("noBG") == null
        val bidStr: String? = param.group(FLAG_BID)

        val bid: Long = bidStr?.toLongOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()

        if (param.group(FLAG_MOD) == null) {
            mod = "NM"
            position = 1
        } else {
            val modStr = param.group("mod")?.uppercase()?.substring(0, 2) ?: throw IllegalArgumentException.WrongException.Mod()
            mod = modStr.substring(0, 2)
            position = modStr.substring(2).toShortOrNull() ?: throw IllegalArgumentException.WrongException("请输入正确的位置！")
        }

        val round =
            if (param.group("round") == null) {
                "Unknown"
            } else {
                param.group("round")
            }

        beatmap = beatmapApiService.getBeatmapFromDatabase(bid)

        if (hasBG) {
            try {
                val image = imageService.getPanelDelta(beatmap, round, mod, position, true)
                event.reply(image)
            } catch (e: Exception) {
                log.error("KITA", e)
                throw IllegalStateException.Send("喜多面板")
            }
        } else {
            val group = event.subject
            if (group is Group) {
                try {
                    val image = imageService.getPanelDelta(beatmap, round, mod, position, false)
                    group.sendFile(image, "${param.group("bid")} ${mod}${position}.png")
                } catch (e: Exception) {
                    log.error("KITA-X", e)
                    throw IllegalStateException.Send("喜多面板")
                }
            } else {
                throw UnsupportedOperationException.NotGroup()
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(KitaService::class.java)
    }
}
