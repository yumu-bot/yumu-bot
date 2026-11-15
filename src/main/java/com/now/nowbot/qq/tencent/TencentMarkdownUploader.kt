package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.KeyboardMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage
import com.now.nowbot.service.qqImageApiService.QQImageFormat
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.yumu.qq.bot.OpenAPI
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration.Companion.seconds

class TencentMarkdownUploader(private val api: OpenAPI) {
    suspend fun upload(chain: MessageChain): String {
        require(chain.messageList.all { it is TextMessage || it is ImageMessage || it is KeyboardMessage }) {
            "不支持的腾讯消息类型"
        }
        val uploadedIds = mutableListOf<String>()
        try {
            return buildString {
                for (part in chain.messageList) {
                    when (part) {
                        is TextMessage -> append(replaceBindingPlaceholder(part.message))
                        is ImageMessage -> {
                            val bytes = readBytes(part)
                            val type = requireNotNull(QQImageFormat.detect(bytes)) { "不支持的图片格式" }
                            val upload = api.prepareSendImage(type.fileName, bytes, type.contentType)
                            val etag = api.uploadImage(upload.presignedUrl, bytes, type.contentType)
                            val image = api.completeUploadImage(upload.uploadId, etag)
                            uploadedIds += image.imageId
                            require(image.url.isNotBlank()) { "图片素材未返回 URL" }
                            val dimensions = QQImageFormat.size(bytes)?.let {
                                " #${it.first}px #${it.second}px"
                            }.orEmpty()
                            append("\n\n![图片$dimensions](${image.url})\n\n")
                        }
                    }
                }
            }
        } finally {
            scheduleCleanup(uploadedIds.toList())
        }
    }

    private fun scheduleCleanup(ids: List<String>) {
        if (ids.isEmpty()) return
        cleanupScope.launch {
            // 调用方取得 Markdown 后才发送，保留平台接收及转存素材的时间。
            delay(120.seconds)
            for (id in ids.distinct()) {
                for (attempt in 0..2) {
                    try {
                        api.deleteImage(id)
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (attempt == 2) log.warn("删除腾讯图片素材失败: {}", id, e)
                        else delay(10_000L * (attempt + 1))
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024
        private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val log = LoggerFactory.getLogger(TencentMarkdownUploader::class.java)

        internal fun replaceBindingPlaceholder(text: String): String = text.replace(
            "(!bi)",
            BindException.TokenExpiredException.OfficialTokenExpired().message.orEmpty(),
        )

        internal fun source(image: ImageMessage): String? {
            val path = image.path ?: return null
            val unwrapped = path.removePrefix("file:///")
            return when {
                unwrapped.startsWith("base64://") || unwrapped.startsWith("data:") -> unwrapped
                unwrapped.startsWith("http://") || unwrapped.startsWith("https://") -> unwrapped
                unwrapped.startsWith("file:") -> Path.of(URI(unwrapped)).toUri().toString()
                image.isUrl -> path
                else -> Path.of(unwrapped).toUri().toString()
            }
        }

        internal suspend fun readBytes(image: ImageMessage): ByteArray = withContext(Dispatchers.IO) {
            val bytes = image.data ?: run {
                val source = requireNotNull(source(image)) { "图片内容为空" }
                when {
                    source.startsWith("base64://") -> Base64.getDecoder().decode(source.removePrefix("base64://"))
                    source.startsWith("data:") -> {
                        require(source.substringBefore(',').endsWith(";base64")) { "不支持的图片 data URI" }
                        Base64.getDecoder().decode(source.substringAfter(','))
                    }

                    else -> URI(source).toURL().openConnection().apply {
                        connectTimeout = 10_000
                        readTimeout = 30_000
                    }.getInputStream().use { it.readNBytes(MAX_IMAGE_SIZE + 1) }
                }
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_IMAGE_SIZE) { "图片大小必须在 1 字节至 10MB 之间" }
            bytes
        }
    }
}
