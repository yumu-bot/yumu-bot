package com.now.nowbot.config

import com.now.nowbot.controller.RenderWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
@EnableWebSocket
class RenderWebSocketConfig(
    private val renderWebSocketHandler: RenderWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(renderWebSocketHandler, "/render-ws")
            .setAllowedOrigins("*") // 允许跨域
    }

    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()

        // 设置文本消息和二进制消息的最大缓冲区 (这里设为 10MB)
        container.maxTextMessageBufferSize = 20 * 1024 * 1024
        container.maxBinaryMessageBufferSize = 20 * 1024 * 1024

        // 设置空闲超时时间
        container.maxSessionIdleTimeout = 3 * 60 * 1000L // 3分钟

        return container
    }
}