package com.now.nowbot.image.resource

import org.springframework.http.MediaType
import java.time.Instant

data class RenderResource(
    val id: String,
    val url: String,
    val mediaType: MediaType,
    val size: Int,
    /**
     * lease 内资源在 lease 关闭前为 [Instant.MAX]，关闭后才开始计算 TTL。
     */
    val expiresAt: Instant,
)

class RenderResourceContent(
    val bytes: ByteArray,
    val mediaType: MediaType,
    val expiresAt: Instant,
)
