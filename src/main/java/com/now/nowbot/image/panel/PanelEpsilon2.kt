package com.now.nowbot.image.panel

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.browser.ScreenshotOptions
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.font.FontFace
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.OsuUser
import java.util.Locale
import kotlin.math.roundToInt

object PanelEpsilon2 {
    private const val WIDTH = 332
    private const val HEIGHT = 87

    fun toSvg(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = toSvg(user, assets(lease, remote))

    fun toSvg(user: OsuUser, assets: ImageAssets): String {
        val avatar = assets.resolveRef(user.avatarUrl) ?: assets.local("avatar-guest.png")
        val background = assets.resolveRef(user.coverUrl) ?: assets.localBanner()
        val nameText = user.username.ifBlank { "Unknown" }
        val ascii = Texts.isAscii(nameText)
        val nameFont = if (ascii) Fonts.tahomaRegular else Fonts.puHuiTi
        val nameSize = if (ascii) 20.0 else 18.0
        val name = shadowPath(
            nameFont,
            nameFont.cutStringTail(nameText, nameSize, 235.0, true),
            90.0,
            22.0,
            nameSize,
        )
        val ppText = if (user.pp > 0) {
            "pp: " + String.format(Locale.US, "%,d", user.pp.roundToInt()) + " "
        } else {
            ""
        }
        val ppScoreText = ppText + "Score:" + Numbers.round(user.statistics?.totalScore ?: 0L, 1, -1).lowercase(Locale.ROOT)
        val ppScore = shadowPath(Fonts.tahomaRegular, ppScoreText, 89.0, 40.0, 14.0)
        val accuracyText = "Accuracy:" + Numbers.round(user.accuracy, 2) + "%"
        val accuracy = shadowPath(Fonts.tahomaRegular, accuracyText, 88.0, 56.0, 14.0)
        val levelText = "Lv" + user.levelCurrent
        val level = shadowPath(Fonts.tahomaRegular, levelText, 89.0, 72.0, 14.0)
        val levelProgress = 195.0 * (user.levelProgress.toDouble() / 100.0)
        val rankColor = if (user.globalRank <= 1000) "#ffff00" else "#fff"
        val rank = Fonts.tahomaRegular.textPath(
            "#" + user.globalRank,
            324.0,
            66.0,
            48.0,
            "right baseline",
            rankColor,
            0.3,
        )
        val mode = assets.local(modeFile(user))?.let {
            Svg.image(291.0, 9.0, 33.0, 33.0, it, 0.3)
        }

        var svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$WIDTH\" height=\"$HEIGHT\" ")
            append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 $WIDTH $HEIGHT\">")
            append("<defs>")
            append("<clipPath id=\"clippath-avatar\"><rect x=\"5\" y=\"6\" width=\"75\" height=\"75\" style=\"fill: none;\"/></clipPath>")
            append("<clipPath id=\"clippath-progress\"></clipPath>")
            append("<linearGradient id=\"base-gradient\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">")
            append("<stop offset=\"10%\" stop-color=\"#3A3F57\"/><stop offset=\"90%\" stop-color=\"#0C0E1A\"/>")
            append("</linearGradient>")
            append("</defs>")
            append("<g id=\"Base\">")
            append("<rect x=\"0\" y=\"0\" width=\"$WIDTH\" height=\"$HEIGHT\" fill=\"url(#base-gradient)\"/>")
            append("<rect x=\"5\" y=\"6\" width=\"75\" height=\"75\" fill=\"#B0F1BB\"/>")
            append("</g>")
            append("<g id=\"Background\"></g>")
            append("<g id=\"Progress\" style=\"clip-path: url(#clippath-progress);\">")
            append("<rect x=\"127\" y=\"70\" width=\"$levelProgress\" height=\"7\" rx=\"3.5\" ry=\"3.5\" style=\"fill: #B0F1BB;\"/>")
            append("</g>")
            append("<g id=\"Avatar\" style=\"clip-path: url(#clippath-avatar);\"></g>")
            append("<g id=\"Text\"></g>")
            append("<g id=\"Overlay\">")
            append("<rect x=\"4.5\" y=\"5.5\" width=\"76\" height=\"76\" rx=\"1\" ry=\"0\" fill=\"none\" stroke=\"#B0F1BB\" stroke-width=\"1\"/>")
            append("<rect x=\"126.5\" y=\"69.5\" width=\"196\" height=\"8\" rx=\"4.5\" ry=\"3.5\" fill=\"none\" stroke=\"#fff\" stroke-width=\"1\"/>")
            append("</g>")
            append("</svg>")
        }
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 332.0, 87.0, it, 0.5) }, "<g id=\"Background\">")
        svg = Svg.setText(svg, avatar?.let { Svg.image(5.0, 6.0, 75.0, 75.0, it, 1.0, "xMidYMid slice") }, "<g id=\"Avatar\">")
        svg = Svg.setTexts(svg, listOf(name, ppScore, accuracy, level, mode, rank), "<g id=\"Text\">")
        return svg
    }

    fun renderImage(
        user: OsuUser,
        lease: RenderResourceLease,
        remote: RemoteImageSource?,
        htmlRenderer: HtmlRenderer,
        outputFormat: ScreenshotType = ScreenshotType.JPEG,
    ): ByteArray {
        val svg = toSvg(user, lease, remote)
        return htmlRenderer.render(
            html(svg),
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

    private fun shadowPath(
        font: FontFace,
        text: String,
        x: Double,
        y: Double,
        size: Double,
    ): String =
        font.textPath(text, x + 1.0, y + 1.0, size, "left baseline", "#000") +
            font.textPath(text, x, y, size, "left baseline", "#fff")

    private fun modeFile(user: OsuUser): String =
        when (GameModes.getGameMode(user.mode, 1)) {
            "o" -> "mode-osu-small.png"
            "t" -> "mode-taiko-small.png"
            "c" -> "mode-fruits-small.png"
            "m" -> "mode-mania-small.png"
            else -> "mode-osu-small.png"
        }

    private fun assets(lease: RenderResourceLease, remote: RemoteImageSource?): ImageAssets =
        ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = RenderReport())

    private fun html(svg: String): String =
        "<!doctype html><html><head><meta charset=\"utf-8\"><style>" +
            "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}" +
            "svg{display:block}</style></head><body>$svg</body></html>"
}
