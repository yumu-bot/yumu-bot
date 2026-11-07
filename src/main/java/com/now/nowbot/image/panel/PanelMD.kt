package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardI3
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiCardA1
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiMD
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.service.messageServiceImpl.MaiDistributionService
import kotlin.math.ceil

object PanelMD {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"
    private const val CARD_HEIGHT_TOKEN = "\${cardheight}"

    fun toSvg(
        param: MaiDistributionService.PanelMDParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: MaiDistributionService.PanelMDParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiDistributionService.PanelMDParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val standard = param.standard.map { CardI3.render(MaiMD.scoreToCardI3(it, assets)) }
        val deluxe = param.deluxe.map { CardI3.render(MaiMD.scoreToCardI3(it, assets)) }
        val standardHeight = ceil(standard.size / 5.0).toInt() * 150.0
        val deluxeHeight = ceil(deluxe.size / 5.0).toInt() * 150.0
        val deluxeOffset = if (standard.isNotEmpty() && deluxe.isNotEmpty()) 20.0 else 0.0
        val cardHeight = standardHeight + deluxeHeight + deluxeOffset + 80.0 - 15.0
        val panelHeight = cardHeight + 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, Svg.number(panelHeight))
        svg = svg.replace(CARD_HEIGHT_TOKEN, Svg.number(cardHeight))
        svg = Svg.setText(svg, MaiDraw.panelName("Maimai Distribution (!ymmd)", "MD"), MARK_INDEX)

        val standardBodies = standard.mapIndexed { index, card ->
            Svg.group(40.0 + 372.0 * (index % 5), 330.0 + 150.0 * (index / 5), card)
        }.joinToString("\n")
        val deluxeBodies = deluxe.mapIndexed { index, card ->
            Svg.group(
                40.0 + 372.0 * (index % 5),
                330.0 + 150.0 * (index / 5) + standardHeight + deluxeOffset,
                card,
            )
        }.joinToString("\n")
        svg = Svg.setText(svg, standardBodies + "\n" + deluxeBodies, MARK_CARD_I)

        val cardA1 = MaiCardA1.render(param.user, param.rating, param.count, param.size, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_CARD_A1)

        val banner = MaiAssets(assets).randomBanner()
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }
}
