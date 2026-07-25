package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

import java.nio.file.Path

@Validated
@ConfigurationProperties(prefix = "yumu.diving-fish", ignoreInvalidFields = true)
class DivingFishConfig{
    /**
     * 接口路径, 一般不用改
     */
    var url: String = "https://www.diving-fish.com"

    var token: String = ""

    var maimai: String = "Maimai"

    var chunithm: String = "Chunithm"
}
