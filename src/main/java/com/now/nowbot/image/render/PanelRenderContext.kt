package com.now.nowbot.image.render

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease

/**
 * 单次面板渲染的资源组合。lease 的成功/失败生命周期只在这里收口：
 * 截图成功后 [RenderResourceLease.releaseNow]，生成或截图异常/超时则 [RenderResourceLease.close]。
 */
class PanelRenderContext(
    private val renderer: SvgScreenshotRenderer,
    private val policy: RenderOutputPolicy,
    internal val lease: RenderResourceLease,
    internal val remoteImageSource: RemoteImageSource,
    val report: RenderReport = RenderReport(),
) : AutoCloseable {
    val assets: ImageAssets = ImageAssets(
        RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
        remoteImageSource,
        report = report,
    )

    private var closed = false

    fun renderSvg(
        format: ScreenshotType = policy.defaultFormat,
        transparent: Boolean = false,
        block: (ImageAssets) -> String,
    ): ByteArray {
        check(!closed) { "PanelRenderContext 已结束，不能重复渲染" }

        val svg = try {
            block(assets)
        } catch (t: Throwable) {
            fail(t)
            throw t
        }

        val bytes = try {
            renderer.render(svg, format, lease.resourceUrls(), transparent)
        } catch (t: Throwable) {
            fail(t)
            throw t
        }

        try {
            lease.releaseNow()
            closed = true
        } catch (t: Throwable) {
            fail(t)
            throw t
        }
        return bytes
    }

    /**
     * 用于直接产出图片字节的面板（Epsilon 动画 WebP、Epsilon2/Zeta 的 JPEG）。
     * 成功 [RenderResourceLease.releaseNow]，异常/超时 [RenderResourceLease.close]。
     */
    fun <T> renderBytes(block: (PanelRenderContext) -> T): T {
        check(!closed) { "PanelRenderContext 已结束，不能重复渲染" }
        return try {
            val value = block(this)
            lease.releaseNow()
            closed = true
            value
        } catch (t: Throwable) {
            fail(t)
            throw t
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        lease.close()
    }

    private fun fail(cause: Throwable) {
        if (closed) return
        closed = true
        try {
            lease.close()
        } catch (e: Throwable) {
            cause.addSuppressed(e)
        }
    }
}
