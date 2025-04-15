package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.mirror", ignoreInvalidFields = true)
class BeatmapMirrorConfig {
    var url: String? = null

    var token: String? = null
        get() = field ?: System.getenv("API_TOKEN")
}