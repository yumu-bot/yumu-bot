package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.cache.QQMessageCacheProvider
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.TimestampConverter
import com.now.nowbot.mapper.BeatmapCountMapper
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.TipsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(
    private val beatmapCountMapper: BeatmapCountMapper,
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
        val bid = param.trim().toLongOrNull() ?: return null

        val delta = beatmapCountMapper.getTimeStampByBeatmapIDs(listOf(bid)).firstOrNull()?.delta ?: return null

        val result = TimestampConverter.bytesToIntArray(delta)

        event.replyAsync("谱面 $bid 的结果：${result.take(20).joinToString(", ")}")

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
