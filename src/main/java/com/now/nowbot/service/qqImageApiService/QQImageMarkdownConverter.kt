package com.now.nowbot.service.qqImageApiService

import com.now.nowbot.config.QQImageApiConfig
import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.KeyboardMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage
import com.now.nowbot.qq.tencent.YumuServer
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.util.Base64

/**
 * 把「文字 + 图片」的普通富文本消息转成带图的 Markdown 消息。
 *
 * 原来 [YumuServer] 只能把一条消息表达成「一段文字 + 一张图片」，
 * 现在借助 QQ 的 Markdown 能力和素材上传接口（[QQImageApiService]），
 * 可以把消息里的每张图都上传成 `qqbot-media://` 引用，再按原顺序插进 Markdown 文本里，
 * 于是图文混排、多图消息都能完整保留。
 *
 * 触发条件（任一不满足就返回 null，让 [YumuServer] 走原来的普通消息逻辑）：
 * - `yumu.qq-image.auto-markdown` 为 true；
 * - 素材接口可用（[QQImageApiService.isAvailable]）；
 * - 消息里至少有一张图片和一段文字（纯图片消息保持原样，避免影响出图命令）；
 * - 没有 Markdown / At / 语音 / 文件等 Markdown 表达不了的元素；
 * - 所有图片都能读出来并上传成功。
 *
 * 上传成功后调用 [QQImageCleanup.scheduleDelete]，延后释放 100 张素材配额。
 */
@Service
class QQImageMarkdownConverter(
    private val api: QQImageApiService,
    private val config: QQImageApiConfig,
) {
    companion object {
        private val log = LoggerFactory.getLogger(QQImageMarkdownConverter::class.java)

        /** 一条消息最多转几张图, 太多会明显拖慢发送 */
        private const val MAX_IMAGES = 9
    }

    /**
     * 转换结果。
     *
     * @param markdown 拼好的 Markdown 文本
     * @param imageIds 已上传的素材 id, 用于后续清理
     */
    data class ConvertedMarkdownMessage(val markdown: String, val imageIds: List<String>)

    @PostConstruct
    fun register() {
        YumuServer.imageMarkdownConverter = this
    }

    /**
     * 当前是否具备转换条件。
     */
    fun canConvert(): Boolean = config.autoMarkdown && api.isAvailable

    /**
     * 尝试转换, 转换不了返回 null。
     */
    fun convert(chain: MessageChain): ConvertedMarkdownMessage? {
        if (!canConvert()) return null
        if (chain.markdown != null) return null

        val messages = chain.messageList
        if (messages.any { it !is TextMessage && it !is ImageMessage && it !is KeyboardMessage }) {
            return null
        }

        val images = messages.filterIsInstance<ImageMessage>()
        if (images.isEmpty() || images.size > MAX_IMAGES) return null

        val text = messages.filterIsInstance<TextMessage>()
            .joinToString("\n") { it.message.trim() }
            .trim()
        // 纯图片消息保持原样; "(!bi)" 是绑定过期的占位符, 交给上层替换成提示
        if (text.isEmpty() || text.contains("(!bi)")) return null

        val parts = mutableListOf<String>()
        val imageIds = mutableListOf<String>()

        for (message in messages) {
            when (message) {
                is TextMessage -> {
                    val part = message.message.trim()
                    if (part.isNotEmpty()) parts += part
                }

                is ImageMessage -> {
                    val bytes = readBytes(message) ?: return null
                    val uploaded = upload(bytes) ?: return null
                    imageIds += uploaded.imageId
                    parts += markdownImage(uploaded.url, bytes)
                }

                else -> Unit
            }
        }

        if (imageIds.isEmpty()) return null

        QQImageCleanup.scheduleDelete(imageIds)
        return ConvertedMarkdownMessage(parts.joinToString("\n\n"), imageIds)
    }

    /**
     * 拼 Markdown 图片引用, 能读出尺寸时带上 `#宽px #高px`, 让客户端按比例渲染。
     */
    private fun markdownImage(url: String, bytes: ByteArray?): String {
        val size = bytes?.let { QQImageFormat.size(it) }
        return if (size == null) {
            "![图片]($url)"
        } else {
            "![图片 #${size.first}px #${size.second}px]($url)"
        }
    }

    private fun upload(bytes: ByteArray): QQImageApiService.MediaImage? {
        if (bytes.size > QQImageApiService.MAX_IMAGE_SIZE) {
            log.warn("图片大小 {} 字节超过素材上传上限, 回退到普通图文消息", bytes.size)
            return null
        }

        val type = QQImageFormat.detect(bytes) ?: run {
            log.warn("无法识别图片格式, 回退到普通图文消息")
            return null
        }

        return try {
            api.upload(bytes, type.fileName, type.contentType)
        } catch (e: Exception) {
            log.warn("Markdown 图片上传失败, 回退到普通图文消息: {}", e.message)
            null
        }
    }

    private fun readBytes(image: ImageMessage): ByteArray? {
        image.data?.let { return it }

        val path = image.path ?: return null
        return try {
            if (path.startsWith("base64://")) {
                Base64.getDecoder().decode(path.removePrefix("base64://"))
            } else {
                URI(path).toURL().openStream().use { it.readBytes() }
            }
        } catch (e: Exception) {
            log.warn("读取待上传图片失败 {}: {}", path, e.message)
            null
        }
    }

}
