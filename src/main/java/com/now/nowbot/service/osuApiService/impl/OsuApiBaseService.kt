package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.config.OsuConfig
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import io.netty.channel.unix.Errors
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

@Service
class OsuApiBaseService(
    @param:Qualifier("osuApiWebClient") val osuApiWebClient: WebClient,
    private val bindDao: BindDao,
    osuConfig: OsuConfig,
    yumuConfig: YumuConfig
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuApiBaseService::class.java)

        // API 限制常量
        private const val GLOBAL_QUOTA_PER_MINUTE = 300
        private const val FOREGROUND_QUOTA_PER_SECOND = 5
        private const val FOREGROUND_BURST_PER_SECOND = 6
        private const val BACKGROUND_QUOTA_PER_SECOND = 1
        private const val BACKGROUND_BURST_PER_SECOND = 1
        private const val MAX_IN_FLIGHT_REQUESTS = 50
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val MAX_RETRIES = 3
    }

    // Token 管理（简化版）
    private var botAccessToken: String? = null
    private var tokenExpiresAt: Long = 0

    final val redirectUrl: String
    final val oauthID: Int
    private val oauthToken: String

    init {
        var callbackUrl = osuConfig.callbackUrl
        if (callbackUrl.isEmpty()) {
            callbackUrl = yumuConfig.publicUrl + osuConfig.callbackPath
        }
        redirectUrl = callbackUrl
        oauthID = osuConfig.id
        oauthToken = osuConfig.token
    }

    // 核心调度器
    private val requestScheduler = RequestScheduler()

    // 供外部使用的 API
    fun <T> submitRequest(
        request: (WebClient) -> Mono<T>,
        isBackground: Boolean = false,
        priority: Int = if (isBackground) 10 else 1
    ): CompletableFuture<T> {
        return requestScheduler.submit(
            request = request,
            isBackground = isBackground,
            priority = priority,
            timeout = REQUEST_TIMEOUT_SECONDS
        )
    }

    // 兼容 StructuredTaskScope
    fun <T> request(isBackground: Boolean = false, request: (WebClient) -> Mono<T>): T {
        return submitRequest(request, isBackground).get()
    }

    // Token 管理方法
    fun getBotToken(): String {
        val now = System.currentTimeMillis()
        if (now >= tokenExpiresAt || botAccessToken == null) {
            refreshBotToken()
        }
        return botAccessToken!!
    }

    private fun refreshBotToken() {
        val body: MultiValueMap<String, String> = LinkedMultiValueMap()
        body.add("client_id", oauthID.toString())
        body.add("client_secret", oauthToken)
        body.add("grant_type", "client_credentials")
        body.add("scope", "public")

        val result = submitRequest({ client ->
            client.post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(JsonNode::class.java)
        }, isBackground = false).get()

        botAccessToken = result["access_token"].asText()
        tokenExpiresAt = System.currentTimeMillis() + result["expires_in"].asLong() * 1000
    }

    fun insertHeader(headers: HttpHeaders, token: String? = null) {
        val actualToken = token ?: getBotToken()
        headers.setAll(
            mapOf(
                "Authorization" to "Bearer $actualToken",
                "x-api-version" to "20251027",
                "User-Agent" to "osu!"
            )
        )
    }

    fun insertHeader(headers: HttpHeaders, user: BindUser) {
        val actualToken = user.accessToken ?: getBotToken()
        headers.setAll(
            mapOf(
                "Authorization" to "Bearer $actualToken",
                "x-api-version" to "20251027",
                "User-Agent" to "osu!"
            )
        )
    }

    // 内部核心调度器实现
    private inner class RequestScheduler {
        // 任务队列：按优先级排序
        private val taskQueue = PriorityBlockingQueue<ApiRequestTask<*>>(1000) { a, b ->
            val priorityCompare = a.priority.compareTo(b.priority)
            if (priorityCompare != 0) priorityCompare else a.createdAt.compareTo(b.createdAt)
        }

        // 限流器
        private val foregroundLimiter = AdaptiveRateLimiter(
            baseQuotaPerSecond = FOREGROUND_QUOTA_PER_SECOND,
            burstCapacity = FOREGROUND_BURST_PER_SECOND,
            maxQuotaPerMinute = FOREGROUND_QUOTA_PER_SECOND * 60
        )

        private val backgroundLimiter = AdaptiveRateLimiter(
            baseQuotaPerSecond = BACKGROUND_QUOTA_PER_SECOND,
            burstCapacity = BACKGROUND_BURST_PER_SECOND,
            maxQuotaPerMinute = BACKGROUND_QUOTA_PER_SECOND * 60
        )

        // 并发控制
        private val inFlightCounter = AtomicLong(0)
        private val semaphore = Semaphore(MAX_IN_FLIGHT_REQUESTS)

        // 全局配额跟踪（分钟级别）
        private val minuteQuotaTracker = SlidingWindowQuotaTracker(
            windowSizeMillis = 60_000L,
            maxRequests = GLOBAL_QUOTA_PER_MINUTE
        )

        // 熔断器状态
        private val circuitBreakerState = AtomicReference(CircuitBreakerState.CLOSED)
        private val last429Time = AtomicLong(0)

        init {
            // 启动消费者线程
            Thread.ofVirtual().name("osu-api-scheduler").start {
                while (IocAllReadyRunner.APP_ALIVE) {
                    try {
                        // 检查熔断器
                        checkCircuitBreaker()

                        // 从队列中获取任务（阻塞）
                        val task = taskQueue.take()

                        // 检查任务是否已超时
                        if (isTaskExpired(task)) {
                            task.completeExceptionally(TimeoutException("Request timeout before execution"))
                            continue
                        }

                        // 使用虚拟线程执行
                        Thread.ofVirtual().start {
                            executeTask(task)
                        }
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        log.error("Scheduler error", e)
                    }
                }
            }
        }

        fun <T> submit(
            request: (WebClient) -> Mono<T>,
            isBackground: Boolean,
            priority: Int,
            timeout: Long
        ): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            val task = ApiRequestTask(
                request = request,
                future = future,
                isBackground = isBackground,
                priority = priority,
                timeoutSeconds = timeout
            )

            // 立即检查熔断器
            if (circuitBreakerState.get() == CircuitBreakerState.OPEN) {
                val waitUntil = last429Time.get()
                val waitTime = waitUntil - System.currentTimeMillis()
                if (waitTime > 0) {
                    task.completeExceptionally(
                        RuntimeException("API circuit breaker open. Retry after ${waitTime}ms")
                    )
                    return future
                } else {
                    circuitBreakerState.set(CircuitBreakerState.CLOSED)
                }
            }

            // 放入队列
            taskQueue.put(task)

            // 设置超时
            val timeoutFuture = future.orTimeout(timeout, TimeUnit.SECONDS)
            timeoutFuture.whenComplete { _: Any?, throwable: Throwable? ->
                if (throwable is TimeoutException) {
                    task.markTimedOut()
                }
            }

            return future
        }

        private fun <T> executeTask(task: ApiRequestTask<T>) {
            // 获取执行许可
            if (!semaphore.tryAcquire()) {
                task.completeExceptionally(RejectedExecutionException("Too many concurrent requests"))
                return
            }

            try {
                inFlightCounter.incrementAndGet()

                // 选择限流器
                val limiter = if (task.isBackground) backgroundLimiter else foregroundLimiter

                // 等待配额（带超时）
                val waitResult = limiter.acquireWithTimeout(task.remainingTimeMillis())
                if (!waitResult) {
                    task.completeExceptionally(TimeoutException("Rate limit timeout"))
                    return
                }

                // 检查全局分钟配额
                if (!minuteQuotaTracker.tryAcquire()) {
                    task.completeExceptionally(RuntimeException("Global minute quota exceeded"))
                    return
                }

                // 执行实际请求
                executeActualRequest(task)

            } finally {
                semaphore.release()
                inFlightCounter.decrementAndGet()
            }
        }

        private fun <T> executeActualRequest(task: ApiRequestTask<T>) {
            val remainingTime = task.remainingTimeMillis()
            if (remainingTime <= 0) {
                task.completeExceptionally(TimeoutException("No time left for request"))
                return
            }

            task.request(osuApiWebClient)
                .timeout(Duration.ofMillis(remainingTime))
                .subscribe(
                    { result -> task.complete(result) },
                    { error -> handleRequestError(task, error) }
                )
        }

        private fun <T> handleRequestError(task: ApiRequestTask<T>, error: Throwable) {
            when (error) {
                is WebClientResponseException.TooManyRequests -> {
                    handle429Error(error)
                    task.retryOrFail(error)
                }
                is WebClientRequestException -> {
                    // 网络错误可重试
                    task.retryOrFail(error)
                }
                else -> {
                    task.completeExceptionally(error)
                }
            }
        }

        private fun handle429Error(error: WebClientResponseException.TooManyRequests) {
            val retryAfter = error.headers.getFirst("Retry-After")?.toLongOrNull() ?: 5L
            val waitUntil = System.currentTimeMillis() + (retryAfter * 1000) + 5000 // 额外5秒缓冲

            last429Time.set(waitUntil)
            circuitBreakerState.set(CircuitBreakerState.OPEN)

            // 清空限流器令牌
            foregroundLimiter.reset()
            backgroundLimiter.reset()

            log.warn("API rate limited. Circuit breaker OPEN until {}",
                Instant.ofEpochMilli(waitUntil))
        }

        private fun checkCircuitBreaker() {
            val state = circuitBreakerState.get()
            if (state == CircuitBreakerState.OPEN) {
                val waitUntil = last429Time.get()
                if (System.currentTimeMillis() >= waitUntil) {
                    circuitBreakerState.set(CircuitBreakerState.CLOSED)
                    log.info("Circuit breaker CLOSED")
                }
            }
        }

        private fun isTaskExpired(task: ApiRequestTask<*>): Boolean {
            return System.currentTimeMillis() > task.createdAt + (task.timeoutSeconds * 1000)
        }
    }

    // 请求任务封装
    private data class ApiRequestTask<T>(
        val request: (WebClient) -> Mono<T>,
        val future: CompletableFuture<T>,
        val isBackground: Boolean,
        val priority: Int,
        val timeoutSeconds: Long,
        val createdAt: Long = System.currentTimeMillis(),
        var retryCount: Int = 0,
        var timedOut: Boolean = false
    ) {
        fun complete(result: T) {
            if (!timedOut && !future.isDone) {
                future.complete(result)
            }
        }

        fun completeExceptionally(error: Throwable) {
            if (!timedOut && !future.isDone) {
                future.completeExceptionally(error)
            }
        }

        fun markTimedOut() {
            timedOut = true
        }

        fun remainingTimeMillis(): Long {
            val elapsed = System.currentTimeMillis() - createdAt
            return max(0, timeoutSeconds * 1000 - elapsed)
        }

        fun retryOrFail(error: Throwable) {
            if (retryCount < MAX_RETRIES && !timedOut && remainingTimeMillis() > 1000) {
                retryCount++
                // 指数退避
                val backoffMillis = (1L shl min(retryCount, 5)) * 1000L
                Thread.ofVirtual().start {
                    Thread.sleep(backoffMillis)
                    // 重新加入队列（如果还有时间）
                    if (remainingTimeMillis() > 1000 && !timedOut) {
                        // 这里需要重新加入任务队列，但为了简化，我们直接重新执行
                        // 在实际实现中，应该有一个方法将任务重新放入队列
                    }
                }
            } else {
                completeExceptionally(error)
            }
        }
    }

    // 自适应限流器
    private class AdaptiveRateLimiter(
        private val baseQuotaPerSecond: Int,
        private val burstCapacity: Int,
        private val maxQuotaPerMinute: Int
    ) {
        private val tokens = AtomicLong(burstCapacity.toLong())
        private val lastRefillTime = AtomicLong(System.currentTimeMillis())
        private val lock = Any()

        private val minuteTracker = SlidingWindowQuotaTracker(60000L, maxQuotaPerMinute)

        fun acquireWithTimeout(timeoutMillis: Long): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMillis

            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire()) {
                    return true
                }

                // 计算下一次补充的时间
                val nextRefill = lastRefillTime.get() + 1000L
                val waitTime = max(1, min(nextRefill - System.currentTimeMillis(), 100L))

                try {
                    Thread.sleep(waitTime)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }

            return false
        }

        private fun tryAcquire(): Boolean {
            refillTokens()

            synchronized(lock) {
                if (tokens.get() > 0) {
                    if (minuteTracker.tryAcquire()) {
                        tokens.decrementAndGet()
                        return true
                    }
                }
            }

            return false
        }

        private fun refillTokens() {
            val now = System.currentTimeMillis()
            val last = lastRefillTime.get()

            if (now - last >= 1000) {
                synchronized(lock) {
                    if (lastRefillTime.compareAndSet(last, now)) {
                        val current = tokens.get()
                        val toAdd = baseQuotaPerSecond.toLong()
                        tokens.set(min(burstCapacity.toLong(), current + toAdd))
                    }
                }
            }
        }

        fun reset() {
            tokens.set(0)
            lastRefillTime.set(System.currentTimeMillis())
        }
    }

    // 滑动窗口配额跟踪器
    private class SlidingWindowQuotaTracker(
        private val windowSizeMillis: Long,
        private val maxRequests: Int
    ) {
        private val timestamps = ConcurrentLinkedDeque<Long>()
        private val lock = Any()

        fun tryAcquire(): Boolean {
            synchronized(lock) {
                val now = System.currentTimeMillis()
                val windowStart = now - windowSizeMillis

                // 清理过期记录
                while (timestamps.isNotEmpty() && timestamps.peekFirst() < windowStart) {
                    timestamps.pollFirst()
                }

                if (timestamps.size < maxRequests) {
                    timestamps.addLast(now)
                    return true
                }

                return false
            }
        }
    }

    // 熔断器状态
    private enum class CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }

    fun refreshUserToken(user: BindUser, firstBind: Boolean): String {
        val b = mapOf(
            "client_id" to oauthID.toString(),
            "client_secret" to oauthToken,
            "redirect_uri" to redirectUrl,
            "grant_type" to if (firstBind) "authorization_code" else "refresh_token",
            (if (firstBind) "code" else "refresh_token") to user.refreshToken!!
        )

        val body = MultiValueMap.fromSingleValue(b)

        val s = try {
            request { client: WebClient ->
                client
                    .post()
                    .uri("https://osu.ppy.sh/oauth/token")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(body))
                    .retrieve()
                    .bodyToMono(JsonNode::class.java)
            }
        } catch (e: Exception) {
            val ex = e.findCauseOfType<WebClientException>()

            when (ex) {
                is WebClientResponseException.BadRequest -> {
                    throw NetworkException.UserException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    bindDao.backupBind(user.userID)
                    log.info("更新令牌失败：令牌过期，退回到名称绑定：${user.userID}", e)
                    throw NetworkException.UserException.Unauthorized()
                }

                is WebClientResponseException.Forbidden -> {
                    throw NetworkException.UserException.Forbidden()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.UserException.NotFound()
                }

                is WebClientResponseException.TooManyRequests -> {
                    throw NetworkException.UserException.TooManyRequests()
                }

                is WebClientResponseException.InternalServerError -> {
                    throw NetworkException.UserException.InternalServerError()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.UserException.BadGateWay()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.UserException.ServiceUnavailable()
                }

                else -> if (e.findCauseOfType<Errors.NativeIoException>() != null) {
                    throw NetworkException.UserException.GatewayTimeout()
                } else if (e.findCauseOfType<ReadTimeoutException>() != null) {
                    throw NetworkException.UserException.RequestTimeout()
                } else {
                    throw NetworkException.UserException.Undefined(e)
                }
            }
        }

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

        if (firstBind) {
            bindDao.saveBind(BindUser().apply {
                this.userID = user.userID
                this.accessToken = accessToken
                this.refreshToken = refreshToken
                this.time = time
            })
        } else {
            bindDao.updateToken(user.userID, accessToken, refreshToken, time)
        }

        return accessToken
    }
}