package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.Bot
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.message.MessageChain

class Event(
    val user: com.now.nowbot.qq.tencent.Contact,
    val messageString: String
) : GroupMessageEvent {
    val chain by lazy {
        MessageChain(messageString)
    }

    override val bot: Bot? = null

    override val message: MessageChain = chain

    override val subject: Group = user

    override val sender: Contact = user

    override val rawMessage: String = messageString

    override val textMessage: String = messageString

}