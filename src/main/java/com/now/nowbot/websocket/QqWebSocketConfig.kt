package com.now.nowbot.websocket

import com.now.nowbot.util.KB
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
import kotlin.time.Duration.Companion.minutes

/**
 * 全局 JSR-356 容器参数。QQ 的 /qq-ws 由 ServerEndpointExporter（NowbotConfig）注册，
 * 依赖这里的文本/二进制缓冲区与空闲超时设置。
 */
@Configuration
class QqWebSocketConfig {
    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()

        container.maxTextMessageBufferSize = 64.KB.bytesInt
        container.maxBinaryMessageBufferSize = 256.KB.bytesInt
        container.maxSessionIdleTimeout = 1.minutes.inWholeMilliseconds

        return container
    }
}
