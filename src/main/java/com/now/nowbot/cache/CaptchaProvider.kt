package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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

    /**
     * 验证码已经被使用过时，返回它当时对应的 userID。
     * 用于区分「重复操作」和「验证码错误」。
     */
    fun getUsedCaptchaUserID(code: String): Long? {
        return cacheManager.getUsedUserID(code)
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
        .removalListener<String, Long> { _, userID, _ ->
            userID?.let { indexCache.remove(it) }
        }
        .build()

    // 已经使用过的验证码，保留一段时间，用于识别重复输入
    private val usedCache: Cache<String, Long> = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build()

    fun save(code: String, userID: Long) {
        captchaCache.put(code, userID)
        indexCache[userID] = code
    }

    fun getAndRemove(code: String): Long? {
        return captchaCache.getIfPresent(code)?.also {
            // 先记录为已使用，再让验证码失效，方便后续区分「重复操作」
            usedCache.put(code, it)
            captchaCache.invalidate(code)
            indexCache.remove(it)
        }
    }

    fun getUsedUserID(code: String): Long? {
        return usedCache.getIfPresent(code)
    }

    fun removeByUserID(userId: Long) {
        indexCache.remove(userId)?.let { captchaCache.invalidate(it) }
    }

    fun exists(code: String): Boolean {
        return captchaCache.getIfPresent(code) != null || usedCache.getIfPresent(code) != null
    }
}