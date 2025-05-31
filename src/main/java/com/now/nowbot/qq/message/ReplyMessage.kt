package com.now.nowbot.qq.message

class ReplyMessage : Message {
    val id: Long
    val text: String?

    constructor(messageId: Long) {
        id = messageId
        this.text = null
    }

    constructor(messageId: Long, text: String?) {
        id = messageId
        this.text = text
    }

    override fun toJson(): JsonMessage {
        return JsonMessage("reply", mapOf("id" to id))
    }

    override fun toString(): String {
        return "[reply:${id}]"
    }
}
