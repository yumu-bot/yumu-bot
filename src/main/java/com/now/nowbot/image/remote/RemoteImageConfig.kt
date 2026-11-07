package com.now.nowbot.image.remote

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.util.Locale

@Validated
@Component
@ConfigurationProperties(prefix = "image.remote", ignoreInvalidFields = true)
class RemoteImageConfig {
    /**
     * 单张远程图片允许的最大字节数（gzip 解压后同样受限）。
     */
    var maxImageBytes: Long = 8L * 1024 * 1024

    /**
     * 最多跟随的重定向次数；0 表示不跟随。
     */
    var maxRedirects: Int = 3

    var connectTimeoutMillis: Long = 3_000L

    var readTimeoutMillis: Long = 5_000L

    /**
     * 一次读取（含 DNS、连接、重定向与响应体）的总时间预算。
     */
    var callTimeoutMillis: Long = 10_000L

    var maxConcurrentRequests: Int = 8

    /**
     * 并发数已满时等待许可的最长时间；0 表示立即失败。
     */
    var acquireTimeoutMillis: Long = 2_000L

    var cacheTtlMillis: Long = 300_000L

    var maxCacheBytes: Long = 32L * 1024 * 1024

    var cleanupIntervalMillis: Long = 60_000L

    var maxUrlLength: Int = 4096

    /**
     * 是否允许明文 HTTP。默认允许以兼容用户自定义横幅；HTTPS 跳转到 HTTP 始终拒绝。
     */
    var allowHttp: Boolean = true

    /**
     * 可选的域名白名单。留空表示允许任意公网域名；配置后同时允许条目自身及其子域名。
     */
    var allowedHosts: Set<String> = emptySet()

    @PostConstruct
    fun validate() {
        require(maxImageBytes in 1L..(64L * 1024 * 1024)) {
            "maxImageBytes 必须在 1..67108864 之间: $maxImageBytes"
        }
        require(maxRedirects in 0..5) { "maxRedirects 必须在 0..5 之间: $maxRedirects" }
        require(connectTimeoutMillis in 100L..60_000L) {
            "connectTimeoutMillis 必须在 100..60000 之间: $connectTimeoutMillis"
        }
        require(readTimeoutMillis in 100L..60_000L) {
            "readTimeoutMillis 必须在 100..60000 之间: $readTimeoutMillis"
        }
        require(callTimeoutMillis in 100L..120_000L) {
            "callTimeoutMillis 必须在 100..120000 之间: $callTimeoutMillis"
        }
        require(callTimeoutMillis >= connectTimeoutMillis) {
            "callTimeoutMillis 不能小于 connectTimeoutMillis"
        }
        require(callTimeoutMillis >= readTimeoutMillis) {
            "callTimeoutMillis 不能小于 readTimeoutMillis"
        }
        require(maxConcurrentRequests in 1..256) {
            "maxConcurrentRequests 必须在 1..256 之间: $maxConcurrentRequests"
        }
        require(acquireTimeoutMillis in 0L..30_000L) {
            "acquireTimeoutMillis 必须在 0..30000 之间: $acquireTimeoutMillis"
        }
        require(cacheTtlMillis in 1_000L..3_600_000L) {
            "cacheTtlMillis 必须在 1000..3600000 之间: $cacheTtlMillis"
        }
        require(maxCacheBytes in 1L..(256L * 1024 * 1024)) {
            "maxCacheBytes 必须在 1..268435456 之间: $maxCacheBytes"
        }
        require(cleanupIntervalMillis in 1_000L..3_600_000L) {
            "cleanupIntervalMillis 必须在 1000..3600000 之间: $cleanupIntervalMillis"
        }
        require(maxUrlLength in 64..16_384) {
            "maxUrlLength 必须在 64..16384 之间: $maxUrlLength"
        }

        val normalized = allowedHosts.map { it.trim().lowercase(Locale.ROOT) }
        normalized.forEach { host ->
            require(RemoteImageAddresses.isValidHost(host)) {
                "allowedHosts 必须是公网域名或 IP（不支持协议、端口或通配符）: $host"
            }
        }
        allowedHosts = normalized.toSet()
    }
}
