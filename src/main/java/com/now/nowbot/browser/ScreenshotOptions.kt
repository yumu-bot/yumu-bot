package com.now.nowbot.browser

import com.microsoft.playwright.options.ScreenshotType

data class ScreenshotOptions(
    val width: Int = 1920,
    val height: Int = 1080,
    val fullPage: Boolean = false,
    val format: ScreenshotType = ScreenshotType.WEBP,
    val jpegQuality: Int = 80,
    val omitBackground: Boolean = false,
    val allowedResourceUrls: Set<String> = emptySet(),
) {
    init {
        require(width in 1..16_384) { "width 必须在 1..16384 之间: $width" }
        require(height in 1..16_384) { "height 必须在 1..16384 之间: $height" }
        require(jpegQuality in 0..100) { "jpegQuality 必须在 0..100 之间: $jpegQuality" }
        require(format != ScreenshotType.JPEG || !omitBackground) { "JPEG 不支持透明背景，omitBackground 只能用于 PNG, WEBP" }
        require(allowedResourceUrls.all { RenderResourceAccessPolicy.isSafeResourceUrl(it) }) {
            "allowedResourceUrls 只能包含受控的 /render-resource/<id> http(s) URL"
        }
    }
}
