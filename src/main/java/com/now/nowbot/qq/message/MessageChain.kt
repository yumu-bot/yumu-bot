package com.now.nowbot.qq.message

import java.net.URL
import java.util.*

class MessageChain {
    class MessageChainBuilder {
        private val msgChain = MessageChain()

        fun addImage(url: URL): MessageChainBuilder {
            msgChain.addMessage(ImageMessage(url))
            return this
        }

        fun addImage(data: ByteArray?): MessageChainBuilder {
            msgChain.addMessage(ImageMessage(data))
            return this
        }

        fun addImage(pathStr: String): MessageChainBuilder {
            msgChain.addMessage(ImageMessage(pathStr))
            return this
        }

        fun addVoice(data: ByteArray): MessageChainBuilder {
            msgChain.addMessage(VoiceMessage(data))
            return this
        }

        fun addText(msg: String): MessageChainBuilder {
            msgChain.addMessage(TextMessage(msg))
            return this
        }

        fun addAt(qq: Long): MessageChainBuilder {
            msgChain.addMessage(AtMessage(qq))
            return this
        }

        fun addAtAll(): MessageChainBuilder {
            msgChain.addMessage(AtMessage())
            return this
        }

        val isEmpty: Boolean
            get() = msgChain.isEmpty

        val isNotEmpty: Boolean
            get() = !isEmpty

        fun build(): MessageChain {
            return msgChain
        }
    }

    internal constructor() {
        messageList = LinkedList()
    }

    constructor(messages: List<Message>) {
        messageList = LinkedList(messages)
    }

    var messageList: LinkedList<Message>
        private set

    constructor(msg: String) {
        messageList = LinkedList()
        addMessage(TextMessage(msg))
    }

    constructor(e: Throwable) {
        messageList = LinkedList()
        addMessage(TextMessage(e.message ?: ""))
    }

    fun addMessage(msg: Message): MessageChain {
        messageList.add(msg)
        return this
    }

    val isEmpty: Boolean
        get() = messageList.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty
    val rawMessage: String
        get() {
            val sb = StringBuilder()
            messageList.forEach {
                sb.append(it.toString())
            }
            return sb.toString()
        }
}
