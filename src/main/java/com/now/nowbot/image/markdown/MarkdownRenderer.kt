package com.now.nowbot.image.markdown

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.browser.ScreenshotOptions
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

data class MarkdownRenderResult(
    val png: ByteArray,
    val width: Int,
    val height: Int,
)

@Service
class MarkdownRenderer(
    private val htmlRenderer: HtmlRenderer,
    private val resourceService: RenderResourceService,
    private val remoteImages: RemoteImageSource,
) {
    private val log = LoggerFactory.getLogger(MarkdownRenderer::class.java)

    fun render(
        markdown: String,
        width: Int = 1840,
        height: Int = 600,
        assetRoot: Path? = null,
    ): MarkdownRenderResult {
        val root = controlledRoot(assetRoot)
        val renderWidth = dimension(width, DEFAULT_WIDTH)
        val renderHeight = dimension(height, DEFAULT_HEIGHT)
        val lease = resourceService.lease()
        try {
            val report = RenderReport()
            val registry = RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }
            val assets = ImageAssets(
                registry,
                remoteImages,
                rootPath = root,
                report = report,
            )
            val fallbackAssets = ImageAssets(
                registry,
                null,
                rootPath = ImageAssets.defaultRoot(),
                report = report,
            )
            val rewritten = MarkdownImageRewriter.rewrite(
                markdown,
                MarkdownAssetResolver(
                    remote = { assets.remote(it) },
                    local = { assets.local(it) },
                    fallbackImage = {
                        assets.local("card-default.webp")
                            ?: fallbackAssets.local("card-default.webp")
                    },
                ),
            )
            if (report.missing.isNotEmpty()) {
                log.warn("Markdown 素材缺失: {} 项", report.missing.size)
            }
            val html = MarkdownHtml.build(rewritten, renderWidth, renderHeight) { name ->
                lease.register(MarkdownAssets.font(name), "font/ttf").url
            }
            val png = htmlRenderer.render(
                html,
                ScreenshotOptions(
                    width = renderWidth,
                    height = 1,
                    fullPage = true,
                    format = ScreenshotType.PNG,
                    allowedResourceUrls = lease.resourceUrls(),
                ),
            )
            val size = MarkdownPng.size(png)
            require(size.width in 1..MAX_DIMENSION && size.height in 1..MAX_DIMENSION) {
                "Markdown 截图尺寸超出限制: ${size.width}x${size.height}"
            }
            lease.releaseNow()
            return MarkdownRenderResult(png, size.width, size.height)
        } catch (t: Throwable) {
            lease.close()
            throw t
        }
    }

    private fun controlledRoot(root: Path?): Path? {
        if (root == null) return null
        val controlled = ImageAssets.defaultRoot()
            ?: throw IllegalStateException("受控素材根未配置")
        val realControlled = runCatching { controlled.toRealPath() }.getOrElse {
            throw IllegalStateException("受控素材根不可用")
        }
        val realRoot = runCatching { root.toRealPath() }.getOrElse {
            throw IllegalArgumentException("assetRoot 不可用")
        }
        require(realRoot.startsWith(realControlled)) {
            "assetRoot 必须位于受控素材根内"
        }
        require(Files.isDirectory(realRoot)) { "assetRoot 不是目录" }
        return realRoot
    }

    private fun dimension(value: Int, fallback: Int): Int =
        if (value < 1) fallback else value.coerceAtMost(MAX_DIMENSION)

    private companion object {
        private const val DEFAULT_WIDTH = 1840
        private const val DEFAULT_HEIGHT = 600
        private const val MAX_DIMENSION = 16384
    }
}
