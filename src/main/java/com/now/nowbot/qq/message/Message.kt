package com.now.nowbot.qq.message

import com.fasterxml.jackson.annotation.JsonInclude

open class Message {
    override fun toString(): String {
        return ""
    }

    open fun getCQ(): String {
        return ""
    }

    // 返回空会自动跳过 onebot 11 的发送, 针对 Markdown / Keyboard 消息
    open fun toJson(): JsonMessage? {
        return null
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    inner class JsonMessage(var type: String?, var data: Map<String, Any>?)
}
