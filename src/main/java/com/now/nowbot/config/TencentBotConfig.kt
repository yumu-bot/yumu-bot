package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 *
 */
@Validated
@ConfigurationProperties(prefix = "yumu.tencent", ignoreInvalidFields = true, ignoreUnknownFields = true)
class TencentBotConfig {
    var appId: Int = 0

    var token: String = ""

    var secret: String = ""

    // 使用 webhook 时必填, 填写后 websocket 失效
    var endpoint: String? = null

    var selfId: String = ""

    var authUrl = "https://bots.qq.com/app/getAppAccessToken"
    var apiUrl = "https://api.sgroup.qq.com"
    var wsUrl = "wss://api.sgroup.qq.com/websocket/gateway"
}