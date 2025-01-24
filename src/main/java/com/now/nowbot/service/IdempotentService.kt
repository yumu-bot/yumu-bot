package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class IdempotentService(
    private val messageRedis: Optional<RedisTemplate<String, String>>,
) {
    private lateinit var redis: RedisTemplate<String, String>
    private lateinit var localCache: Cache<String, Unit>
    private var isRedis: Boolean = false
    private val EXPIRE_TIME: Long = 30

    @PostConstruct
    fun init() {
        if (messageRedis.isPresent) {
            redis = messageRedis.get()
            isRedis = true
        } else {
            localCache = Caffeine.newBuilder()
                .expireAfterWrite(EXPIRE_TIME, TimeUnit.SECONDS)
                .build()
        }
    }

    fun checkByMessageId(messageId: String): Boolean {
        return if (isRedis) {
            redis
                .opsForValue()
                .setIfAbsent(messageId, "", EXPIRE_TIME, TimeUnit.SECONDS)
                ?: false

        } else {
            return localCache.asMap().putIfAbsent(messageId, Unit) == null
        }
    }
}