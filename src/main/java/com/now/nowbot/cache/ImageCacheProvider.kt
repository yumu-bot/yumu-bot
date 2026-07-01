package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.now.nowbot.util.MB
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Component
class ImageCacheProvider {
    private val imageCache: Cache<String, ByteArray> = Caffeine.newBuilder()
        .expireAfterWrite(7, TimeUnit.DAYS)
        .maximumWeight(12.MB.bytes)
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