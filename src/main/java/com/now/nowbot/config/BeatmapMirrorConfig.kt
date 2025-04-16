package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.mirror", ignoreInvalidFields = true)
class BeatmapMirrorConfig {
    var url: String? = ""

    var token: String? = ""
        get() = if (field.isNullOrEmpty()) System.getenv("API_TOKEN") else field
}