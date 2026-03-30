package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.springframework.stereotype.Service

@Service("GUESS")
class GuessService(
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService
): MessageService<GuessService.GuessParam> {

    data class GuessParam(
        val beatmaps: List<Beatmap>,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GuessParam>
    ): Boolean {
        data.value = GuessParam(emptyList())

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: GuessParam
    ): ServiceCallStatistic? {
        val image = imageService.getPanel(param, "A8")

        event.reply(image)

        return null
    }
}