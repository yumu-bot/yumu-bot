package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import org.springframework.stereotype.Service

//@Service("POPULAR_GROUP")
class PopularGroupMapService(private val bindDao: BindDao): MessageService<Long> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Long>): Boolean {
        return false
    }

    override fun HandleMessage(event: MessageEvent, param: Long) {
        val group = event.bot.groups.associateBy { it.id }[param] ?: throw NoSuchElementException("没有这个群")

        val qqIDs = group.allUser.map { it.id }

        // 存在的玩家
        val qqUsers = bindDao.getAllQQBindUser(qqIDs)

    }
}