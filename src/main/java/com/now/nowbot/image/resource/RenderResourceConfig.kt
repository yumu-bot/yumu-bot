package com.now.nowbot.image.resource

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.net.URI

@Validated
@Component("renderResourceConfig")
@ConfigurationProperties(prefix = "render.resource", ignoreInvalidFields = true)
class RenderResourceConfig {
    /**
     * 资源最短存活时间；实际 TTL 还会覆盖排队、浏览器渲染超时与 resultWaitGrace。
     */
    var ttlMillis: Long = 180_000L

    /**
     * 在浏览器渲染超时之外额外保留的安全余量。
     */
    var browserTimeoutMarginMillis: Long = 30_000L

    var maxBytes: Long = 128L * 1024 * 1024

    var maxResources: Long = 4096L

    var maxResourceBytes: Long = 16L * 1024 * 1024

    var cleanupIntervalMillis: Long = 30_000L

    /**
     * 留空时使用本机回环地址、实际 server.port 与 context-path。
     */
    var localBaseUrl: String = ""

    @PostConstruct
    fun validate() {
        require(ttlMillis in 1_000L..3_600_000L) {
            "ttlMillis 必须在 1000..3600000 之间: $ttlMillis"
        }
        require(browserTimeoutMarginMillis in 0L..600_000L) {
            "browserTimeoutMarginMillis 必须在 0..600000 之间: $browserTimeoutMarginMillis"
        }
        require(maxBytes > 0L) { "maxBytes 必须大于 0: $maxBytes" }
        require(maxResources in 1L..1_000_000L) {
            "maxResources 必须在 1..1000000 之间: $maxResources"
        }
        require(maxResourceBytes in 1L..maxBytes) {
            "maxResourceBytes 必须在 1..maxBytes 之间: $maxResourceBytes"
        }
        require(cleanupIntervalMillis in 1_000L..3_600_000L) {
            "cleanupIntervalMillis 必须在 1000..3600000 之间: $cleanupIntervalMillis"
        }
        val baseUrl = localBaseUrl.trim()
        if (baseUrl.isNotEmpty()) {
            val uri = runCatching { URI(baseUrl) }.getOrNull()
            val path = uri?.rawPath.orEmpty()
            val unsafePath = path.contains('%') ||
                path.contains('\\') ||
                path.contains("//") ||
                path.split('/').any { it == "." || it == ".." }
            require(
                uri != null &&
                    (uri.scheme == "http" || uri.scheme == "https") &&
                    !uri.host.isNullOrBlank() &&
                    uri.rawUserInfo == null &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null &&
                    !unsafePath
            ) {
                "localBaseUrl 必须是有效的 http(s) 绝对地址: $localBaseUrl"
            }
        }
    }
}
