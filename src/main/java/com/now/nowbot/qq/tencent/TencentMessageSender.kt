package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.KeyboardMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage
import com.now.nowbot.qq.tencent.TencentMarkdownUploader.Companion.readBytes
import com.now.nowbot.qq.tencent.TencentMarkdownUploader.Companion.replaceBindingPlaceholder
import com.now.nowbot.qq.tencent.TencentMarkdownUploader.Companion.source
import com.yumu.qq.bot.OpenAPI
import com.yumu.qq.message.Markdown
import com.yumu.qq.message.Media
import com.yumu.qq.message.MessageType
import com.yumu.qq.message.QQMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TencentMessageSender(
    private val api: OpenAPI,
    private val openID: String,
    private val messageID: String,
    private val isGroup: Boolean,
) {
    private val mutex = Mutex()
    private var lastSequence = 0

    suspend fun send(chain: MessageChain) = mutex.withLock {
        val message = chain.markdown?.let {
            markdownMessage(replaceBindingPlaceholder(it.markdown))
        } ?: run {
            require(chain.messageList.all { it is TextMessage || it is ImageMessage || it is KeyboardMessage }) {
                "不支持的腾讯消息类型"
            }
            val images = chain.messageList.filterIsInstance<ImageMessage>()
            val text = chain.messageList.filterIsInstance<TextMessage>().joinToString("") { it.message }
            when {
                images.isEmpty() -> {
                    if (text.isEmpty() && chain.keyboard == null) return@withLock
                    QQMessage(
                        content = replaceBindingPlaceholder(text).ifEmpty { " " },
                        messageID = messageID,
                        messageSeq = 0,
                        isGroup = isGroup,
                    )
                }

                images.size == 1 && text.isEmpty() -> ordinaryImage(images.single())
                else -> markdownMessage(TencentMarkdownUploader(api).upload(chain))
            }
        }
        message.keyboard = chain.keyboard?.keyboard
        sendMessage(message)
    }

    private suspend fun ordinaryImage(image: ImageMessage): QQMessage {
        val source = source(image)
        val media = if (image.isUrl && source != null) {
            Media.image(source)
        } else {
            Media.image(readBytes(image))
        }
        return if (isGroup) {
            api.uploadGroupMedia(openID, messageID, media)
        } else {
            api.uploadUserMedia(openID, messageID, media)
        }
    }

    private fun markdownMessage(content: String) = QQMessage(
        content = "",
        messageID = messageID,
        messageSeq = 0,
        isGroup = isGroup,
        type = MessageType.Markdown,
        markdown = Markdown(content = content),
    )

    private suspend fun sendMessage(message: QQMessage) {
        // 依赖中的序号分配不是原子的，同一 msg_id 的分配需串行。
        synchronized(QQMessage::class.java) {
            lastSequence = maxOf(lastSequence + 1, QQMessage.getKey(messageID))
            message.messageSeq = lastSequence
        }
        api.sendQQMessage(openID, message)
    }
}
