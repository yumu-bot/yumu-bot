package com.now.nowbot.qq.contact

import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.message.MessageReceipt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

interface Contact {
    val contactID: Long

    val name: String?

    fun sendMessage(msg: MessageChain): MessageReceipt

    fun sendMessage(msg: String): MessageReceipt {
        val m = MessageChainBuilder().addText(msg).build()
        return sendMessage(m)
    }

    fun sendText(msg: String): MessageReceipt {
        return sendMessage(MessageChainBuilder().addText(msg).build())
    }

    fun sendImage(url: URL): MessageReceipt {
        return sendMessage(MessageChainBuilder().addImage(url).build())
    }

    fun sendImage(data: ByteArray): MessageReceipt {
        return sendMessage(MessageChainBuilder().addImage(data).build())
    }

    fun sendVoice(data: ByteArray): MessageReceipt {
        return sendMessage(MessageChainBuilder().addVoice(data).build())
    }

    fun recall(msg: MessageReceipt) {
        try {
            msg.recall()
        } catch (_: Exception) {

        }
    }

    fun recallIn(msg: MessageReceipt, time: Long) {
        msg.recallIn(time)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Contact::class.java)
    }
}
