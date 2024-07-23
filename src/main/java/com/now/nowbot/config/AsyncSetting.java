package com.now.nowbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/***
 * 线程池配置
 */
@Component
@EnableAsync
@Configuration
public class AsyncSetting implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncSetting.class);
    public static final ThreadFactory V_THREAD_FACORY = Thread.ofVirtual().name("Bot").factory();
    static private final ThreadPoolTaskExecutor threadPool;

    static {
        threadPool = new ThreadPoolTaskExecutor();
        threadPool.setThreadFactory(V_THREAD_FACORY);
        threadPool.setCorePoolSize(1000);
        threadPool.setMaxPoolSize(Integer.MAX_VALUE);
        threadPool.setKeepAliveSeconds(5);
        threadPool.setQueueCapacity(Runtime.getRuntime().availableProcessors() * 10);
        threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        threadPool.setWaitForTasksToCompleteOnShutdown(true);
        threadPool.setAwaitTerminationSeconds(5);
        threadPool.initialize();
    }

    @Bean("botAsyncExecutor")
    public ThreadPoolTaskExecutor executor() {
        return threadPool;
    }

    @Bean(name = {"mainExecutor", "shiroTaskExecutor"})
    public Executor getAsyncExecutor() {
        return AsyncSetting.threadPool;
    }

    /**
     * 自定义异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.info("Method data - {}", method.getName(), throwable);
            for (Object param : obj) {
                log.info("Parameter value - {}", param.toString());
            }
        };
    }

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public TaskExecutor getTaskExecutor(){
        return AsyncSetting.threadPool;
    }

    @Bean
    public AsyncTaskExecutor AstncConf() {
        return AsyncSetting.threadPool;
    }
}