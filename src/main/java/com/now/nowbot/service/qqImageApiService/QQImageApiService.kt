package com.now.nowbot.service.qqImageApiService

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.config.QQImageApiConfig
import com.now.nowbot.util.exchangeToBody
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * QQ 机器人素材上传接口。
 *
 * 用于把 Markdown 消息里要用的图片预先传到平台，拿到 `qqbot-media://image/v1/<token>` 引用。
 * 官方文档：https://bot.q.qq.com/wiki/develop/api-v2/server-inter/message/material-management.html
 *
 * 完整流程（[upload] 已经封装好）：
 * 1. [prepare] 申请上传，拿到 upload_id 和预签名 URL
 * 2. PUT 预签名 URL 直传图片二进制
 * 3. [complete] 完成上传，拿到 image_id 和 markdown 引用地址
 * 4. 在 Markdown 里写 `![说明 #宽px #高px](qqbot-media://image/v1/<token>)`
 *
 * 可用性：
 * - 配置了 [QQImageApiConfig.accessToken] 时直接把它当凭证，[isAvailable] 立刻为 true，不会去换 token；
 * - 否则启动时尝试换取一次 access_token，成功后 [isAvailable] 为 true；
 * - 凭证为空、或鉴权被拒（400/401/403、响应 code 非 0）时判定为不可用，并停止重试；
 * - 网络波动（超时、连接失败、408/429/5xx）时每 60 秒重试，最多尝试约 30 分钟。
 *
 * 注意：
 * - 单机器人最多 100 张，[delete] 可以释放配额；
 * - 图片是滑动过期，每次被消息引用顺延 90 天；
 * - 上传任务只有 4 小时有效期，预签名 URL 以其自身的 expires_in 为准；
 * - **必须在消息发送成功后再删除**：发送时平台才会转存副本，提前删除会让这条消息的引用失效。
 */
@Service
class QQImageApiService(
    private val restClient: RestClient,
    private val config: QQImageApiConfig,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(QQImageApiService::class.java)

        /** 单张图片大小上限, 官方限制 10MB */
        const val MAX_IMAGE_SIZE: Long = 10L * 1024 * 1024

        /** access_token 快到期时提前刷新的秒数 */
        private const val TOKEN_REFRESH_AHEAD_SECONDS = 60L

        /** 启动检查失败后的重试间隔（秒） */
        private const val STARTUP_RETRY_INTERVAL_SECONDS = 60L

        /** 启动检查最多尝试的次数, 约 30 分钟 */
        private const val STARTUP_MAX_ATTEMPTS = 30

        /** 这些状态码说明凭证或权限有问题, 重试也没用 */
        private val PERMANENT_AUTH_STATUS = setOf(400, 401, 403)

        private val ALLOWED_CONTENT_TYPES = setOf(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            "image/gif",
            "image/webp",
        )

        private val CONTENT_TYPE_BY_EXTENSION = mapOf(
            "png" to MediaType.IMAGE_PNG_VALUE,
            "jpg" to MediaType.IMAGE_JPEG_VALUE,
            "jpeg" to MediaType.IMAGE_JPEG_VALUE,
            "gif" to "image/gif",
            "webp" to "image/webp",
        )
    }

    /**
     * 凭证被拒绝（而不是网络问题）时抛出，启动检查收到它就不再重试。
     */
    class QQImageAuthException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

    /**
     * 素材接口当前是否可用。调用方据此决定要不要把图片塞进 Markdown。
     */
    @Volatile
    var isAvailable: Boolean = false
        private set

    @Volatile
    private var accessToken: String? = null

    /** access_token 过期时间, 单位秒 */
    @Volatile
    private var tokenExpiresAt: Long = 0

    private val tokenLock = Any()

    private val startupScheduler = Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().name("qq-image-bootstrap-", 0).factory()
    )

    @PostConstruct
    fun init() {
        QQImageCleanup.bind(this)

        if (config.hasFixedToken()) {
            isAvailable = true
            log.info("QQ 图片素材服务使用配置文件中的 access_token, 跳过换取流程")
            return
        }

        if (!config.isConfigured()) {
            log.warn("未配置 QQ 机器人凭证, 带图 Markdown 功能不可用; 可填写 yumu.qq-image.token 或 app-id / client-secret")
            return
        }

        checkAtStartup(0)
    }

    /**
     * 启动时换取一次 token，失败按错误类型决定重试还是放弃。
     */
    private fun checkAtStartup(attempt: Int) {
        val delay = if (attempt == 0) 0L else STARTUP_RETRY_INTERVAL_SECONDS

        startupScheduler.schedule({
            try {
                accessToken()
                isAvailable = true
                log.info("QQ 图片素材服务已就绪")
            } catch (e: QQImageAuthException) {
                isAvailable = false
                log.error("QQ 凭证被拒绝, 图片素材服务不可用, 不再重试: {}", e.message)
            } catch (e: Exception) {
                if (attempt + 1 >= STARTUP_MAX_ATTEMPTS) {
                    isAvailable = false
                    log.error("QQ 图片素材服务启动检查连续失败 {} 次, 停止重试: {}", STARTUP_MAX_ATTEMPTS, e.message)
                } else {
                    log.warn(
                        "QQ 图片素材服务启动检查失败 (第 {}/{} 次), {} 秒后重试: {}",
                        attempt + 1, STARTUP_MAX_ATTEMPTS, STARTUP_RETRY_INTERVAL_SECONDS, e.message
                    )
                    checkAtStartup(attempt + 1)
                }
            }
        }, delay, TimeUnit.SECONDS)
    }

    /**
     * 获取 access_token, 带进程内缓存。
     *
     * 配置了固定 token 时直接返回它；否则调用 [QQImageApiConfig.tokenUrl] 换取。
     * 官方在过期前 60 秒内再次获取会返回新 token, 老 token 在这 60 秒内仍有效,
     * 所以提前 [TOKEN_REFRESH_AHEAD_SECONDS] 秒刷新即可。
     */
    fun accessToken(): String {
        val fixed = config.accessToken
        if (fixed.isNotBlank()) return fixed

        requireConfigured()

        val now = System.currentTimeMillis() / 1000
        val cached = accessToken
        if (cached != null && now < tokenExpiresAt) return cached

        synchronized(tokenLock) {
            val doubleChecked = accessToken
            if (doubleChecked != null && now < tokenExpiresAt) return doubleChecked

            val response = try {
                restClient.post()
                    .uri(config.tokenUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AccessTokenRequest(config.appId, config.clientSecret))
                    .exchangeToBody<AccessTokenResponse>()
            } catch (e: RestClientResponseException) {
                val message = "获取 QQ access_token 失败: HTTP ${e.statusCode.value()} ${e.responseBodyAsString}"
                if (e.statusCode.value() in PERMANENT_AUTH_STATUS) {
                    throw QQImageAuthException(message, e)
                }
                throw IllegalStateException(message, e)
            }

            val token = response.accessToken
            if (token.isNullOrBlank()) {
                // 该接口失败时 HTTP 仍是 200, 错误在响应体的 code 里
                val message = "获取 QQ access_token 失败: code=${response.code}, message=${response.message}"
                if (response.code != null && response.code != 0) {
                    throw QQImageAuthException(message)
                }
                throw IllegalStateException(message)
            }

            val expiresIn = response.expiresIn?.toLongOrNull() ?: 7200L
            accessToken = token
            tokenExpiresAt = now + (expiresIn - TOKEN_REFRESH_AHEAD_SECONDS).coerceAtLeast(0L)
            log.debug("QQ access_token 已刷新, {} 秒后过期", expiresIn)
            return token
        }
    }

    /**
     * 上传一张图片, 返回 Markdown 可以引用的 [MediaImage]。
     *
     * @param image 图片二进制
     * @param fileName 文件名（含扩展名）, 扩展名需要与 [contentType] 一致
     */
    fun upload(
        image: ByteArray,
        fileName: String = "image.png",
        contentType: String = contentTypeOf(fileName),
    ): MediaImage {
        requireConfigured()
        requireAvailable()
        requireImage(image, fileName, contentType)

        val ticket = prepare(fileName, image, contentType)
        uploadToPresignedUrl(ticket.presignedUrl, image, contentType)
        // 预签名 PUT 返回的 ETag 就是 md5, 直接复用 prepare 时算好的值即可
        return complete(ticket.uploadId, md5Hex(image))
    }

    /**
     * 1. 申请图片上传。
     */
    fun prepare(
        fileName: String,
        image: ByteArray,
        contentType: String = contentTypeOf(fileName),
    ): UploadTicket {
        requireConfigured()
        requireAvailable()
        requireImage(image, fileName, contentType)

        val response = restClient.post()
            .uri("${config.apiUrl}/v2/media-images/prepare")
            .header(HttpHeaders.AUTHORIZATION, "QQBot ${accessToken()}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                PrepareRequest(
                    fileName = fileName,
                    fileSize = image.size.toLong(),
                    contentType = contentType,
                    md5 = md5Hex(image),
                    sha1 = sha1Hex(image),
                )
            )
            .exchangeToBody<PrepareResponse>()

        return UploadTicket(
            uploadId = response.uploadId,
            presignedUrl = response.presignedUrl,
            expiresIn = response.expiresIn ?: 0L,
        )
    }

    /**
     * 2. 用预签名 URL 直传图片二进制, 返回 COS 的 ETag（文件 MD5）。
     *
     * Content-Type 必须与 [prepare] 时声明的一致, 否则 COS 返回 403。
     */
    fun uploadToPresignedUrl(presignedUrl: String, image: ByteArray, contentType: String): String {
        return restClient.put()
            .uri(presignedUrl)
            .contentType(MediaType.parseMediaType(contentType))
            .body(image)
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful) {
                    throw HttpClientErrorException(
                        response.statusCode,
                        response.statusText,
                        response.body.readAllBytes(),
                        Charsets.UTF_8
                    )
                }
                response.headers.getFirst(HttpHeaders.ETAG)?.trim('"')
                    ?: throw IllegalStateException("预签名上传未返回 ETag, 无法完成上传")
            }
    }

    /**
     * 3. 完成上传, 拿到图片引用。
     *
     * @param md5 上传返回的 ETag（32 位十六进制）, 服务端会与 COS 对象实际的 ETag 比对
     */
    fun complete(uploadId: String, md5: String): MediaImage {
        requireConfigured()
        requireAvailable()

        val response = restClient.post()
            .uri("${config.apiUrl}/v2/media-images/complete")
            .header(HttpHeaders.AUTHORIZATION, "QQBot ${accessToken()}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(CompleteRequest(uploadId = uploadId, md5 = md5))
            .exchangeToBody<CompleteResponse>()

        log.debug("QQ 图片上传完成: imageId={}, url={}", response.imageId, response.url)

        return MediaImage(
            imageId = response.imageId,
            url = response.url,
            ttl = response.ttl,
            previewUrl = response.previewUrl,
        )
    }

    /**
     * 查询当前机器人的有效图片, 按上传时间倒序。
     *
     * @param cursor 上一页返回的 nextCursor, 首页传 null
     */
    fun list(cursor: String? = null): MediaImagePage {
        requireConfigured()
        requireAvailable()

        val uri = if (cursor.isNullOrBlank()) {
            URI.create("${config.apiUrl}/v2/media-images")
        } else {
            UriComponentsBuilder.fromUriString("${config.apiUrl}/v2/media-images")
                .queryParam("cursor", cursor)
                .build()
                .encode()
                .toUri()
        }

        val response = restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "QQBot ${accessToken()}")
            .exchangeToBody<QueryResponse>()

        return MediaImagePage(
            images = response.images.map {
                MediaImageItem(
                    imageId = it.imageId,
                    url = it.url,
                    fileName = it.fileName,
                    previewUrl = it.previewUrl,
                    createdAt = it.createdAt,
                    expiresAt = it.expiresAt,
                )
            },
            nextCursor = response.nextCursor,
            total = response.total ?: response.images.size.toLong(),
        )
    }

    /**
     * 删除图片, 释放 100 张的数量配额。
     *
     * 删除后引用链接立即失效, **已经发出的消息不受影响**（发送时平台已转存副本）。
     */
    fun delete(imageId: String) {
        requireConfigured()

        restClient.delete()
            .uri("${config.apiUrl}/v2/media-images/$imageId/delete")
            .header(HttpHeaders.AUTHORIZATION, "QQBot ${accessToken()}")
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful) {
                    throw HttpClientErrorException(
                        response.statusCode,
                        response.statusText,
                        response.body.readAllBytes(),
                        Charsets.UTF_8
                    )
                }
            }
    }

    /**
     * 判断传输异常是不是凭证问题（区别于网络波动）。
     */
    fun isAuthFailure(t: Throwable): Boolean {
        var cause: Throwable? = t
        var depth = 0
        while (cause != null && depth < 10) {
            if (cause is QQImageAuthException) return true
            if (cause is HttpClientErrorException && cause.statusCode.value() in PERMANENT_AUTH_STATUS) return true
            cause = cause.cause
            depth++
        }
        return false
    }

    private fun requireConfigured() {
        check(config.isConfigured()) {
            "未配置 QQ 机器人凭证, 请填写 yumu.qq-image.token 或 yumu.qq-image.app-id / yumu.qq-image.client-secret"
        }
    }

    private fun requireAvailable() {
        check(isAvailable) {
            "QQ 图片素材服务尚未就绪, 暂时不能上传图片"
        }
    }

    private fun requireImage(image: ByteArray, fileName: String, contentType: String) {
        require(image.isNotEmpty()) { "图片内容为空" }
        require(image.size <= MAX_IMAGE_SIZE) {
            "图片大小 ${image.size} 字节超过上限 ${MAX_IMAGE_SIZE} 字节（10MB）"
        }
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "不支持的图片类型: $contentType, 仅支持 png/jpeg/gif/webp"
        }
        require(contentTypeOf(fileName) == contentType) {
            "文件扩展名与 contentType 不一致: $fileName -> $contentType"
        }
    }

    private fun contentTypeOf(fileName: String): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return CONTENT_TYPE_BY_EXTENSION[extension]
            ?: throw IllegalArgumentException("不支持的图片扩展名: $fileName")
    }

    private fun md5Hex(bytes: ByteArray): String = digestHex("MD5", bytes)

    private fun sha1Hex(bytes: ByteArray): String = digestHex("SHA1", bytes)

    private fun digestHex(algorithm: String, bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes))

    /**
     * prepare 返回的上传任务。
     */
    data class UploadTicket(
        @field:JsonProperty("upload_id") val uploadId: String,
        @field:JsonProperty("presigned_url") val presignedUrl: String,
        @field:JsonProperty("expires_in") val expiresIn: Long = 0,
    )

    /**
     * complete 返回的图片引用。
     */
    data class MediaImage(
        @field:JsonProperty("image_id") val imageId: String,
        @field:JsonProperty("url") val url: String,
        @field:JsonProperty("ttl") val ttl: Long = 0,
        @field:JsonProperty("preview_url") val previewUrl: String? = null,
    )

    /**
     * 图片列表中的一项。
     */
    data class MediaImageItem(
        @field:JsonProperty("image_id") val imageId: String,
        @field:JsonProperty("url") val url: String,
        @field:JsonProperty("file_name") val fileName: String? = null,
        @field:JsonProperty("preview_url") val previewUrl: String? = null,
        @field:JsonProperty("created_at") val createdAt: Long = 0,
        @field:JsonProperty("expires_at") val expiresAt: Long = 0,
    )

    /**
     * 图片列表的一页。
     */
    data class MediaImagePage(
        val images: List<MediaImageItem> = emptyList(),
        @field:JsonProperty("next_cursor") val nextCursor: String? = null,
        val total: Long = 0,
    )

    private data class AccessTokenRequest(
        @field:JsonProperty("appId") val appId: String,
        @field:JsonProperty("clientSecret") val clientSecret: String,
    )

    private data class AccessTokenResponse(
        @field:JsonProperty("access_token") val accessToken: String? = null,
        @field:JsonProperty("expires_in") val expiresIn: String? = null,
        val code: Int? = null,
        val message: String? = null,
    )

    private data class PrepareRequest(
        @field:JsonProperty("file_name") val fileName: String,
        @field:JsonProperty("file_size") val fileSize: Long,
        @field:JsonProperty("content_type") val contentType: String,
        val md5: String,
        val sha1: String,
    )

    private data class PrepareResponse(
        @field:JsonProperty("upload_id") val uploadId: String,
        @field:JsonProperty("presigned_url") val presignedUrl: String,
        @field:JsonProperty("expires_in") val expiresIn: Long? = null,
    )

    private data class CompleteRequest(
        @field:JsonProperty("upload_id") val uploadId: String,
        val md5: String,
    )

    private data class CompleteResponse(
        @field:JsonProperty("image_id") val imageId: String,
        val url: String,
        val ttl: Long = 0,
        @field:JsonProperty("preview_url") val previewUrl: String? = null,
    )

    private data class QueryResponse(
        val images: List<MediaImageItem> = emptyList(),
        @field:JsonProperty("next_cursor") val nextCursor: String? = null,
        val total: Long? = null,
    )
}
