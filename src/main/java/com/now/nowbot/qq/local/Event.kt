package com.now.nowbot.qq.local

import com.now.nowbot.qq.Bot
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.Event
import com.now.nowbot.qq.local.contact.LocalGroup
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage

open class Event internal constructor(override val bot: Bot?) : Event {

    open class MessageEvent internal constructor(
        private val localBot: com.now.nowbot.qq.local.Bot?,
        val group: LocalGroup,
        override val textMessage: String,
    ) : Event, com.now.nowbot.qq.event.MessageEvent {
        override val bot: Bot?
            get() = localBot

        override val subject: Contact
            get() = group

        override val sender: Contact
            get() = group

        override val message
            get() = MessageChain(listOf(TextMessage(this.textMessage)))

        override val rawMessage: String
            get() = textMessage
    }

    class GroupMessageEvent(val localBot: com.now.nowbot.qq.local.Bot?, group: LocalGroup, message: String) :
        MessageEvent(localBot, group, message), com.now.nowbot.qq.event.GroupMessageEvent {

        override val bot: Bot?
            get() = localBot

        override val subject: Group
            get() = group

        override fun getGroup(): Group {
            return super.group
        }
    }
}