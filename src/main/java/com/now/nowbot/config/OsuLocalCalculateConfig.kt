package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.local", ignoreInvalidFields = true)
class OsuLocalCalculateConfig {
    var host: String = "localhost"

    var port: Int = 23316

    // 如果为真，则会启用 rosu。
    var rosu: Boolean = false
}