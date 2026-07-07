package com.now.nowbot.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration

@Configuration
class FuckOffRedisConfig : ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        try {
            // 在 Spring 刚准备好环境、还没加载任何 Data 模块前，强行改掉级别
            val logger = LoggerFactory.getLogger(
                "org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport"
            ) as Logger

            logger.setLevel(Level.WARN)
        } catch (e: Throwable) {
            // 防止非 Logback 桥接导致崩溃
        }
    }
}