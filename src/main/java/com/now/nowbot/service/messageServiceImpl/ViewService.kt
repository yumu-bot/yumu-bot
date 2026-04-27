package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_PAGE
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("VIEW")
class ViewService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao,
) : MessageService<ViewService.ViewParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ViewParam>
    ): Boolean {
        val matcher = Instruction.VIEW.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val beatmapID = matcher.group(FLAG_BID)?.toLongOrNull()
            ?: dao.getLastBeatmapID(event.subject.contactID, null, LocalDateTime.now().minusHours(24L))
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val beatmap = beatmapApiService.getBeatmap(beatmapID)

        if (beatmap.mode != OsuMode.MANIA) {
            throw UnsupportedOperationException.OnlyMania()
        }

        beatmapApiService.getBeatmapFileString(beatmapID)
            ?: throw NoSuchElementException.BeatmapCache(beatmapID)

        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1

        data.value = ViewParam(beatmap, page, beatmap.mode)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: ViewParam
    ): ServiceCallStatistic? {
        val image = imageService.getPanel(param, "V")

        event.reply(image)

        return ServiceCallStatistic.build(event, beatmapID = param.beatmap.beatmapID)
    }

    data class ViewParam(
        val beatmap: Beatmap,
        val page: Int,
        val mode: OsuMode
    )
}