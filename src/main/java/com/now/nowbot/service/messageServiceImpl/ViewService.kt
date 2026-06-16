package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_PAGE
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("VIEW")
class ViewService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao,
) : MessageService<ViewService.ViewParam>, TencentMessageService<ViewService.ViewParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ViewParam>
    ): Boolean {
        val matcher = Instruction.VIEW.matcher(messageText)
        val matcher2 = Instruction.VIEW_VARIATION.matcher(messageText)

        if (matcher.find()) {
            data.value = getParam(event, matcher, isOfficial = false, isVariation = false)
            return true
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, isOfficial = false, isVariation = true)
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: ViewParam
    ): ServiceCallStatistic? {
        event.replyAsync(param.getMessageChain())

        return ServiceCallStatistic.build(event, beatmapID = param.beatmap.beatmapID)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isOfficial: Boolean = false, isVariation: Boolean = false): ViewParam {
        val beatmapID = matcher.group(FLAG_BID)?.toLongOrNull()
            ?: dao.getLastBeatmapID(event)
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val beatmap = beatmapApiService.getBeatmap(beatmapID)

        if (beatmap.mode != OsuMode.MANIA && beatmap.mode != OsuMode.TAIKO) {
            throw UnsupportedOperationException.OnlyManiaAndTaiko()
        }

        beatmapApiService.getBeatmapFileString(beatmapID)
            ?: throw NoSuchElementException.BeatmapCache(beatmapID)

        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1

        val row = if (isOfficial) {
            1
        } else {
            5
        }

        return ViewParam(beatmap, page, beatmap.mode, row, isVariation)
    }

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): ViewParam? {
        val matcher = OfficialInstruction.VIEW.matcher(messageText)
        val matcher2 = OfficialInstruction.VIEW_VARIATION.matcher(messageText)

        if (matcher.find()) {
            return getParam(event, matcher, isOfficial = true, isVariation = false)
        } else if (matcher2.find()) {
            return getParam(event, matcher2, isOfficial = true, isVariation = true)
        }

        return null
    }

    override fun reply(
        event: MessageEvent,
        param: ViewParam
    ): MessageChain? {
        return param.getMessageChain()
    }

    private fun ViewParam.getMessageChain(): MessageChain {
        return when(this.mode) {
            OsuMode.TAIKO -> MessageChain(imageService.getPanel(this, "V2"))
            OsuMode.MANIA -> MessageChain(imageService.getPanel(this, "V"))

            else -> throw UnsupportedOperationException.OnlyManiaAndTaiko()
        }
    }

    data class ViewParam(
        val beatmap: Beatmap,
        val page: Int,
        val mode: OsuMode,
        val row: Int = 5,
        val variation: Boolean = false
    )
}