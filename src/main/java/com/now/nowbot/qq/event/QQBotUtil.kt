package com.now.nowbot.qq.event

import com.fasterxml.jackson.annotation.JsonProperty
import com.mikuac.shiro.core.Bot
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import kotlin.jvm.Throws

// 定义一个可以接收任何格式消息的类
class FlexibleGetMsgResp {
    @JsonProperty("message_id")
    var messageID: Int? = null

    @JsonProperty("message")
    var message: Any? = null // 关键：Any 可以接收 String 或 List

    @JsonProperty("sender")
    var sender: Map<String, Any>? = null
}

@Throws(UnsupportedOperationException::class)
fun Bot.getMessageFromStringOrArray(messageID: Int): MessageChain? {
    val response = this.customRawRequest(
        { "get_msg" },
        mapOf("message_id" to messageID),
        FlexibleGetMsgResp::class.java
    )

    val data = response?.data ?: throw UnsupportedOperationException.BotOperation.MessageUnavailable(messageID)

    // 这里的 data.message 可能是 String，也可能是 ArrayList
    val finalMessageChain = when (val raw = data.message) {
        is String -> MessageChain.MessageChainParser.parse(raw) // 用之前的正则解析
        is List<*> -> MessageChain.parseMessageArray(raw)      // 解析数组格式
        else -> null
    }

    return finalMessageChain
}