package com.now.nowbot.qq.message

class MarkdownMessage(var markdown: String) : Message() {
    override fun toString(): String {
        return markdown
    }

    override fun toJson(): JsonMessage? {
        return null
    }
}