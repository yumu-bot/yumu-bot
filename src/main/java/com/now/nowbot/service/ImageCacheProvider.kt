package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Service
// 用 Caffeine 作缓存，不自己维护 Map
class ImageCacheProvider {
    // 定义一个 Caffeine 缓存实例
    private val imageCache: Cache<String, ByteArray> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumWeight(10 * 1024 * 1024)
        .weigher { _: String, value: ByteArray -> value.size }
        .softValues() // 别 OOM 了
        .build()

    fun getImage(name: String, fetch: () -> ByteArray?): ByteArray? {
        val result = imageCache.get(name) {
            fetch() ?: NOT_FOUND
        }

        return if (result === NOT_FOUND) null else result
    }

    fun removeFromCache(name: String) = imageCache.invalidate(name)

    fun clearCache() = imageCache.invalidateAll()

    fun getCacheInfo(): ConcurrentMap<String, ByteArray> = imageCache.asMap()

    companion object {
        private val NOT_FOUND = ByteArray(0)
    }
}

/*
@Service
class ImageCacheProvider {
    private val imageCache = mutableMapOf<String, Pair<LocalDateTime, ByteArray>>()

    /**
     * 可缓存图片的图片提供者
     * @param expired 过期时间
     */
    fun getImage(
        name: String,
        expired: Duration = 1.toDuration(DurationUnit.DAYS),
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
    fun clearExpiredCache(expired: Duration = 1.toDuration(DurationUnit.DAYS)) {
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

 */