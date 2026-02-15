package com.now.nowbot.qq.event

import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException.NotGroup
import java.net.URL

interface MessageEvent : Event {
    val subject: Contact

    val sender: Contact

    val message: MessageChain

    fun reply(message: MessageChain): MessageReceipt {
        return this.subject.sendMessage(message)
    }

    fun reply(message: String): MessageReceipt {
        return this.subject.sendMessage(message)
    }

    fun reply(e: Throwable): MessageReceipt {
        return this.subject.sendMessage(e.message ?: "")
    }

    fun reply(image: ByteArray): MessageReceipt {
        return this.subject.sendImage(image)
    }

    fun reply(image: ByteArray?, message: String): MessageReceipt {
        return this.subject.sendMessage(MessageChainBuilder().addImage(image).addText(message).build())
    }

    fun reply(url: URL): MessageReceipt {
        return this.subject.sendImage(url)
    }

    fun replyVoice(voice: ByteArray): MessageReceipt {
        return this.subject.sendVoice(voice)
    }

    fun replyInGroup(message: MessageChain): MessageReceipt {
        val sub = this.subject

        if (sub is Group) {
            return sub.sendMessage(message)
        } else {
            throw NotGroup()
        }
    }

    fun replyInGroup(message: String): MessageReceipt {
        val sub = this.subject

        if (sub is Group) {
            return sub.sendMessage(message)
        } else {
            throw NotGroup()
        }
    }

    fun replyInGroup(e: Exception): MessageReceipt {
        val sub = this.subject

        if (sub is Group) {
            return sub.sendMessage(e.message ?: "")
        } else {
            throw NotGroup()
        }
    }

    fun replyInGroup(image: ByteArray): MessageReceipt {
        val sub = this.subject

        if (sub is Group) {
            return sub.sendImage(image)
        } else {
            throw NotGroup()
        }
    }

    fun replyInGroup(image: URL): MessageReceipt {
        val sub = this.subject

        if (sub is Group) {
            return sub.sendImage(image)
        } else {
            throw NotGroup()
        }
    }

    fun replyFileInGroup(data: ByteArray, name: String?) {
        val sub = this.subject

        if (sub is Group) {
            sub.sendFile(data, name ?: "Yumu-file")
        } else {
            throw NotGroup()
        }
    }

    val rawMessage: String

    val textMessage: String

    val image: ImageMessage?
        get() = getMessageType<ImageMessage>()

    fun hasImage(): Boolean {
        return this.image != null
    }

    val at: AtMessage?
        get() = getMessageType<AtMessage>()

    fun hasAt(): Boolean {
        return this.at != null
    }

    val replyMessage: ReplyMessage?
        get() = getMessageType<ReplyMessage>()

    fun hasReply(): Boolean {
        return this.replyMessage != null
    }

    val target: Long
        get() = this.at?.target ?: 0L

    companion object {
        // 这个太常用了，所以写进来了，本来是 QQMsgUtil 的 getType
        private inline fun <reified T : Message> MessageEvent.getMessageType(): T? {
            return this.message.messageList
                .filterIsInstance<T>()
                .firstOrNull()
        }
    }
}
