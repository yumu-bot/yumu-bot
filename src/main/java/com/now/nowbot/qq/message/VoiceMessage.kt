package com.now.nowbot.qq.message

import com.now.nowbot.util.QQMsgUtil

class VoiceMessage(val data: ByteArray) : Message() {
    override fun toString(): String {
        return "[语音]"
    }

    override fun toJson(): JsonMessage {
        return JsonMessage("record", mapOf("file" to "base64://${QQMsgUtil.byte2str(data)}"))
    }
}
