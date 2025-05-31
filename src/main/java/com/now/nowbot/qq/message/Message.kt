package com.now.nowbot.qq.message

import com.fasterxml.jackson.annotation.JsonInclude

open class Message {
    override fun toString(): String {
        return ""
    }

    open fun getCQ(): String {
        return ""
    }

    open fun toJson(): JsonMessage? {
        return null
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    inner class JsonMessage(var type: String?, var data: Map<String, Any>?)
}
