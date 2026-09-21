package com.now.nowbot.qq.message

import com.yumu.core.extensions.toJson
import com.yumu.qq.message.Keyboard

class KeyboardMessage(val keyboard: Keyboard) : Message() {
    override fun toJson(): JsonMessage? {
        return null
    }

    override fun getCQ(): String {
        return "[keyboard]"
    }

    override fun toString(): String {
        return keyboard.toJson()
    }
}