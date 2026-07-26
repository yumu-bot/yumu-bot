package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class CaptchaProvider(
    private val generator: CaptchaGenerator,
    private val cacheManager: CaptchaCacheManager
) {
    fun generateCaptcha(userID: Long): String {
        cacheManager.removeByUserID(userID)

        var code: String
        do {
            code = generator.generate()
        } while (cacheManager.exists(code)) // 需要添加exists方法

        cacheManager.save(code, userID)
        return code
    }

    fun verifyCaptcha(code: String): Long? {
        return cacheManager.getAndRemove(code)
    }
}

@Component
class CaptchaGenerator {
    fun generate(): String {
        return (100000..999999).random().toString()
    }
}

@Component
class CaptchaCacheManager {
    private val indexCache = ConcurrentHashMap<Long, String>()
    private val captchaCache: Cache<String, Long> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .removalListener { _: Any?, id: Any?, _: RemovalCause? ->
            indexCache.remove(id)
        }
        .build()

    fun save(code: String, userID: Long) {
        captchaCache.put(code, userID)
        indexCache[userID] = code
    }

    fun getAndRemove(code: String): Long? {
        return captchaCache.getIfPresent(code)?.also {
            captchaCache.invalidate(code)
            indexCache.remove(it)
        }
    }

    fun removeByUserID(userId: Long) {
        indexCache.remove(userId)?.let { captchaCache.invalidate(it) }
    }

    fun exists(code: String): Boolean {
        return captchaCache.getIfPresent(code) != null
    }
}