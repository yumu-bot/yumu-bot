package com.now.nowbot.qq.message

import com.now.nowbot.util.QQMsgUtil
import java.net.URL

class ImageMessage : Message {
    private enum class Type {
        FILE, URL, BYTE_ARRAY
    }

    private val type: Type
    @JvmField val path: String?
    @JvmField val data: ByteArray?

    constructor(img: ByteArray?) {
        data = img
        path = null
        type = Type.BYTE_ARRAY
    }

    constructor(path: String) {
        data = null
        this.path = "file:///$path"
        type = Type.FILE
    }

    constructor(url: URL) {
        data = null
        path = url.toExternalForm()
        type = Type.URL
    }

    val isByteArray: Boolean
        get() = type == Type.BYTE_ARRAY

    val isUrl: Boolean
        get() = type == Type.URL

    override fun toString(): String {
        return "[图片]"
    }

    override fun toJson(): JsonMessage {
        val img: Any? = if (isByteArray) {
            "base64://" + QQMsgUtil.byte2str(data)
        } else {
            path
        }
        return JsonMessage("image", mapOf("file" to img))
    }
}
