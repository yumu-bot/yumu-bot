package com.now.nowbot.image.panel

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.browser.ScreenshotOptions
import com.now.nowbot.image.color.ColorWasm
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.OsuUser

object PanelZeta {
    private const val WIDTH = 848
    private const val HEIGHT = 524
    private const val FONT_FAMILY = "JetBrainsMono NF"
    private const val FONT_RESOURCE = "/static/font/draw/JetBrainsMonoNerdFont-ExtraBold.ttf"

    fun toSvg(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String {
        val assets = assets(lease, remote)
        val bannerBytes = bannerBytes(user, assets, remote)
        val avatarBytes = avatarBytes(user, assets, remote)
        val color = ColorWasm.topHex(bannerBytes)
        return toSvg(
            name = user.username,
            avatarUrl = lease.registerImage(avatarBytes).url,
            bannerUrl = lease.registerImage(bannerBytes).url,
            color = color,
        )
    }

    fun toSvg(
        name: String,
        avatarUrl: String,
        bannerUrl: String,
        color: String,
    ): String {
        var svg = Templates.read("User_Avatar_Card.svg")
        svg = svg.replace("\${obj.avatar}", Svg.escape(avatarUrl))
        svg = svg.replace("\${obj.banner}", Svg.escape(bannerUrl))
        svg = svg.replace("\${obj.color}", Svg.escape(color))
        svg = svg.replace("\${obj.name}", Svg.escape(name.ifBlank { "Unknown" }))
        return svg
    }

    fun render(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource?,
        htmlRenderer: HtmlRenderer,
        outputFormat: ScreenshotType = ScreenshotType.JPEG,
    ): ByteArray {
        val assets = assets(lease, remote)
        val bannerBytes = bannerBytes(user, assets, remote)
        val avatarBytes = avatarBytes(user, assets, remote)
        val color = ColorWasm.topHex(bannerBytes)
        val font = lease.register(fontBytes(), "font/ttf").url
        val svg = toSvg(
            name = user.username,
            avatarUrl = lease.registerImage(avatarBytes).url,
            bannerUrl = lease.registerImage(bannerBytes).url,
            color = color,
        )
        return htmlRenderer.render(
            html(svg, font),
            ScreenshotOptions(
                width = WIDTH,
                height = HEIGHT,
                format = outputFormat,
                jpegQuality = 90,
                omitBackground = outputFormat != ScreenshotType.JPEG,
                allowedResourceUrls = lease.resourceUrls(),
            ),
        )
    }

    private fun assets(lease: RenderResourceLease, remote: RemoteImageSource?): ImageAssets =
        ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = RenderReport())

    private fun bannerBytes(user: OsuUser, assets: ImageAssets, remote: RemoteImageSource?): ByteArray {
        val url = user.coverUrl.trim()
        if (url.isNotEmpty()) {
            assets.readLocal(url)?.let { return it }
            remote?.read(url)?.let { return it }
        }
        return assets.readLocal("card-default.webp")
            ?: assets.readLocal("Banner/c1.webp")
            ?: throw IllegalStateException("缺少 Zeta 背景素材: ${user.coverUrl}")
    }

    private fun avatarBytes(user: OsuUser, assets: ImageAssets, remote: RemoteImageSource?): ByteArray {
        val url = user.avatarUrl.trim()
        if (url.isNotEmpty()) {
            assets.readLocal(url)?.let { return it }
            remote?.read(url)?.let { return it }
        }
        return assets.readLocal("avatar-guest.png")
            ?: throw IllegalStateException("缺少 Zeta 头像素材: ${user.avatarUrl}")
    }

    private fun fontBytes(): ByteArray =
        PanelZeta::class.java.getResourceAsStream(FONT_RESOURCE)?.use { it.readBytes() }
            ?: throw IllegalStateException("缺少字体资源: $FONT_RESOURCE")

    private fun html(svg: String, fontUrl: String): String =
        "<!doctype html><html><head><meta charset=\"utf-8\"><style>" +
            "@font-face{font-family:'$FONT_FAMILY';src:url('${Svg.escape(fontUrl)}') format('truetype');" +
            "font-weight:800;font-style:normal;}" +
            "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}" +
            "svg{display:block;width:100%;height:100%}</style></head><body>$svg</body></html>"
}
