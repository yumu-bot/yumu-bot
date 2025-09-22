package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.OsuConfig
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.service.RequestService
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
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutionException
import java.util.function.Consumer

@Service
class OsuApiBaseService(@Lazy private val bindDao: BindDao, @Qualifier("osuApiWebClient") val osuApiWebClient: WebClient, osuConfig: OsuConfig, yumuConfig: YumuConfig) {
    @JvmField final val oauthId: Int
    @JvmField final val redirectUrl: String
    @JvmField final val oauthToken: String

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

    private lateinit var requestService: RequestService

    @PostConstruct fun init() {
        requestService = RequestService(osuApiWebClient, "osu-api-priority")
        Thread.startVirtualThread {
            requestService.runTask()
        }
    }

    @Throws(ExecutionException::class)
    fun <T> request(request: (WebClient) -> Mono<T>): T {
        return requestService.request(request)
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

            requestService.setPriority(0)
            val s = request { client: WebClient ->
                client
                    .post()
                    .uri("https://osu.ppy.sh/oauth/token")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(body)).retrieve()
                    .bodyToMono(JsonNode::class.java)
            }
            requestService.clearPriority()

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
        if (!requestService.hasPriority()) {
            requestService.setPriority(1)
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
        requestService.clearPriority()
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
                "x-api-version" to "20240529"
            )
        )
    }

    fun insertHeader(user: BindUser): Consumer<HttpHeaders> {
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

        return Consumer { headers: HttpHeaders ->
            headers.setAll(
                mapOf(
                    "Authorization" to "Bearer $token",
                    "x-api-version" to "20241101"
                )
            )
        }
    }

    /*
    @Throws(ExecutionException::class)
    fun <T> request(request: Function<WebClient, Mono<T>>): T {
        val future = CompletableFuture<T>()
        val priority = ContextUtil.getContext(
            THREAD_LOCAL_ENVIRONMENT, DEFAULT_PRIORITY,
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

    private fun runTask() {
        while (IocAllReadyRunner.APP_ALIVE) {
            try {
                limiter.acquire()
                val task = TASKS.take()
                Thread.startVirtualThread { task.run(osuApiWebClient) }
            } catch (e: InterruptedException) {
                log.error("请求队列异常", e)
            }
        }
    }

    internal class RequestTask<T>(
        private var priority: Int,
        private var future: CompletableFuture<T>,
        var request: Function<WebClient, Mono<T>>
    ) :
        Comparable<RequestTask<*>> {
        var time: Int = nowTime
        private var toManyRequests: Boolean = false
        private var retry: Short = 0

        fun run(client: WebClient) {
            request.apply(client).subscribe(
                Consumer { value: T -> future.complete(value) },
                Consumer { e: Throwable -> this.onError(e) }
            ) { this.onComplete() }
        }

        private fun onError(e: Throwable) {
            if (retry >= MAX_RETRY) {
                future.completeExceptionally(e)
            }

            when (e) {
                is WebClientResponseException.TooManyRequests -> {
                    log.info("出现 429 错误")
                    toManyRequests = true
                    TASKS.add(this)
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

        private fun onComplete() {
            if (toManyRequests) {
                log.info("请求过多, 触发退避")
                limiter.onTooManyRequests(TOO_MANY_REQUESTS_COUNT++)
            } else if (TOO_MANY_REQUESTS_COUNT > 0) {
                TOO_MANY_REQUESTS_COUNT = 0
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
            private var TOO_MANY_REQUESTS_COUNT = 0
            private val nowTime: Int
                get() = // seconds since 2025-01-01
                    (System.currentTimeMillis() / 1000 - 1735660800).toInt()
        }
    }

    internal class RateLimiter(var rate: Int, max: Int) {
        var out: Int = -1
        private var semaphore: Semaphore = Semaphore(max)

        init {
            Thread.startVirtualThread { this.run() }
        }

        fun run() {
            while (IocAllReadyRunner.APP_ALIVE) {
                LockSupport.parkNanos(Duration.ofSeconds(1).toNanos())
                semaphore.release(rate)
            }
        }

        @Throws(InterruptedException::class) fun acquire() {
            if (out >= 0) {
                val seconds = min(2 * out + 1, 10)
                log.info("请求触发退避, 等待时间: {} 秒", seconds)
                LockSupport.parkNanos(Duration.ofSeconds(seconds.toLong()).toNanos())
                out = -1
                val permits = semaphore.availablePermits()
                if (permits > 1) semaphore.acquire(permits - 1)
            }
            semaphore.acquire()
        }

        fun onTooManyRequests(n: Int) {
            out = n + 1
        }
    }

     */

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuApiBaseService::class.java)
        private var accessToken: String? = null
        private var time = System.currentTimeMillis()
        /*
        private const val DEFAULT_PRIORITY = 5
        private const val MAX_RETRY = 3
        private val limiter = RateLimiter(1, 20)

        private val TASKS = PriorityBlockingQueue<RequestTask<*>>()

         */

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
