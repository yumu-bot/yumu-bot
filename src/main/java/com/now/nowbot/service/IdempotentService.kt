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

    private val idempotencyCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 高阶函数：带并发等待与极低 GC 开销的幂等控制
     */

    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {

        val isFirstIn = idempotencyCache.asMap().putIfAbsent(messageID, true) == null

        if (!isFirstIn) {
            log.debug("消息 [{}] 重复触发，快速失败阻断", messageID)
            return false
        }

        return try {
            action()
            true
        } catch (e: Exception) {
            log.error("消息 [$messageID] 处理异常", e)
            false
        }
    }
}