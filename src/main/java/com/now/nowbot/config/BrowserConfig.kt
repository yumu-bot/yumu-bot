package com.now.nowbot.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.ZoneId
import java.util.*

@Primary
@Validated
@Component("browserConfig")
@ConfigurationProperties(prefix = "browser", ignoreInvalidFields = true)
class BrowserConfig {
    /**
     * 浏览器可执行文件路径；留空时由 Playwright 自行发现 Chromium。
     * 仍需系统已安装浏览器及其依赖，不会自动下载。
     */
    var executablePath: String = ""

    var poolSize: Int = 5

    var deviceScaleFactor: Double = 1.0

    var renderTimeoutMillis: Long = 90_000L

    var acquireTimeoutMillis: Long = 30_000L

    var launchTimeoutMillis: Long = 30_000L

    var chromiumSandbox: Boolean = false

    var locale: String = "zh-CN"

    var timezone: String = "Asia/Shanghai"

    var resultWaitGraceMillis: Long = 30_000L

    var closeWaitTimeoutMillis: Long = 30_000L

    var closeCleanupTimeoutMillis: Long = 15_000L

    @PostConstruct
    fun validate() {
        require(poolSize in 1..32) { "poolSize 必须在 1..32 之间: $poolSize" }
        require(deviceScaleFactor in 0.5..4.0) { "deviceScaleFactor 必须在 0.5..4.0 之间: $deviceScaleFactor" }
        require(renderTimeoutMillis in 1_000L..600_000L) {
            "renderTimeoutMillis 必须在 1000..600000 之间: $renderTimeoutMillis"
        }
        require(resultWaitGraceMillis in 1_000L..600_000L) {
            "resultWaitGraceMillis 必须在 1000..600000 之间: $resultWaitGraceMillis"
        }
        require(acquireTimeoutMillis in 1L..600_000L) {
            "acquireTimeoutMillis 必须在 1..600000 之间: $acquireTimeoutMillis"
        }
        require(launchTimeoutMillis in 1_000L..600_000L) {
            "launchTimeoutMillis 必须在 1000..600000 之间: $launchTimeoutMillis"
        }
        require(closeWaitTimeoutMillis in 1_000L..900_000L) {
            "closeWaitTimeoutMillis 必须在 1000..900000 之间: $closeWaitTimeoutMillis"
        }
        require(closeCleanupTimeoutMillis in 1_000L..600_000L) {
            "closeCleanupTimeoutMillis 必须在 1000..600000 之间: $closeCleanupTimeoutMillis"
        }
        require(Locale.forLanguageTag(locale).language.isNotBlank()) { "locale 不是有效的语言标签: $locale" }
        require(runCatching { ZoneId.of(timezone) }.isSuccess) { "timezone 不是有效的时区: $timezone" }
    }
}