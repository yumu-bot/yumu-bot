package com.now.nowbot.image.render

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.browser.RenderException
import com.now.nowbot.browser.ScreenshotOptions
import org.springframework.stereotype.Component

@Component
class SvgScreenshotRenderer(
    private val htmlRenderer: HtmlRenderer,
    private val policy: RenderOutputPolicy,
) {
    fun render(
        svg: String,
        format: ScreenshotType,
        allowedResourceUrls: Set<String>,
        transparent: Boolean = false,
    ): ByteArray {
        val size = SvgDocument.parseSize(svg)
        val omitBackground = transparent && format != ScreenshotType.JPEG
        val options = ScreenshotOptions(
            width = size.width,
            height = size.height,
            format = format,
            jpegQuality = policy.qualityFor(format),
            omitBackground = omitBackground,
            allowedResourceUrls = allowedResourceUrls,
        )
        val html = SvgDocument.toHtml(svg, size)
        return try {
            htmlRenderer.render(html, options)
        } catch (e: Exception) {
            throw RenderException(
                "SVG 截图失败（$format，上限 ${policy.svgScreenshotTimeoutMillis}ms）: ${e.message}",
                e,
            )
        }
    }
}
