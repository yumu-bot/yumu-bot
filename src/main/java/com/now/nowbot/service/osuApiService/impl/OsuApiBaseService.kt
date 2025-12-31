package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.config.OsuConfig
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.ContextUtil
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutionException
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

@Service
class OsuApiBaseService(@Lazy private val bindDao: BindDao, @param:Qualifier("osuApiWebClient") val osuApiWebClient: WebClient, osuConfig: OsuConfig, yumuConfig: YumuConfig) {
    @JvmField final val oauthId: Int
    @JvmField final val redirectUrl: String
    @JvmField final val oauthToken: String

    private val priorityEnvironment: String = "osu-api-priority"

    init {
        var url: String
        oauthId = osuConfig.id
        if ((osuConfig.callbackUrl.also { url = it }).isEmpty()) {
            url = yumuConfig.publicUrl + osuConfig.callbackPath
        }
        redirectUrl = url
        oauthToken = osuConfig.token
    }

    private val isExpired: Boolean
        get() = System.currentTimeMillis() > time

    @PostConstruct fun init() {
        Thread.startVirtualThread {
            runTask()
        }
    }

    @Throws(ExecutionException::class)
    fun <T> request(request: (WebClient) -> Mono<T>): T {

        /*
        throw WebClientResponseException.create(403,
            "Forbidden",
            HttpHeaders.EMPTY,
            "{\"error\":\"forbidden_testing\"}".toByteArray(),
            null)

         */

        val future = CompletableFuture<T>()
        val priority = ContextUtil.getContext(
            priorityEnvironment, DEFAULT_PRIORITY,
            Int::class.java
        )
        val task = RequestTask(priority, future, request)
        TASKS.offer(task)
        try {
            return future.get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun runTask() {
        while (IocAllReadyRunner.APP_ALIVE) {
            try {
                limiter.acquire()
                val task = TASKS.take()
                Thread.startVirtualThread {
                    task.run(osuApiWebClient)
                }
            } catch (e: InterruptedException) {
                log.error("请求队列异常", e)
            }
        }
    }

    internal class RequestTask<T>(
        private var priority: Int,
        private var future: CompletableFuture<T>,
        var request: (WebClient) -> Mono<T>
    ) :
        Comparable<RequestTask<*>> {

        var time: Int = nowTime
        private var is429: AtomicBoolean = AtomicBoolean(false)
        private var retry: Short = 0

        fun run(client: WebClient) {
            request(client)
                .timeout(Duration.ofSeconds(30))
                .subscribe(
                    { value: T -> future.complete(value) },
                    { e: Throwable -> this.onError(e) }
                ) {
                    this.onComplete()
                }
        }

        private fun onError(e: Throwable) {
            if (retry >= MAX_RETRY) {
                if (!future.isDone) {
                    future.completeExceptionally(e)
                }
                return // 没返回怎么停下来啊你这 429 不是把 ppy 的老冯按地上摩擦吗

            }

            when (e) {
                is WebClientResponseException.TooManyRequests -> {
                    is429.set(true)
                    retry++

                    // 优先使用API返回的Retry-After头部
                    val waitSeconds = e.headers["Retry-After"]?.firstOrNull()?.toLongOrNull()
                        ?: calculateExponentialBackoff(retry) // 默认退避策略

                    log.info("出现 429 错误，等待 {} 秒后重试", waitSeconds)

                    Thread.startVirtualThread {
                        Thread.sleep(waitSeconds * 1000L)
                        TASKS.add(this@RequestTask)
                    }
                }

                is WebClientRequestException -> {
                    retry++
                    TASKS.add(this)
                }

                else -> {
                    future.completeExceptionally(e)
                }
            }
        }


        private fun calculateExponentialBackoff(retry: Short): Long {
            // 指数退避: 16... 最大512秒
            val baseDelay = 16L shl min(retry.toInt(), 5)
            return minOf(baseDelay, 512L)
        }

        private fun onComplete() {
            if (is429.get()) {
                log.info("请求过多, 触发退避")
                limiter.onTooManyRequests(TOO_MANY_REQUESTS_COUNT.getAndIncrement())
            } else if (TOO_MANY_REQUESTS_COUNT.get() > 0) {
                TOO_MANY_REQUESTS_COUNT.set(0)
            }
        }

        override fun compareTo(other: RequestTask<*>): Int {
            return getPriority() - other.getPriority()
        }

        private fun getPriority(): Int {
            // 用于对比, 优先级 * (一个大数 n + 时间), 这个数字大到不可能存在两个请求的时间超过 n 秒
            return (3600 * priority) + time
        }

        companion object {
            private var TOO_MANY_REQUESTS_COUNT = AtomicInteger(0)
            private val nowTime: Int
                get() = // seconds since 2025-01-01
                    (System.currentTimeMillis() / 1000 - 1735660800).toInt()
        }
    }

    internal class RateLimiter(var rate: Int, max: Int, private val minuteLimit: Int = 800) {
        var out: Int = -1
        private var semaphore: Semaphore = Semaphore(max)
        private val minuteRequests = ConcurrentLinkedDeque<Long>() // 存储最近一分钟的请求时间戳
        private val minuteLock = ReentrantLock()

        init {
            Thread.startVirtualThread { this.run() }
            // 每分钟清理一次过期记录
            Thread.startVirtualThread { this.cleanup() }
        }

        fun run() {
            while (IocAllReadyRunner.APP_ALIVE) {
                LockSupport.parkNanos(Duration.ofSeconds(1).toNanos())
                semaphore.release(rate)
            }
        }

        private fun cleanup() {
            while (IocAllReadyRunner.APP_ALIVE) {
                Thread.sleep(30000) // 每30秒清理一次
                val oneMinuteAgo = System.currentTimeMillis() - 60000
                minuteLock.lock()
                try {
                    while (minuteRequests.isNotEmpty() && minuteRequests.first < oneMinuteAgo) {
                        minuteRequests.pollFirst()
                    }
                } finally {
                    minuteLock.unlock()
                }
            }
        }

        @Throws(InterruptedException::class)
        fun acquire(): Boolean {
            if (out >= 0) {
                val seconds = min(2 * out + 1, 10)
                log.info("请求触发退避, 等待时间: {} 秒", seconds)
                LockSupport.parkNanos(Duration.ofSeconds(seconds.toLong()).toNanos())
                out = -1
                val permits = semaphore.availablePermits()
                if (permits > 1) semaphore.acquire(permits - 1)
            }

            // 检查分钟限制
            if (!checkMinuteLimit()) {
                return false
            }

            semaphore.acquire()
            return true
        }


        private fun checkMinuteLimit(): Boolean {
            minuteLock.lock()
            try {
                val now = System.currentTimeMillis()
                val oneMinuteAgo = now - 60000

                // 清理过期记录
                while (minuteRequests.isNotEmpty() && minuteRequests.first < oneMinuteAgo) {
                    minuteRequests.pollFirst()
                }

                // 检查是否超限
                if (minuteRequests.size >= minuteLimit) {
                    val oldestTime = minuteRequests.first
                    val waitTime = 61000 - (now - oldestTime) // 等到最早请求满1分钟后
                    log.warn("分钟请求数已达上限(${minuteLimit} / min)，等待 $waitTime ms")
                    Thread.sleep(waitTime)
                    return checkMinuteLimit() // 递归检查
                }

                minuteRequests.addLast(now)
                return true
            } finally {
                minuteLock.unlock()
            }
        }


        fun onTooManyRequests(n: Int) {
            out = n + 1
        }
    }

    val botToken: String?
        get() {
            if (!isExpired) {
                return accessToken
            }
            val body: MultiValueMap<String, String> = LinkedMultiValueMap()
            body.add("client_id", oauthId.toString())
            body.add("client_secret", oauthToken)
            body.add("grant_type", "client_credentials")
            body.add("scope", "public")

            setPriority(0)
            val s = request { client: WebClient ->
                client
                    .post()
                    .uri("https://osu.ppy.sh/oauth/token")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(body)).retrieve()
                    .bodyToMono(JsonNode::class.java)
            }
            clearPriority()

            if (s != null) {
                accessToken = s["access_token"].asText()
                time = System.currentTimeMillis() + s["expires_in"].asLong() * 1000
            } else {
                throw RuntimeException("更新 Oauth 令牌 请求失败")
            }
            return accessToken
        }

    fun refreshUserToken(user: BindUser, first: Boolean): String {
        val body = LinkedMultiValueMap<String, String>()
        body.add("client_id", oauthId.toString())
        body.add("client_secret", oauthToken)
        body.add("redirect_uri", redirectUrl)
        body.add("grant_type", if (first) "authorization_code" else "refresh_token")
        body.add(if (first) "code" else "refresh_token", user.refreshToken)
        if (!hasPriority()) {
            setPriority(1)
        }
        val s = request { client: WebClient ->
            client
                .post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(JsonNode::class.java)
        }
        clearPriority()
        val accessToken: String
        val refreshToken: String
        val time: Long
        if (s != null) {
            accessToken = s["access_token"].asText()
            user.accessToken = accessToken
            refreshToken = s["refresh_token"].asText()
            user.refreshToken = refreshToken
            time = user.setTimeToAfter(s["expires_in"].asLong() * 1000)
        } else {
            throw RuntimeException("更新 Oauth 令牌, 接口格式错误")
        }
        if (!first) {
            // 第一次更新需要在外面更新去更新数据库
            bindDao.updateToken(user.userID, accessToken, refreshToken, time)
        }
        return accessToken
    }

    fun insertHeader(headers: HttpHeaders) {
        headers.setAll(
            mapOf(
                "Authorization" to "Bearer $botToken",
                "x-api-version" to "20251027",
                "User-Agent" to "osu!"
            )
        )
    }

    fun insertHeader(headers: HttpHeaders, user: BindUser) {
        val token = if (!user.isAuthorized) {
            botToken
        } else if (user.isExpired) {
            try {
                refreshUserToken(user, false)
            } catch (e: WebClientResponseException.Unauthorized) {
                bindDao.backupBind(user.userID)
                log.info("更新令牌失败：令牌过期，退回到名称绑定：${user.userID}", e)
                botToken
            } catch (e: WebClientResponseException.Forbidden) {
                log.info("更新令牌失败：账号封禁", e)
                throw BindException.BindIllegalArgumentException.IllegalUserState()
            } catch (e: WebClientResponseException.NotFound) {
                log.info("更新令牌失败：找不到账号", e)
                throw BindException.BindIllegalArgumentException.IllegalUser()
            } catch (e: WebClientResponseException.TooManyRequests) {
                log.info("更新令牌失败：API 访问太频繁", e)
                throw BindException.BindNetworkException()
            }
        } else {
            user.accessToken
        }

        headers.setAll(
            mapOf(
                "Authorization" to "Bearer $token",
                "x-api-version" to "20251027",
                "User-Agent" to "osu!"
            )
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuApiBaseService::class.java)
        private var accessToken: String? = null
        private var time = System.currentTimeMillis()

        private const val DEFAULT_PRIORITY = 5
        private const val MAX_RETRY = 3
        private val limiter = RateLimiter(1, 20)

        private val TASKS = PriorityBlockingQueue<RequestTask<*>>()


        fun hasPriority(): Boolean {
            return ContextUtil.getContext(
                "osu-api-priority",
                Int::class.java
            ) != null
        }

        /**
         * 借助线程变量设置后续请求的优先级, 如果使用线程池, 务必在请求结束后调用 [.clearPriority] 方法
         *
         * @param priority 默认为 5, 越低越优先, 相同优先级先来后到
         */
        @JvmStatic fun setPriority(priority: Int) {
            ContextUtil.setContext("osu-api-priority", priority)
        }

        fun clearPriority() {
            ContextUtil.setContext("osu-api-priority", null)
        }
    }
}
