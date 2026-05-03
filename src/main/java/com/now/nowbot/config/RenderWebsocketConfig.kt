package com.now.nowbot.config

import com.now.nowbot.controller.RenderWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
import kotlin.time.Duration.Companion.minutes

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

        container.maxTextMessageBufferSize = 2 * 1024 * 1024
        container.maxBinaryMessageBufferSize = 20 * 1024 * 1024

        // 设置空闲超时时间
        container.maxSessionIdleTimeout = 1.minutes.inWholeMilliseconds

        return container
    }
}