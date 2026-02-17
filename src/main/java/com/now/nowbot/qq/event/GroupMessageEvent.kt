package com.now.nowbot.qq.event

import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group

interface GroupMessageEvent : MessageEvent {
    override val subject: Group
    override val sender: Contact

    val group: Group
        get() = subject
}
