package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.now.nowbot.util.MB
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Component
class BeatmapFileCacheProvider {

    companion object {
        private const val EXPIRE_DURATION = 7L
        private const val MAX_CACHE_SIZE = 10_000L
        private val MAX_FILE_SIZE = 1.MB.bytes

        private const val NOT_FOUND = ""
    }

    private val fileCache: Cache<Long, String> = Caffeine.newBuilder()
        .expireAfterAccess(EXPIRE_DURATION, TimeUnit.DAYS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 获取或加载谱面文件文本内容
     * @param beatmapID 谱面 ID
     * @param fetchWhenAbsent 缓存未命中时的加载逻辑
     */
    fun getOrFetchString(beatmapID: Long, fetchWhenAbsent: () -> String?): String? {
        // 1. 先尝试直接从缓存拿（如果已存在，直接返回，不走逻辑）
        val cached = fileCache.getIfPresent(beatmapID)
        if (cached != null) {
            return if (cached == NOT_FOUND) null else cached
        }

        val fetched = fetchWhenAbsent()?.takeIf { it.isNotBlank() }

        if (fetched == null) {
            fileCache.put(beatmapID, NOT_FOUND)
            return null
        }

        // 3. 校验单条文件大小：
        // 如果单条文件超过上限（如 > 200KB），直接返回给业务方使用，但【不存入】Caffeine 缓存，防止挤爆 1MB 内存池
        val size = fetched.toByteArray(StandardCharsets.UTF_8).size

        if (size <= MAX_FILE_SIZE) {
            fileCache.put(beatmapID, fetched)
        }

        return fetched
    }

    fun remove(beatmapID: Long) = fileCache.invalidate(beatmapID)

    fun clear() = fileCache.invalidateAll()
}