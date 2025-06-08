package com.now.nowbot.service

import com.now.nowbot.config.IocAllReadyRunner
import com.now.nowbot.util.ContextUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.LockSupport
import kotlin.math.min

@Service
class RequestService(private val webClient: WebClient, private val priorityEnvironment: String = "osu-api-priority") {

    @Throws(ExecutionException::class)
    fun <T> request(request: (WebClient) -> Mono<T>): T {
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
                    task.run(webClient)
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
        private var toManyRequests: Boolean = false
        private var retry: Short = 0

        fun run(client: WebClient) {
            request(client)
                .subscribe(
                    { value: T -> future.complete(value) },
                    { e: Throwable -> this.onError(e) }
                ) {
                    this.onComplete()
                }
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

    fun hasPriority(): Boolean {
        return ContextUtil.getContext(priorityEnvironment, Int::class.java) != null
    }

    fun setPriority(priority: Int) {
        ContextUtil.setContext(priorityEnvironment, priority)
    }

    fun clearPriority() {
        ContextUtil.setContext(priorityEnvironment, null)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RequestService::class.java)

        private const val DEFAULT_PRIORITY = 5
        private const val MAX_RETRY = 3
        private val limiter = RateLimiter(1, 20)

        private val TASKS = PriorityBlockingQueue<RequestTask<*>>()
    }
}