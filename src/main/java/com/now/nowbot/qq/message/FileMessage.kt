package com.now.nowbot.qq.message

/**
 * 收到的文件消息。
 *
 * OneBot 的 file 段通常只带 file_id / file_name，url 需要额外调用
 * get_group_file_url 获取；部分客户端会直接带上 url，这里两种都兼容。
 */
class FileMessage(
    val name: String? = null,
    val url: String? = null,
    val fileID: String? = null,
    val busID: Int = 0,
    val size: Long = 0,
) : Message() {
    val isUsable: Boolean
        get() = !url.isNullOrBlank()

    override fun toString(): String {
        return if (name.isNullOrBlank()) "[文件]" else "[文件:$name]"
    }

    override fun toJson(): JsonMessage {
        val data = mutableMapOf<String, Any>("file_size" to size)

        name?.let { data["file_name"] = it }
        url?.let { data["file"] = it }
        fileID?.let { data["file_id"] = it }

        return JsonMessage("file", data)
    }
}
