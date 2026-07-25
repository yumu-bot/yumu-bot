package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BeatmapPathCacheProvider {

    companion object {
        private const val EXPIRE_DURATION = 7L
        private const val MAX_CACHE_SIZE = 100_000L

        private const val NOT_FOUND = false
    }

    private val pathCache: Cache<Long, Boolean> = Caffeine.newBuilder()
        .expireAfterAccess(EXPIRE_DURATION, TimeUnit.DAYS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    fun getOrPreparePath(beatmapID: Long, fetchWhenAbsent: () -> Boolean?): Boolean? {
        val result = pathCache.get(beatmapID) {
            fetchWhenAbsent() ?: NOT_FOUND
        }

        return if (!result) null else true
    }

    fun put(beatmapID: Long) {
        pathCache.put(beatmapID, true)
    }

    fun remove(beatmapID: Long) = pathCache.invalidate(beatmapID)

    fun clear() = pathCache.invalidateAll()
}