package com.now.nowbot.qq.onebot

import com.mikuac.shiro.core.Bot
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.qq.message.ReplyMessage
import com.now.nowbot.qq.onebot.contact.Contact
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class OneBotMessageReceipt private constructor() : MessageReceipt() {
    var messageID: Int = 0
    var bot: Bot? = null
    var contact: Contact? = null

    override fun recall() {
        bot?.deleteMsg(messageID) ?: return
    }

    override fun recallIn(time: Long) {
        bot?.let {
            executor.schedule({
                it.deleteMsg(messageID)
            }, time, TimeUnit.MILLISECONDS)
        }
    }

    override fun getTarget(): com.now.nowbot.qq.contact.Contact? {
        return contact
    }

    override fun reply(): ReplyMessage {
        return ReplyMessage(messageID.toLong())
    }

    companion object {
        private val executor: ScheduledExecutorService

        init {
            val threadFactory = Thread.ofVirtual().name("v-OneBotMessageReceipt", 50).factory()
            executor = Executors.newScheduledThreadPool(Int.MAX_VALUE, threadFactory)
        }

        fun create(bot: Bot, messageID: Int, contact: Contact?): OneBotMessageReceipt {
            val r = OneBotMessageReceipt()
            r.messageID = messageID
            r.bot = bot
            r.contact = contact
            return r
        }

        fun create(): OneBotMessageReceipt {
            return OneBotMessageReceipt()
        }
    }
}
