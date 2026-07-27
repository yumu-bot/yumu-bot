package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class IdempotentService {

    private val log = LoggerFactory.getLogger(IdempotentService::class.java)

    companion object {
        private const val EXPIRE_TIME_SECONDS = 30L
        private const val MAX_CACHE_SIZE = 100_000L
    }

    // 只需要一个 Dummy 值占位即可
    private val dummy = true

    private val lockCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 严格对标 Redis SETNX 语义的单机幂等控制
     */
    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {
        val isFirst = lockCache.asMap().putIfAbsent(messageID, dummy) == null

        if (!isFirst) {
            log.debug("消息 [{}] 重复或并发冲突，已被阻断", messageID)
            return false
        }

        return try {
            action()
            true
        } catch (e: Exception) {
            log.error("消息 [$messageID] 处理异常", e)
            // 注意：千万不要在这里 invalidate(messageID)！
            // 保持 Key 在 30 秒内依然存在，强行封锁并发和短时间内的盲目重试
            false
        }
    }
}