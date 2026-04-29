package com.now.nowbot.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.concurrent.Executor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor

/***
 * 线程池配置
 */
@Component
@EnableAsync
@Configuration
class AsyncSetting : AsyncConfigurer {
    @Bean
    fun AsyncConf(): AsyncTaskExecutor {
        return threadPool
    }

    @Bean("botAsyncExecutor")
    fun executor(): ThreadPoolTaskExecutor {
        return threadPool
    }

    @Bean(name = ["mainExecutor", "shiroTaskExecutor"])
    override fun getAsyncExecutor(): Executor {
        return threadPool
    }

    /**
     * 自定义异常处理
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? {
        return AsyncUncaughtExceptionHandler { throwable: Throwable?, method: Method?, obj: Array<Any?>? ->
            log.info("Method data - {}", method!!.name, throwable)
            for (param in obj!!) {
                log.info("Parameter value - {}", param.toString())
            }
        }
    }

    @get:Bean(name = ["applicationTaskExecutor", "taskExecutor", "kotlinTaskExecutor"])
    val taskExecutor: TaskExecutor
        get() = threadPool

    internal class ThreadFactoryBox(private val threadFactory: ThreadFactory) : ThreadFactory {
        override fun newThread(r: Runnable): Thread? {
            return threadFactory.newThread {
                try {
                    r.run()
                } catch (e: Throwable) {
                    log.error("thread throw: ", e)
                }
            }
        }
    }


    companion object {
        private val log: Logger = LoggerFactory.getLogger(AsyncSetting::class.java)

        private val virtualThreadFactory: ThreadFactory = Thread.ofVirtual().name("Bot").factory()
        val threadFactory: ThreadFactory = ThreadFactoryBox(virtualThreadFactory)

        private val threadPool: ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
            this.setThreadFactory(threadFactory)
            this.corePoolSize = 1000
            this.maxPoolSize = Int.MAX_VALUE
            this.keepAliveSeconds = 0
            this.queueCapacity = Runtime.getRuntime().availableProcessors() * 10
            this.setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
            this.setWaitForTasksToCompleteOnShutdown(false)
            this.setAwaitTerminationSeconds(5)
            this.initialize()
        }
    }
}