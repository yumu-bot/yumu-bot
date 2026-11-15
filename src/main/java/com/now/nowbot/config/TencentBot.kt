package com.now.nowbot.config

import com.now.nowbot.qq.tencent.TencentBotListener
import com.yumu.qq.QQBot
import com.yumu.qq.bot.WebhookBot
import com.yumu.qq.model.QQBotConfig
import com.yumu.qq.webhook.spring.SpringWebhookAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration(proxyBeanMethods = false)
class TencentBot {
    @Bean
    @ConditionalOnExpression(
        """
        environment.getProperty('yumu.tencent.app-id', T(java.lang.Integer), 0) != 0
        and T(org.springframework.util.StringUtils).hasText(environment.getProperty('yumu.tencent.token'))
        and T(org.springframework.util.StringUtils).hasText(environment.getProperty('yumu.tencent.secret'))
        and T(org.springframework.util.StringUtils).hasText(environment.getProperty('yumu.tencent.endpoint'))
        """
    )
    fun webhookBot(config: TencentBotConfig): WebhookBot {
        return config.register()!!
    }

    @Bean
    @ConditionalOnBean(WebhookBot::class)
    fun webhookAdapter(
        webhook: WebhookBot,
        config: TencentBotConfig,
        @Qualifier("requestMappingHandlerMapping") mapping: RequestMappingHandlerMapping,
    ): SpringWebhookAdapter = SpringWebhookAdapter(
        webhook,
        requireNotNull(config.endpoint),
    ).apply { register(mapping) }


    // 没有 endpoint 直接走 websocket
    @Bean
    @ConditionalOnExpression(
        """
        environment.getProperty('yumu.tencent.app-id', T(java.lang.Integer), 0) != 0
        and T(org.springframework.util.StringUtils).hasText(environment.getProperty('yumu.tencent.token'))
        and T(org.springframework.util.StringUtils).hasText(environment.getProperty('yumu.tencent.secret'))
        and T(org.springframework.util.StringUtils).isEmpty(environment.getProperty('yumu.tencent.endpoint'))
        """
    )
    fun websocketBot(
        config: TencentBotConfig,
    ): TencentBotListener {
        config.register()
        return TencentBotListener
    }

    fun TencentBotConfig.register(): WebhookBot? {
        val config = QQBotConfig(
            app = appId,
            token = token,
            secret = secret,
            selfId = selfId,
            endpoint = endpoint,
            serverUrl = QQBotConfig.Url(
                auth = authUrl,
                api = apiUrl,
                ws = wsUrl,
            )
        )

        return QQBot.registerBot(config, TencentBotListener)
    }
}