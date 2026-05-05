package com.now.nowbot.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncSetting : AsyncConfigurer {

    // 将 threadPool 移动到 Spring Bean 管理下，不要放在 companion object 里
    @Bean(name = ["botAsyncExecutor", "mainExecutor", "shiroTaskExecutor", "applicationTaskExecutor", "taskExecutor", "kotlinTaskExecutor"])
    fun threadPoolTaskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()

        // 关键：使用虚拟线程工厂
        executor.setThreadFactory(threadFactory)

        executor.corePoolSize = 100
        executor.maxPoolSize = 2000 // 虚拟线程不需要太大的池管理，其实可以直接用虚拟线程执行器
        executor.queueCapacity = 500

        // 允许优雅停机
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(10) // 给10秒收尾时间

        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }

    // 实现接口方法，直接注入上面定义的 Bean
    override fun getAsyncExecutor(): Executor {
        return threadPoolTaskExecutor()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AsyncSetting::class.java)

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

        // 仅保留 Factory
        private val virtualThreadFactory: ThreadFactory = Thread.ofVirtual().name("Bot-", 0).factory()
        val threadFactory: ThreadFactory = ThreadFactoryBox(virtualThreadFactory)
    }
}