package com.now.nowbot.qq.message

class ReplyMessage : Message {
    val id: Long
    val text: String?

    constructor(messageID: Long) {
        id = messageID
        this.text = null
    }

    constructor(messageID: Long, text: String?) {
        id = messageID
        this.text = text
    }

    override fun toJson(): JsonMessage {
        return JsonMessage("reply", mapOf("id" to id))
    }

    override fun toString(): String {
        return "[reply:${id}]"
    }
}
