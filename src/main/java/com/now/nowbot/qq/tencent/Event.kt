package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.Bot
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.message.MessageChain

class Event(
    val user: com.now.nowbot.qq.tencent.Contact,
    val message: String
) : GroupMessageEvent {
    val chain by lazy {
        MessageChain(message)
    }

    override fun getBot(): Bot? = null

    override fun getSubject(): Group = user

    override fun getSender(): Contact = user

    override fun getMessage(): MessageChain = chain

    override fun getRawMessage(): String = message

    override fun getTextMessage(): String = message

}