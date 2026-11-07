package com.now.nowbot.image.panel

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.browser.ScreenshotOptions
import com.now.nowbot.image.anim.EpsilonAnimator
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.OsuUser

object PanelEpsilon {
    private const val WIDTH = 460

    fun toSvg(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String {
        val assets = assets(lease, remote)
        return toSvg(user, assets, includeAvatar = true)
    }

    fun toSvg(user: OsuUser, assets: ImageAssets, includeAvatar: Boolean = true): String {
        val height = height(user)
        val background = assets.local("panel-oldavatar.png")
            ?: throw IllegalStateException("缺少素材: panel-oldavatar.png")
        val avatar = if (includeAvatar) {
            avatarUrl(user, assets)
        } else {
            null
        }
        val nameText = user.username.ifBlank { "Unknown" }
        val ascii = Texts.isAscii(nameText)
        val nameFont = if (ascii) Fonts.tahomaRegular else Fonts.puHuiTi
        var nameSize = if (ascii) 60.0 else 58.0
        if (nameFont.width(nameText, 60.0) > WIDTH) nameSize -= 10.0
        val name = nameFont.textPath(
            nameFont.cutStringTail(nameText, nameSize, WIDTH.toDouble(), true),
            230.0,
            435.0,
            nameSize,
            "center baseline",
            "#000",
        )
        val group = user.groups.firstOrNull()
        val groupColor = group?.color?.takeIf { it.isNotBlank() } ?: "#000"
        val groupText = (user.title?.split(" / ")?.firstOrNull()?.trim() ?: group?.name.orEmpty())
            .replace("(Probationary)", "(Prob)")
            .replace("Nominators", "Nominator")
        val groupNameText = Fonts.tahomaBold.cutStringTail(groupText, 28.0, WIDTH.toDouble())
        val groupName = Fonts.tahomaBold.textPath(
            groupNameText,
            230.0,
            482.0,
            28.0,
            "center baseline",
            groupColor,
        )
        val groupShadow = if (Colors.hex2OKLabBrightness(groupColor) >= 0.7) {
            Fonts.tahomaBold.textPath(
                groupNameText,
                231.0,
                483.0,
                28.0,
                "center baseline",
                "#000",
            )
        } else {
            ""
        }
        val country = assets.countryFlag(
            user.countryCode.ifBlank { user.country?.code.orEmpty() },
            if (user.isSupporter) 95.0 else 207.5,
            (if (height(user) > 520) 510.0 else 465.0) - 3.0,
            30.0,
        )
        val supporter = if (user.isSupporter) {
            assets.local("object-user-supporter.png")?.let {
                Svg.image(162.0, if (height(user) > 520) 510.0 else 465.0, 200.0, 30.0, it)
            }
        } else {
            null
        }

        var svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$WIDTH\" height=\"$height\" ")
            append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 $WIDTH $height\">")
            append("<g id=\"Base\"><rect width=\"$WIDTH\" height=\"$height\" rx=\"0\" ry=\"0\" style=\"fill: #f1eefb;\"/></g>")
            append("<g id=\"Background\"></g>")
            append("<g id=\"Avatar\"></g>")
            append("<g id=\"Text\"></g>")
            append("</svg>")
        }
        svg = Svg.setText(svg, Svg.image(0.0, 0.0, 460.0, 460.0, background), "<g id=\"Background\">")
        svg = Svg.setText(
            svg,
            avatar?.let { Svg.image(70.0, 40.0, 320.0, 320.0, it, 1.0, "xMidYMid slice") },
            "<g id=\"Avatar\">",
        )
        svg = Svg.setTexts(svg, listOf(name, country, groupName, groupShadow, supporter), "<g id=\"Text\">")
        return svg
    }

    internal fun buildBackgroundSvg(user: OsuUser, assets: ImageAssets): String =
        toSvg(user, assets, includeAvatar = false)

    fun render(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource?,
        htmlRenderer: HtmlRenderer,
    ): ByteArray {
        val assets = assets(lease, remote)
        val svg = buildBackgroundSvg(user, assets)
        val height = height(user)
        val backgroundHtml = html(svg)
        val background = htmlRenderer.render(
            backgroundHtml,
            ScreenshotOptions(
                width = WIDTH,
                height = height,
                format = ScreenshotType.WEBP,
                jpegQuality = 90,
                allowedResourceUrls = lease.resourceUrls(),
            ),
        )
        val avatarBytes = avatarBytes(user, assets, remote)
            ?: throw IllegalStateException("缺少头像素材: ${user.avatarUrl}")
        return EpsilonAnimator.compose(background, avatarBytes)
    }

    fun renderAnimated(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource?,
        htmlRenderer: HtmlRenderer,
    ): ByteArray = render(user, lease, remote, htmlRenderer)

    private fun height(user: OsuUser): Int =
        if (user.groups.isNotEmpty() || user.title != null) 560 else 515

    private fun assets(lease: RenderResourceLease, remote: RemoteImageSource?): ImageAssets =
        ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = RenderReport())

    private fun avatarUrl(user: OsuUser, assets: ImageAssets): String? {
        return assets.resolveRef(user.avatarUrl) ?: assets.local("avatar-guest.png")
    }

    private fun avatarBytes(user: OsuUser, assets: ImageAssets, remote: RemoteImageSource?): ByteArray? {
        val value = user.avatarUrl.trim()
        if (value.isNotEmpty()) {
            assets.readLocal(value)?.let { return it }
            remote?.read(value)?.let { return it }
        }
        return assets.readLocal("avatar-guest.png")
    }

    private fun html(svg: String): String =
        "<!doctype html><html><head><meta charset=\"utf-8\"><style>" +
            "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}" +
            "svg{display:block}</style></head><body>$svg</body></html>"
}
