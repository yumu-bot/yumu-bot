package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.cache.QQMessageCacheProvider
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.TipsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(
    private val matchApiService: OsuMatchApiService
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText.contains("!yuumu") && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replaceFirst("!yuumu", "")
            return true
        } else {
            return false
        }

        // return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        val ids = param.replace("\\s*".toRegex(), "").split(",").map { it.toLongOrNull() ?: throw TipsException("转换 $it 失败") }

        val matches = ids.flatMap { i ->
            val m = matchApiService.getMatch(i)

            m.events.mapNotNull { e -> e.round }.flatMap { r ->
                r.scores.map { s -> r.beatmap?.previewName to s }
            }
        }

        var ok = 0

        matches.forEach { (preview, score) ->

            if (String.format("%.2f", score.accuracy * 100.0).replace(".", "").contains("727", ignoreCase = true)) {
                ok ++
                event.replyAsync("找到acc里有 727 的玩家：${score.userID}, ${preview}, ${score.accuracy}")
            }
        }

        if (ok == 0) {
            event.replyAsync("没找到符合条件的对象")
        }

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
