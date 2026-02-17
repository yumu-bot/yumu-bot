package com.now.nowbot.qq.onebot.event

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.qq.onebot.contact.GroupContact

class GroupMessageEvent(bot: Bot, val groupEvent: GroupMessageEvent) : MessageEvent(
    groupEvent, bot
), com.now.nowbot.qq.event.GroupMessageEvent {
    override val group: com.now.nowbot.qq.contact.Group
        get() = Group(bot!!.trueBot, groupEvent.groupId)

    override val sender: GroupContact
        get() = GroupContact(
            bot!!.trueBot,
            groupEvent.sender.userId,
            groupEvent.groupId,
            groupEvent.sender.nickname,
            groupEvent.sender.role
        )

    override val subject: Group
        get() = Group(bot!!.trueBot, groupEvent.groupId)
}
