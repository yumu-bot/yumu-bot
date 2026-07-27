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

    // 使用 Boolean 作为返回值占位
    private val lockCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 采用 Caffeine 原生键级独占锁机制，绝对防穿透
     */
    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {
        var executed = false

        log.debug("消息 [{}] 进入：", messageID)

        // Caffeine 的 get 方法针对单个 Key 内部加锁：
        // 1. 如果 key 不存在，会进入 lambda，此时其他并发线程会被挂起或直接返回
        // 2. 如果 key 已存在，则直接返回已有值，不会重复进入 lambda
        lockCache.get(messageID) {
            executed = true
            try {
                action()
            } catch (e: Exception) {
                log.debug("消息 [{}] 处理异常：{}", messageID, e.message)
                throw e
            }
            true // 存入缓存的值
        }

        if (!executed) {
            log.debug("消息 [{}] 重复或并发冲突，已被 Caffeine 独占锁阻断", messageID)
            return false
        }

        return true
    }
}