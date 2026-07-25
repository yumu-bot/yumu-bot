package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.now.nowbot.util.MB
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Component
class ImageCacheProvider {

    companion object {
        private const val EXPIRE_DURATION = 7L
        private const val MAX_CACHE_SIZE = 100L
        private val MAX_FILE_SIZE = 50.MB.bytes

        private val NOT_FOUND = ByteArray(0)
    }

    private val imageCache: Cache<String, ByteArray> = Caffeine.newBuilder()
        .expireAfterAccess(EXPIRE_DURATION, TimeUnit.DAYS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    fun getImage(name: String, fetch: () -> ByteArray?): ByteArray? {
        val cached = imageCache.getIfPresent(name)
        if (cached != null) {
            return if (cached === NOT_FOUND) null else cached
        }

        val fetched = fetch()

        if (fetched == null || fetched.isEmpty()) {
            imageCache.put(name, NOT_FOUND) // 防缓存穿透
            return null
        }

        if (fetched.size <= MAX_FILE_SIZE) {
            imageCache.put(name, fetched)
        }

        return fetched
    }

    fun remove(name: String) = imageCache.invalidate(name)

    fun clear() = imageCache.invalidateAll()

    fun getCacheInfo(): ConcurrentMap<String, ByteArray> = imageCache.asMap()
}