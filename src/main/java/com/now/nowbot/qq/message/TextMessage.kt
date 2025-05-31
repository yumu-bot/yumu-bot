package com.now.nowbot.qq.message

class TextMessage(var message: String) : Message() {
    override fun toString(): String {
        return message
    }

    override fun toJson(): JsonMessage {
        return JsonMessage("text", mapOf("text" to message))
    }
}
