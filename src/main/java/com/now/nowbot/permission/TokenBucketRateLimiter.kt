package com.now.nowbot.permission

import com.now.nowbot.throwable.botRuntimeException.PermissionException
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TokenBucketRateLimiter(
    private val capacity: Int = 3,
    private val refill: Duration = 10.toDuration(DurationUnit.SECONDS) // 1令牌的毫秒
) {
    private val tokens = ConcurrentHashMap<String, Double>()
    private val lastRefill = ConcurrentHashMap<String, Instant>()

    /**
     * 使用一次免费次数（91 并感）。如果没令牌可用则抛出错误
     */
    @Throws(PermissionException.TokenBucketException.OutOfToken::class)
    fun checkOrThrow(service: String, userID: Long? = null) {
        val tokenKey = if (userID != null) {
            "${service}-${userID}"
        } else {
            service
        }

        if (!isAllowed(tokenKey)) {
            val now = Instant.now()

            val remain = java.time.Duration.between(lastRefill[tokenKey]?: now, now).toSeconds()

            val time = "${(refill.inWholeSeconds - remain).coerceAtLeast(0L)} 秒"
            throw PermissionException.TokenBucketException.OutOfToken(service, time)
        }
    }


    fun isAllowed(service: String): Boolean {
        val now = Instant.now()

        return synchronized(this) {
            val currentTokens = tokens.getOrDefault(service, capacity.toDouble())
            val lastRefillTime = lastRefill.getOrDefault(service, now)

            // 计算时间差（毫秒）
            val timePassedMillis = java.time.Duration.between(lastRefillTime, now).toMillis()
            val tokensToAdd = timePassedMillis * 1.0 / refill.inWholeMilliseconds

            // 更新令牌数，不超过容量
            val newTokens = (currentTokens + tokensToAdd).coerceAtMost(capacity.toDouble())

            if (newTokens >= 1.0) {
                // 消耗一个令牌
                tokens[service] = newTokens - 1.0
                lastRefill[service] = now
                true
            } else {
                // 令牌不足，拒绝请求
                tokens[service] = newTokens
                lastRefill[service] = lastRefillTime
                false
            }
        }
    }
}