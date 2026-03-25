package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Service
class ImageCacheProvider {
    private val imageCache: Cache<String, ByteArray> = Caffeine.newBuilder()
        .expireAfterWrite(7, TimeUnit.DAYS)
        .maximumWeight(12 * 1024 * 1024)
        .weigher { _: String, value: ByteArray -> value.size }
        // .softValues() // 留着吧
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