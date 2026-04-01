package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "yumu.cosu", ignoreInvalidFields = true)
class CosuConfig {
    var url: String = "http://127.0.0.1:23316/calculate"
}