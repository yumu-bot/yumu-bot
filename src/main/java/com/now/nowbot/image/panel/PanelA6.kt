package com.now.nowbot.image.panel

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.markdown.MarkdownRenderResult
import com.now.nowbot.image.markdown.MarkdownRenderer
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser
import java.nio.file.Path

object PanelA6 {
    private const val VERSION = "v0.8.5"
    private const val VERSION_CODE = "VS"
    private const val POWERED_BY = "Yumubot"

    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_ALPHA = "<g id=\"CardAlpha\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PP-1);\">"

    fun toSvg(
        user: OsuUser?,
        markdown: String?,
        name: String = "",
        width: Int = 1840,
        height: Int = 600,
        lease: RenderResourceLease,
        markdownRenderer: MarkdownRenderer,
        remote: RemoteImageSource? = null,
        bannerIndex: Int? = null,
    ): String {
        val content = markdown?.takeIf { it.isNotEmpty() } ?: Templates.read("Panel_A6_default.md")
        val rendered = markdownRenderer.render(content, width, height, helpRoot(name))
        val markdownImage = lease.registerImage(rendered.png).url
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = RenderReport(),
        )
        return toSvg(user, rendered, markdownImage, name, assets, bannerIndex)
    }

    internal fun toSvg(
        user: OsuUser?,
        markdown: MarkdownRenderResult,
        markdownImage: String,
        name: String,
        assets: ImageAssets,
        bannerIndex: Int? = null,
    ): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_A6.svg"))
        val panelHeight = if (markdown.height > 0) 330 + markdown.height + 40 else 1080
        val cardHeight = if (markdown.height > 0) 40 + markdown.height + 40 else 790
        svg = svg
            .replace("\${panelheight}", panelHeight.toString())
            .replace("\${cardheight}", cardHeight.toString())

        val card = CardA1.render(user, null, assets, profileCard = null, bannerIndex = bannerIndex)
        val banner = assets.randomBanner(index = bannerIndex)
        val imageX = (1920 - markdown.width) / 2.0

        svg = Svg.setText(svg, panelName(name), MARK_INDEX)
        svg = Svg.setText(
            svg,
            Svg.image(
                imageX,
                330.0,
                markdown.width.toDouble(),
                markdown.height.toDouble(),
                markdownImage,
                1.0,
            ),
            MARK_CARD_ALPHA,
        )
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, card), MARK_CARD_A1)
        return svg
    }

    private fun helpRoot(name: String): Path? {
        if (name != "help") return null
        val configured = NowbotConfig.EXPORT_FILE_PATH
            ?: throw IllegalStateException("Help 素材根未配置")
        require(configured.isNotBlank()) { "Help 素材根未配置" }
        return Path.of(configured).resolve("Help")
    }

    private fun panelName(name: String): String {
        val (title, sign) = when (name) {
            "group" -> "Group Mode" to "GM"
            "service" -> "Service Count" to "SC"
            "switch" -> "Service Switch" to "SW"
            "wiki" -> "Wiki Page" to "W"
            "help" -> "Help Page" to "H"
            else -> "Markdown Page" to "MD"
        }
        return panelNameSvg(title, sign, "request time: ${TimeFormat.nowTimestamp()}")
    }

    private fun panelNameSvg(title: String, sign: String, requestTime: String): String {
        val powered = Fonts.torus.textPath(
            "powered by $POWERED_BY $VERSION $VERSION_CODE // $title",
            20.0, 26.84, 24.0, "left baseline", "#fff",
        )
        val request = Fonts.torus.textPath(requestTime, 1900.0, 26.84, 24.0, "right baseline", "#fff")
        val signPath = Fonts.torus.textPath(sign, 607.5, 83.67, 48.0, "center baseline", "#fff")
        return powered + request + signPath
    }
}
