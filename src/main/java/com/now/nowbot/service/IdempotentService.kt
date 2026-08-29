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
        private const val EXPIRE_TIME_SECONDS = 10L
        private const val MAX_CACHE_SIZE = 100_000L
    }

    private val lockCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 兼容虚拟线程的原子防抖机制
     */
    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {
        val alreadyExists = lockCache.asMap().putIfAbsent(messageID, true)

        if (alreadyExists != null) {
            //log.debug("消息 [{}] 重复或并发冲突，已被阻断", messageID)
            return false
        }

        //log.debug("消息 [{}] 进入处理：", messageID)
        try {
            action()
            return true
        } catch (e: Exception) {
            log.debug("消息 [{}] 处理异常：{}", messageID, e.message)
            // 可选策略：如果业务执行崩溃，你想让用户能再次重试，可以在这里清理缓存
            // lockCache.invalidate(messageID)
            throw e
        }
    }
}