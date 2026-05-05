package com.now.nowbot.config

import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.calculate", ignoreInvalidFields = true)
class OsuLocalCalculateConfig {
    // cosu 的地址
    var host: String = "localhost"

    // cosu 的端口
    var port: Int = 23316

    // 如果为真，则会启用 rosu。
    var rosu: Boolean = false

    var priority: List<CalculateApiImpl.CalculateStrategy> = listOf(
        CalculateApiImpl.CalculateStrategy.LOCAL_DATABASE,
        CalculateApiImpl.CalculateStrategy.OFFICIAL_API,
        CalculateApiImpl.CalculateStrategy.R_OSU,
        CalculateApiImpl.CalculateStrategy.C_OSU,
    )
}