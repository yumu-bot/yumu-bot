package com.now.nowbot.service

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class ImageCacheProvider {
    private val imageCache = mutableMapOf<String, Pair<LocalDateTime, ByteArray>>()

    /**
     * 可缓存图片的图片提供者
     * @param expired 过期时间，一般设为 7 天
     */
    fun getImage(
        name: String,
        expired: Duration = 7.toDuration(DurationUnit.DAYS),
        fetch: () -> ByteArray?,
    ): ByteArray? {
        val now = LocalDateTime.now()

        // 先快速检查（不加锁）
        checkAndClearExpired(name, now, expired)
        imageCache[name]?.let { return it.second }

        // 缓存中没有，获取
        val image = fetch()

        // 存入缓存
        image?.let { imageCache[name] = now to it }

        return image
    }

    private fun checkAndClearExpired(name: String, now: LocalDateTime, expired: Duration) {
        val cache = imageCache[name]
        if (cache != null && cache.first.plusSeconds(expired.inWholeSeconds).isBefore(now)) {
            removeFromCache(name)
        }
    }

    // 清理所有过期缓存
    fun clearExpiredCache(expired: Duration = 7.toDuration(DurationUnit.DAYS)) {
        val now = LocalDateTime.now()
        val iterator = imageCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()

            if (entry.value.first.plusSeconds(expired.inWholeSeconds).isBefore(now)) {
                iterator.remove()
            }
        }
    }

    // 清理所有缓存
    fun clearCache() {
        imageCache.clear()
    }

    // 移除特定图片缓存
    fun removeFromCache(name: String) {
        imageCache.remove(name)
    }

    // 获取缓存信息（用于调试）
    fun getCacheInfo(): Map<String, LocalDateTime> {
        return imageCache.mapValues { it.value.first }
    }
}