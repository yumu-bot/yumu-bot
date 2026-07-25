package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class IdempotentService {

    private val log = LoggerFactory.getLogger(IdempotentService::class.java)

    companion object {
        private const val EXPIRE_TIME_SECONDS = 30L
        private const val MAX_CACHE_SIZE = 100_000L
        private const val WAIT_TIMEOUT_MS = 2000L
    }

    // 缓存不再存状态 Enum，而是存 CompletableFuture<Boolean>
    // 这样并发线程可以直接 await，不需要 Thread.sleep 轮询
    private val futureCache: Cache<String, CompletableFuture<Boolean>> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 高阶函数：带并发等待与极低 GC 开销的幂等控制
     */
    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {
        // 1. 原子创建或获取现有的 Future (无锁并发控制)
        val newFuture = CompletableFuture<Boolean>()
        val existingFuture = futureCache.asMap().putIfAbsent(messageID, newFuture)

        // 2. 如果存在 existingFuture，说明已有其他线程在处理
        if (existingFuture != null) {
            return awaitOtherThread(messageID, existingFuture)
        }

        return try {
            action()

            newFuture.complete(true)
            true
        } catch (e: Exception) {
            log.error("消息 [$messageID] 处理异常，清除状态允许重试", e)

            // 失败：通知等待线程失败，并从缓存清理
            newFuture.complete(false)
            futureCache.invalidate(messageID)
            false
        }
    }

    /**
     * 并发线程唤醒等待：零 Thread.sleep()，零轮询 GC 压力
     */
    private fun awaitOtherThread(messageID: String, future: CompletableFuture<Boolean>): Boolean {
        return try {
            future.get(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            log.warn("消息 [{}] 等待超时，阻断并发", messageID)
            false
        } catch (_: Exception) {
            false
        }
    }
}