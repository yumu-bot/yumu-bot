package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardMF
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiCardA1
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.maimai.readMaiFindParam
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.service.messageServiceImpl.MaiFindService

object PanelMF {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"
    private const val CARD_HEIGHT_TOKEN = "\${cardheight}"

    fun toSvg(
        param: MaiFindService.MaiFindParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        user: MaiBestScore.User? = null,
    ): String = render(param, lease, remote, user).svg

    fun toSvg(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(body, lease, remote).svg

    fun render(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(readMaiFindParam(body), lease, remote)

    fun render(
        param: MaiFindService.MaiFindParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        user: MaiBestScore.User? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, user, report), report)
    }

    fun render(
        param: MaiFindService.MaiFindParam,
        assets: ImageAssets,
        user: MaiBestScore.User? = null,
        report: RenderReport = RenderReport(),
    ): String {
        val panelHeight = MaiDraw.panelHeight(param.songs.size)
        val cardHeight = panelHeight - 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, Svg.number(panelHeight))
        svg = svg.replace(CARD_HEIGHT_TOKEN, Svg.number(cardHeight))

        val requestTime = "results count: ${param.count} // request time: ${TimeFormat.nowTimestamp()}"
        val panelName = MaiDraw.panelName("Maimai Find v2 (!ymmf)", "MF", requestTime)
        svg = Svg.setText(svg, panelName, MARK_INDEX)

        val banner = MaiAssets(assets).randomBanner()
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )

        val cardA1 = MaiCardA1.render(user, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_CARD_A1)

        val cards = param.songs.mapIndexed { index, song ->
            val x = 40.0 + (index % 4) * 470.0
            val y = 330.0 + (index / 4) * 230.0
            Svg.group(x, y, CardMF.render(song, assets))
        }.joinToString("\n")

        val page = Fonts.torusBold.textPath(
            "page: ${param.page} of ${param.maxPage}",
            960.0,
            panelHeight - 15.0,
            20.0,
            "center baseline",
            "#fff",
            0.6,
        )
        svg = Svg.setText(svg, page + "\n" + cards, MARK_CARD_I)
        return svg
    }
}
