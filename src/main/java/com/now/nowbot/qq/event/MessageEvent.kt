package com.now.nowbot.qq.event

import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException.NotGroup
import org.slf4j.LoggerFactory
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MessageEvent : Event {
    val subject: Contact

    val sender: Contact

    val message: MessageChain

    fun async(block: () -> Unit, onError: ((Throwable) -> Unit)? = null) {
        Thread.ofVirtual().start {
            try {
                block.invoke()
            } catch (e: Exception) {
                if (onError != null) {
                    onError.invoke(e)
                } else {
                    log.warn("发送消息：发生未捕获异常: ${e.message}")
                }
            }
        }
    }


    fun replyAndRecallAsync(message: Any, recallIn: Duration = 30.seconds, onError: ((Throwable) -> Unit)? = null) {
        async(
            { when(message) {
                is String -> reply(message)
                is MessageChain -> reply(message)
                is ByteArray -> reply(message)
                is Throwable -> reply(message)
                is URL -> reply(message)
                else -> reply(message.toString())
            }.recallIn(recallIn.inWholeMilliseconds) },
            onError
        )
    }

    fun replyAsync(message: MessageChain, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(message) },
            onError
        )
    }

    fun replyAsync(message: String, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(message) },
            onError
        )
    }

    fun replyAsync(e: Throwable, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(e) },
            onError
        )
    }

    fun replyAsync(image: ByteArray, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(image) },
            onError
        )
    }

    fun replyAsync(image: ByteArray?, message: String, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(image, message) },
            onError
        )
    }

    fun replyAsync(url: URL, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(url) },
            onError
        )
    }

    fun replyAsync(any: Any, onError: ((Throwable) -> Unit)? = null) {
        async(
            { reply(any) },
            onError
        )
    }

    fun replyVoiceAsync(voice: ByteArray, onError: ((Throwable) -> Unit)? = null) {
        async(
            { replyVoice(voice) },
            onError
        )
    }

    fun replyFileInGroupAsync(data: ByteArray, name: String?, onError: ((Throwable) -> Unit)? = null) {
        async(
            { replyFileInGroup(data, name) },
            onError
        )
    }

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

    fun reply(any: Any): MessageReceipt {
        return this.subject.sendMessage(any.toString())
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
        get() = getMessageType<AtMessage>()?.target ?: 0L

    val targets: List<Long>
        get() = getMessageTypes<AtMessage>().map { it.target }

    companion object {
        // 这个太常用了，所以写进来了，本来是 QQMsgUtil 的 getType
        private inline fun <reified T : Message> MessageEvent.getMessageType(): T? {
            return getMessageTypes<T>().firstOrNull()
        }
        private inline fun <reified T : Message> MessageEvent.getMessageTypes(): List<T> {
            return this.message.messageList
                .filterIsInstance<T>()
        }

        private val log = LoggerFactory.getLogger(MessageEvent::class.java)
    }
}
