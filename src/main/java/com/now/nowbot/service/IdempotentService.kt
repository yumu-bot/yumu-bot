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
        private const val EXPIRE_TIME_SECONDS = 60L
        private const val MAX_CACHE_SIZE = 100_000L
    }

    // 内部状态枚举，复用单例，零 GC 开销
    private enum class State {
        PROCESSING, // 正在处理中（阻断并发）
        DONE        // 已处理完成（阻断重试）
    }

    private val stateCache: Cache<String, State> = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_TIME_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MAX_CACHE_SIZE)
        .build()

    /**
     * 极速防穿透幂等控制
     * 哪怕并发请求到达间隔仅有 10us，也能确保只有一个线程执行 action()
     */
    fun <T> executeIdempotent(messageID: String, action: () -> T): Boolean {
        // 利用线程本地栈变量隔离状态
        var isFirstIn = false

        val currentState = stateCache.get(messageID) {
            isFirstIn = true
            State.PROCESSING // 将状态初始化为处理中
        }

        if (!isFirstIn) {
            if (currentState == State.DONE) {
                log.debug("消息 [{}] 之前已处理完成，直接阻断", messageID)
            } else {
                log.debug("消息 [{}] 并发冲突(已有线程抢占)，快速失败", messageID)
            }
            return false
        }

        // 抢占成功的线程执行核心业务
        return try {
            action()

            // 业务执行成功，状态变更为 DONE
            stateCache.put(messageID, State.DONE)
            true
        } catch (e: Exception) {
            log.error("消息 [$messageID] 处理异常，释放锁允许下一次重试", e)

            // 业务抛错，清理缓存槽位，给后续重试留出机会
            stateCache.invalidate(messageID)
            false
        }
    }
}