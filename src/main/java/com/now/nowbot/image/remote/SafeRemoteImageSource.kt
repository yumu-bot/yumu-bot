package com.now.nowbot.image.remote

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.resource.RenderResourceMediaTypes
import okhttp3.HttpUrl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.Locale
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@Service
class SafeRemoteImageSource internal constructor(
    private val config: RemoteImageConfig,
    private val transport: RemoteImageTransport,
    ticker: Ticker,
    private val nanoTime: () -> Long,
    private val permits: Semaphore,
) : RemoteImageSource {
    @Autowired
    constructor(config: RemoteImageConfig) : this(
        config,
        OkHttpRemoteImageTransport(config),
        Ticker.systemTicker(),
        { System.nanoTime() },
        Semaphore(config.maxConcurrentRequests),
    )

    private val urlPolicy = RemoteImageUrlPolicy(config)

    private val cache: Cache<String, ByteArray> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(config.cacheTtlMillis))
        .maximumWeight(config.maxCacheBytes)
        .weigher<String, ByteArray> { key, value -> cacheWeight(key, value) }
        .ticker(ticker)
        .executor { command -> command.run() }
        .build()

    override fun read(url: String): ByteArray? {
        val target = urlPolicy.parse(url) ?: return null
        val key = target.toString()
        cachedCopy(key)?.let { return it }

        if (!acquirePermit()) return null
        try {
            cachedCopy(key)?.let { return it }
            val bytes = cache.get(key) { _ -> load(target) ?: NOT_FOUND } ?: return null
            if (bytes === NOT_FOUND) {
                // Caffeine 不缓存 null；用哨兵避免负缓存，并在返回前按值移除。
                cache.asMap().remove(key, NOT_FOUND)
                return null
            }
            return bytes.copyOf()
        } finally {
            permits.release()
        }
    }

    private fun cacheWeight(key: String, value: ByteArray): Int {
        // 长 URL 与 entry 本身也要占内存，按字节权重时一并计入，避免小图绕过上限。
        val weight = value.size.toLong() + key.length.toLong() * 2L + CACHE_ENTRY_OVERHEAD_BYTES
        return weight.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
    }

    private fun cachedCopy(key: String): ByteArray? {
        val value = cache.getIfPresent(key) ?: return null
        if (value === NOT_FOUND) {
            cache.asMap().remove(key, NOT_FOUND)
            return null
        }
        return value.copyOf()
    }

    @Scheduled(fixedDelayString = "\${image.remote.cleanup-interval-millis:60000}")
    fun cleanUp() {
        cache.cleanUp()
    }

    private fun acquirePermit(): Boolean = try {
        permits.tryAcquire(config.acquireTimeoutMillis, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }

    private fun load(start: HttpUrl): ByteArray? {
        val deadline = nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.callTimeoutMillis)
        var current = start
        var redirects = 0

        while (true) {
            val remaining = deadline - nanoTime()
            if (remaining <= 0L) return null
            val response = try {
                transport.execute(current, TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1L))
            } catch (e: IOException) {
                logFailure(current, e)
                return null
            }

            if (response.statusCode in 300..399) {
                val next = try {
                    response.use { redirect ->
                        if (redirects >= config.maxRedirects) return null
                        val location = redirect.location?.takeIf { it.isNotBlank() } ?: return null
                        urlPolicy.resolve(current, location) ?: return null
                    }
                } catch (e: IOException) {
                    logFailure(current, e)
                    return null
                }
                current = next
                redirects++
                continue
            }

            return try {
                response.use { image ->
                    if (image.statusCode in 200..299) readImage(image) else null
                }
            } catch (e: IOException) {
                logFailure(current, e)
                null
            }
        }
    }

    private fun readImage(response: RemoteImageResponse): ByteArray? {
        if (!isAllowedContentType(response.contentType)) return null
        if (response.contentLength > config.maxImageBytes) return null
        val body = response.body ?: return null
        val bytes = readLimited(body, config.maxImageBytes) ?: return null
        if (bytes.isEmpty()) return null

        val mediaType = RenderResourceMediaTypes.detect(bytes) ?: return null
        if (mediaType.subtype.equals("svg+xml", ignoreCase = true)) return null
        return bytes
    }

    private fun readLimited(body: InputStream, limit: Long): ByteArray? {
        val initialSize = minOf(limit, 64L * 1024L).toInt().coerceAtLeast(0)
        val output = ByteArrayOutputStream(initialSize)
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = body.read(buffer)
            if (read == -1) break
            if (read == 0) continue
            total += read
            if (total > limit) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun isAllowedContentType(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val mediaType = try {
            MediaType.parseMediaType(value)
        } catch (e: IllegalArgumentException) {
            return false
        }
        val type = mediaType.type.lowercase(Locale.ROOT)
        val subtype = mediaType.subtype.lowercase(Locale.ROOT)
        if (type == "*" || subtype.isBlank() || subtype == "*") return false
        if (type == "image" && subtype == "svg+xml") return false
        return type == "image" ||
            (type == "application" && subtype == "octet-stream") ||
            (type == "binary" && subtype == "octet-stream")
    }

    private fun logFailure(url: HttpUrl, error: IOException) {
        log.debug("远程图片请求失败: host={}, error={}", url.host, error.javaClass.simpleName)
    }

    private companion object {
        private const val CACHE_ENTRY_OVERHEAD_BYTES = 128L
        private val log = LoggerFactory.getLogger(SafeRemoteImageSource::class.java)
        private val NOT_FOUND = ByteArray(0)
    }
}
