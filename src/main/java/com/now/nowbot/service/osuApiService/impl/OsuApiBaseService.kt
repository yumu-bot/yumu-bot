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
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

@Service
class OsuApiBaseService(
    @param:Lazy private val bindDao: BindDao,
    @param:Qualifier("osuApiWebClient") val osuApiWebClient: WebClient,
    osuConfig: OsuConfig,
    yumuConfig: YumuConfig
) {
    final val oauthId: Int
    final val redirectUrl: String
    final val oauthToken: String

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

    @PostConstruct
    fun init() {
        // 使用虚拟线程启动消费者，名称方便调试
        Thread.ofVirtual().name("osu-api-consumer").start {
            runTask()
        }
    }

    /**
     * 发送请求
     * 支持 StructuredTaskScope，如果 Scope 关闭导致线程中断，会抛出异常并尝试取消 Future
     */
    fun <T> request(request: (WebClient) -> Mono<T>): T {
        val future = CompletableFuture<T>()
        val priority = ContextUtil.getContext(
            priorityEnvironment, DEFAULT_PRIORITY,
            Int::class.java
        ) ?: DEFAULT_PRIORITY

        val task = RequestTask(priority, future, request)
        TASKS.offer(task)

        try {
            // 兼容 StructuredTaskScope：如果外部取消或超时，get() 会抛出 InterruptedException
            return future.get()
        } catch (e: InterruptedException) {
            // 响应中断：尝试取消 Future，并不再重试
            future.cancel(true)
            Thread.currentThread().interrupt() // 恢复中断状态
            throw RuntimeException("OsuApi request interrupted", e)
        } catch (e: ExecutionException) {
            // 解包 ExecutionException，抛出原始异常
            val cause = e.cause
            if (cause is RuntimeException) throw cause
            throw RuntimeException(cause)
        }
    }

    fun runTask() {
        while (IocAllReadyRunner.APP_ALIVE) {
            val task = TASKS.take()
            val priority = task.priority

            // 关键逻辑：如果是后台任务，必须先过一遍极慢的 backgroundLimiter
            if (priority > 5) {
                backgroundLimiter.acquire(priority)
            }

            // 然后再过全局 limiter (确保不突破总配额)
            limiter.acquire(priority)

            Thread.ofVirtual().start { task.run(osuApiWebClient) }
        }
    }

    // --- 内部类：请求任务 ---
    internal class RequestTask<T>(
        var priority: Int,
        private var future: CompletableFuture<T>,
        var request: (WebClient) -> Mono<T>
    ) : Comparable<RequestTask<*>> {

        var time: Int = nowTime

        private var is429: AtomicBoolean = AtomicBoolean(false)
        private var retry: Short = 0

        fun run(client: WebClient) {
            // 如果 Future 已经被取消（例如调用方超时），则不再执行
            if (future.isDone || future.isCancelled) return

            request(client)
                .timeout(Duration.ofSeconds(30))
                .subscribe(
                    { value: T -> future.complete(value) },
                    { e: Throwable -> this.onError(e) },
                    { this.onComplete() }
                )
        }

        private fun onError(e: Throwable) {
            if (future.isDone || future.isCancelled) return

            if (retry >= MAX_RETRY) {
                future.completeExceptionally(e)
                return
            }

            when (e) {
                is WebClientResponseException.TooManyRequests -> {

                    val retryAfterStr = e.headers.getFirst("Retry-After")
                    val waitSeconds = retryAfterStr?.toLongOrNull() ?: calculateExponentialBackoff(retry)

                    // 1. 先同步状态
                    limiter.onTooManyRequests(waitSeconds)

                    // 2. 检查：如果此时已经在熔断，且我还没到 MAX_RETRY，就悄悄回队列，别打印 Retry 日志了
                    if (retry < MAX_RETRY) {
                        retry++
                        Thread.ofVirtual().start {
                            // 比熔断时间多等一秒，确保消费者先启动
                            Thread.sleep(waitSeconds * 1000L + 1000)
                            TASKS.add(this@RequestTask)
                        }
                        return // 直接返回，屏蔽掉后面的 log.info
                    }
                }
                is WebClientRequestException -> {
                    // 网络层面的错误（连接超时等），重试
                    retry++
                    log.warn("Network error, retrying... ($retry/$MAX_RETRY)")
                    TASKS.add(this)
                }
                else -> {
                    future.completeExceptionally(e)
                }
            }
        }

        private fun calculateExponentialBackoff(retry: Short): Long {
            val baseDelay = 16L shl min(retry.toInt(), 5)
            return minOf(baseDelay, 512L)
        }

        private fun onComplete() {
            if (is429.get()) {
                limiter.onTooManyRequests(TOO_MANY_REQUESTS_COUNT.getAndIncrement())
            } else {
                // 成功请求后逐渐减少计数
                if (TOO_MANY_REQUESTS_COUNT.get() > 0) {
                    // 简单的减少策略，避免并发 CAS 竞争太激烈
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        TOO_MANY_REQUESTS_COUNT.decrementAndGet()
                    }
                }
            }
        }

        override fun compareTo(other: RequestTask<*>): Int {
            return getPriorityValue() - other.getPriorityValue()
        }

        private fun getPriorityValue(): Int {
            return (3600 * priority) + time
        }

        companion object {
            private var TOO_MANY_REQUESTS_COUNT = AtomicInteger(0)
            private val nowTime: Int
                get() = (System.currentTimeMillis() / 1000 - 1735660800).toInt()
        }
    }

    // --- 内部类：重构后的 RateLimiter ---
    /**
     * 令牌桶 + 滑动窗口限流器
     * 修复了原版 Semaphore 无限增长和锁内睡眠的问题
     */
    internal class RateLimiter(
        private val permitsPerSecond: Int, // 每秒填充速率
        private val maxBurst: Int,         // 最大桶容量
        private val minuteLimit: Int = 800 // 分钟硬限制
    ) {
        // 用于退避（Backoff）的时间乘数
        @Volatile var backoffMultiplier: Int = -1
        // 全局冷却截止时间戳（毫秒）
        @Volatile private var coolingDownUntil: Long = 0

        // 1. 瞬时/秒级限流：使用 Semaphore，但严格控制 release
        private val burstSemaphore = Semaphore(maxBurst)

        // 2. 分钟级限流：使用队列记录时间戳
        private val minuteRequests = ConcurrentLinkedDeque<Long>()
        private val minuteLock = ReentrantLock()

        init {
            // 启动填充线程
            Thread.ofVirtual().name("rate-limiter-refill").start {
                while (IocAllReadyRunner.APP_ALIVE) {
                    try {
                        // 均匀填充，例如 5 req/s -> 每 200ms 填充 1 个
                        // 这里的 1 req/s -> 1000ms
                        val interval = 1000L / permitsPerSecond
                        Thread.sleep(interval)

                        // 关键修复：只有在未满时才释放，防止无限积攒
                        if (burstSemaphore.availablePermits() < maxBurst) {
                            burstSemaphore.release()
                        }
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }

        @Throws(InterruptedException::class)
        fun acquire(priority: Int = 5) {
            val now = System.currentTimeMillis()

            if (now < coolingDownUntil) {
                val waitTime = coolingDownUntil - now
                if (priority <= 5) {
                    if (waitTime > 25000) { // 如果要等 5 秒以上
                        log.error("API 熔断时间过长，为了保住前台响应，直接熔断该请求")
                        throw RuntimeException("请求超时。请等待一会后重试。")
                    }
                    Thread.sleep(waitTime + 100)
                } else {
                    // 后台任务随便等
                    Thread.sleep(waitTime + 30000)
                }
            }

            // 3. 检查分钟级限流 (Sliding Window)
            checkMinuteLimitBlocking()

            // 4. 获取令牌 (阻塞等待)
            burstSemaphore.acquire()
        }

        private fun checkMinuteLimitBlocking() {
            while (true) {
                var sleepTime: Long

                // 仅在计算检查时加锁，不在锁内 sleep
                minuteLock.lock()

                try {
                    val now = System.currentTimeMillis()
                    val oneMinuteAgo = now - 60000

                    // 清理过期记录
                    while (!minuteRequests.isEmpty() && minuteRequests.peekFirst() < oneMinuteAgo) {
                        minuteRequests.pollFirst()
                    }

                    if (minuteRequests.size < minuteLimit) {
                        // 未超限，记录当前时间并通行
                        minuteRequests.addLast(now)
                        return
                    } else {
                        // 已超限，计算需要等待的时间
                        val oldest = minuteRequests.peekFirst()
                        // 如果 oldest 异常为空（并发边缘情况），默认等 1秒
                        sleepTime = if (oldest != null) (oldest + 60000) - now else 1000L
                        // 防止计算出负数
                        if (sleepTime < 0) sleepTime = 0
                    }
                } finally {
                    minuteLock.unlock()
                }

                // 在锁外睡眠
                if (sleepTime > 0) {
                    if (sleepTime > 1000) log.warn("分钟限流生效，等待 {} ms", sleepTime)
                    Thread.sleep(sleepTime + 20) // 多睡 20ms 以确保过期
                }
            }
        }

        // 修改：由计数器改为具体的时间窗熔断
        fun onTooManyRequests(waitSeconds: Long) {
            // 额外惩罚 5 秒，确保完全越过 API 的封锁窗口
            val penalty = 5000L
            val nextWindow = System.currentTimeMillis() + (waitSeconds * 1000L) + penalty

            if (nextWindow > coolingDownUntil) {
                coolingDownUntil = nextWindow
                burstSemaphore.drainPermits()
                log.error("触发熔断并增加惩罚时间，直到：{}", Instant.ofEpochMilli(nextWindow).atOffset(ZoneOffset.ofHours(8)))
            }
        }

        fun onTooManyRequests(n: Int) {
            backoffMultiplier = n + 1
            burstSemaphore.drainPermits()
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

        private val limiter = RateLimiter(5, 5, 300)
        private val backgroundLimiter = RateLimiter(1, 1, 100)

        private val TASKS = PriorityBlockingQueue<RequestTask<*>>()

        fun getTaskQueueSize(): Int = TASKS.size

        fun hasPriority(): Boolean = ContextUtil.getContext("osu-api-priority", Int::class.java) != null

        fun setPriority(priority: Int) = ContextUtil.setContext("osu-api-priority", priority)

        fun clearPriority() = ContextUtil.setContext("osu-api-priority", null)
    }
}
