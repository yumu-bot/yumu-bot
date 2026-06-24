package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(
    private val userApiService: OsuUserApiService,
    private val botContainer: BotContainer,
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText == "!yuumu" && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replace("!yuumu", "")
            return true
        } else {
            return false
        }

        // return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        val ls = userApiService.getQuickplayLeaderboard()

        val sb = StringBuilder()

        ls.second.take(5).forEach {
            sb.append(it.toString()).append("\n")
        }

        event.reply(sb.toString())

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
