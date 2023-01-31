package com.now.nowbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/***
 * 线程池配置
 */
@Component
public class AsyncSetting implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncSetting.class);
    static private final ThreadPoolTaskExecutor threadPool;
    static {
        threadPool = new ThreadPoolTaskExecutor();
        threadPool.setCorePoolSize(5);
        threadPool.setMaxPoolSize(20);
        threadPool.setWaitForTasksToCompleteOnShutdown(true);
        threadPool.setAwaitTerminationSeconds(60 * 15);
        threadPool.setThreadNamePrefix("NoBot-");
        threadPool.initialize();
    }

    @Bean("botAsyncExecutor")
    public ThreadPoolTaskExecutor executor() {
        return threadPool;
    }
    @Override
    public Executor getAsyncExecutor() {
        return AsyncSetting.threadPool;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.info("Method name - {}", method.getName(),throwable);
            for (Object param : obj) {
                log.info("Parameter value - " + param.toString());
            }
        };
    }

    /**
     * 自定义异常处理类
     * @author hry
     *
     */

    @Bean
    public AsyncTaskExecutor AstncConf(){
        return AsyncSetting.threadPool;
    }
    @Bean
    public ThreadPoolTaskExecutor getThreadPool(){
        return AsyncSetting.threadPool;
    }
}