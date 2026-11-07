package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardI3
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiCardA1
import com.now.nowbot.image.maimai.MaiCards
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiFilterRenderParam
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.maimai.readMaiFilterRenderParam
import com.now.nowbot.image.maimai.toRenderParam
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.service.messageServiceImpl.MaiFilterService
import kotlin.math.ceil

object PanelMI {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"
    private const val CARD_HEIGHT_TOKEN = "\${cardheight}"

    fun toSvg(
        param: MaiFilterService.MaiFilterParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(body, lease, remote).svg

    fun render(
        param: MaiFilterService.MaiFilterParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(param.toRenderParam(), lease, remote)

    fun render(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(readMaiFilterRenderParam(body), lease, remote)

    fun render(
        param: MaiFilterRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiFilterRenderParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val cards = param.scores.map { score ->
            CardI3.render(MaiCards.scoreToCardI3(score, assets))
        }
        val rows = ceil(cards.size / 5.0).toInt()
        val cardHeight = rows * 150.0 + 80.0 - 20.0
        val panelHeight = cardHeight + 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, Svg.number(panelHeight))
        svg = svg.replace(CARD_HEIGHT_TOKEN, Svg.number(cardHeight))

        val (name, sign) = when (param.type) {
            "fc" -> "Maimai Full Combo Scores (!ymxc)" to "XC"
            "ap" -> "Maimai All Perfect Scores (!ymxp)" to "XP"
            else -> "Maimai Multiple Scores (!ymx)" to "X"
        }
        svg = Svg.setText(svg, MaiDraw.panelName(name, sign), MARK_INDEX)

        val banner = MaiAssets(assets).randomBanner()
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )

        val cardA1 = MaiCardA1.render(param.user, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_CARD_A1)

        val cardBodies = cards.mapIndexed { index, card ->
            val x = 40.0 + (index % 5) * 372.0
            val y = 330.0 + (index / 5) * 150.0
            Svg.group(x, y, card)
        }.joinToString("\n")

        val pageText = Fonts.torusBold.textPath(
            "page: ${param.page} of ${param.maxPage}",
            960.0,
            panelHeight - 15.0,
            20.0,
            "center baseline",
            "#fff",
            0.6,
        )
        svg = Svg.setText(svg, pageText + "\n" + cardBodies, MARK_CARD_I)
        return svg
    }
}
