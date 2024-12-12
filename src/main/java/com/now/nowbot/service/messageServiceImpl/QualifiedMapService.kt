package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@Service("QUALIFIED_MAP") class QualifiedMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<Matcher> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.QUALIFIED_MAP.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) { // 获取参数
        val statusStr = matcher.group("status") ?: "q"
        val sortStr = matcher.group("sort") ?: "ranked_asc"
        val rangeStr = matcher.group("range") ?: "12"
        val mode = OsuMode.getMode(matcher.group(FLAG_MODE)).modeValue

        val range = try {
            rangeStr.toInt()
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param)
        }

        if (range < 1 || range > 999) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param)
        }

        val tries = max(floor(range / 50.0).roundToInt() + 1, 10)

        val status = DataUtil.getStatus(statusStr)
        val sort = DataUtil.getSort(sortStr)

        val query = mapOf<String, Any>(
            "m" to mode,
            "s" to if (status == "any" || status == null) "qualified" else status,
            "sort" to sort,
            "page" to 1,
        )

        try {
            val search = beatmapApiService.searchBeatMapSet(query, tries)

            val img = imageService.getPanelA2(search)
            event.reply(img)
        } catch (e: Exception) {
            log.error("过审谱面：", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "过审谱面")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QualifiedMapService::class.java)
    }
}
