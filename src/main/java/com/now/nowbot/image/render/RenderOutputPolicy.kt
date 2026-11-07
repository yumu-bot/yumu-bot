package com.now.nowbot.image.render

import com.microsoft.playwright.options.ScreenshotType
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * 原生渲染的输出格式策略。默认值即产品约定：DEF=JPEG q90，ADV=WEBP q90。
 *
 * [svgScreenshotTimeoutMillis] 与浏览器池的单次渲染超时保持一致；实际的硬超时
 * 仍由 [com.now.nowbot.browser.RenderPool] 按 BrowserConfig 执行。
 */
@Validated
@Component
@ConfigurationProperties(prefix = "render.output", ignoreInvalidFields = true)
class RenderOutputPolicy {
    var defaultFormat: ScreenshotType = ScreenshotType.JPEG

    var advancedFormat: ScreenshotType = ScreenshotType.WEBP

    var jpegQuality: Int = 90

    var webpQuality: Int = 90

    var svgScreenshotTimeoutMillis: Long = 90_000L

    @PostConstruct
    fun validate() {
        require(jpegQuality in 0..100) { "render.output.jpeg-quality 必须在 0..100 之间: $jpegQuality" }
        require(webpQuality in 0..100) { "render.output.webp-quality 必须在 0..100 之间: $webpQuality" }
        require(svgScreenshotTimeoutMillis in 1_000L..600_000L) {
            "render.output.svg-screenshot-timeout-millis 必须在 1000..600000 之间: $svgScreenshotTimeoutMillis"
        }
    }

    fun qualityFor(format: ScreenshotType): Int {
        return when (format) {
            ScreenshotType.JPEG -> jpegQuality
            ScreenshotType.WEBP -> webpQuality
            else -> jpegQuality
        }
    }
}
