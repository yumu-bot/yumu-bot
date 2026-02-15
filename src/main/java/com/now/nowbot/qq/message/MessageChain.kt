package com.now.nowbot.qq.message

import java.net.URI
import java.net.URL
import java.util.*
import kotlin.collections.get

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

    constructor(text: String) {
        messageList = LinkedList()
        addMessage(TextMessage(text))
    }

    constructor(image: ByteArray) {
        messageList = LinkedList()
        addMessage(ImageMessage(image))
    }

    constructor(text: String, image: ByteArray) {
        messageList = LinkedList()
        addMessage(ImageMessage(image))
        addMessage(TextMessage(text))
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

    object MessageChainParser {
        // 匹配 CQ 码的正则表达式：[CQ:类型,参数=值,参数=值]
        private val CQ_REGEX = Regex("""\[CQ:([a-z]+), (.*?)]""", RegexOption.IGNORE_CASE)

        fun parse(raw: String): MessageChain {
            val builder = MessageChainBuilder()
            var lastIndex = 0

            // 查找所有匹配的 CQ 码
            CQ_REGEX.findAll(raw).forEach { match ->
                // 1. 处理 CQ 码之前的普通文本
                val textBefore = raw.substring(lastIndex, match.range.first)
                if (textBefore.isNotEmpty()) {
                    builder.addText(textBefore)
                }

                // 2. 处理 CQ 码本身
                val type = match.groupValues[1]       // 例如 "image", "at", "record"
                val params = parseParams(match.groupValues[2]) // 解析参数对

                when (type) {
                    "image" -> {
                        // OneBot 中图片可能是 url, file(路径或base64)
                        val url = params["url"]
                        val file = params["file"]
                        if (url != null) builder.addImage(URI(url).toURL())
                        else if (file != null) builder.addImage(file)
                    }
                    "at" -> {
                        val qq = params["qq"]
                        if (qq == "all") builder.addAtAll()
                        else qq?.toLongOrNull()?.let { builder.addAt(it) }
                    }
                    "record" -> {
                        // 语音消息，通常 OneBot 只有路径或 URL
                        // 注意：你的 Builder addVoice 只接受 ByteArray，这里可能需要下载
                        val url = params["url"]
                        if (url != null) {
                            // 警告：这里是同步下载，实际建议异步处理
                            builder.addVoice(URI(url).toURL().readBytes())
                        }
                    }
                    // 可以继续添加其他 CQ 码支持...
                    else -> builder.addText(match.value) // 不认识的 CQ 码原样当作文本
                }

                lastIndex = match.range.last + 1
            }

            // 3. 处理剩余的尾部文本
            if (lastIndex < raw.length) {
                builder.addText(raw.substring(lastIndex))
            }

            return builder.build()
        }

        // 将 "qq=123,name=abc" 解析为 Map
        private fun parseParams(paramStr: String): Map<String, String> {
            return paramStr.split(",")
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .associate { it[0].trim() to it[1].trim() }
        }
    }

    object MessageArrayParser {
        fun parse(messageObj: Any?): MessageChain {
            val builder = MessageChainBuilder()

            // 如果返回的是字符串（CQ码模式）
            if (messageObj is String) {
                return MessageChainParser.parse(messageObj) // 调用之前写的正则解析器
            }

            // 如果返回的是数组（JSON 数组模式）
            if (messageObj is List<*>) {
                messageObj.forEach { item ->
                    val map = item as? Map<*, *> ?: return@forEach
                    val type = map["type"] as? String
                    val data = map["data"] as? Map<*, *> ?: return@forEach

                    when (type) {
                        "text" -> builder.addText(data["text"] as? String ?: "")
                        "at" -> {
                            val qq = data["qq"].toString()
                            if (qq == "all") builder.addAtAll()
                            else builder.addAt(qq.toLong())
                        }
                        "image" -> {
                            val url = data["url"] as? String
                            val file = data["file"] as? String
                            if (url != null) builder.addImage(URI(url).toURL())
                            else if (file != null) builder.addImage(file)
                        }
                        "record" -> {
                            // 语音处理
                            (data["url"] as? String)?.let {
                                builder.addVoice(URI(it).toURL().readBytes())
                            }
                        }
                    }
                }
            }

            return builder.build()
        }
    }

    companion object {
        fun parseMessageArray(list: List<*>): MessageChain {
            val builder = MessageChainBuilder()
            list.forEach { item ->
                val segment = item as? Map<*, *> ?: return@forEach
                val type = segment["type"] as? String
                val segmentData = segment["data"] as? Map<*, *> ?: return@forEach

                when (type) {
                    "text" -> builder.addText(segmentData["text"]?.toString() ?: "")
                    "image" -> {
                        val url = segmentData["url"]?.toString()
                        if (url != null) builder.addImage(URI(url).toURL())
                        else builder.addImage(segmentData["file"]?.toString() ?: "")
                    }
                    "at" -> {
                        val qq = segmentData["qq"]?.toString()
                        if (qq == "all") builder.addAtAll() else builder.addAt(qq?.toLong() ?: 0L)
                    }
                    "record" -> {
                        // 语音处理
                        (segmentData["url"] as? String)?.let {
                            builder.addVoice(URI(it).toURL().readBytes())
                        }
                    }
                }
            }
            return builder.build()
        }
    }
}
