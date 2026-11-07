package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardI3
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.ChuMultipleRenderParam
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiChuCardA1
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.chuScoreToCardI3
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import kotlin.math.ceil
import kotlin.math.max

object PanelMA2 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"
    private const val CARD_HEIGHT_TOKEN = "\${cardheight}"

    fun toSvg(
        param: ChuMultipleRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: ChuMultipleRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: ChuMultipleRenderParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val b30 = param.scores.map { CardI3.render(chuScoreToCardI3(it, assets)) }
        val n20 = param.scoresLatest.map { CardI3.render(chuScoreToCardI3(it, assets)) }
        val s10 = param.scoresSelection.map { CardI3.render(chuScoreToCardI3(it, assets)) }

        val b30Height = max(ceil(b30.size / 5.0).toInt() * 150.0 - 20.0, 0.0)
        val n20Height = max(ceil(n20.size / 5.0).toInt() * 150.0 - 20.0, 0.0)
        val s10Height = max(ceil(s10.size / 5.0).toInt() * 150.0 - 20.0, 0.0)
        val bnOffset = if (b30.isNotEmpty() && n20.isNotEmpty()) 80.0 else 0.0
        val nsOffset = if ((b30.isNotEmpty() || n20.isNotEmpty()) && s10.isNotEmpty()) 80.0 else 0.0

        val cardHeight = b30Height + bnOffset + n20Height + nsOffset + s10Height + 80.0
        val panelHeight = cardHeight + 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, Svg.number(panelHeight))
        svg = svg.replace(CARD_HEIGHT_TOKEN, Svg.number(cardHeight))

        val name = if (param.panel == "CB") {
            MaiDraw.panelName("CHUNITHM Multiple Best Scores (!ymcb)", "CB")
        } else {
            MaiDraw.panelName("CHUNITHM Multiple Scores (!ymy)", "Y")
        }
        svg = Svg.setText(svg, name, MARK_INDEX)

        val b30Bodies = b30.mapIndexed { index, card ->
            Svg.group(40.0 + 372.0 * (index % 5), 330.0 + 150.0 * (index / 5), card)
        }.joinToString("\n")
        val n20Bodies = n20.mapIndexed { index, card ->
            Svg.group(
                40.0 + 372.0 * (index % 5),
                330.0 + 150.0 * (index / 5) + b30Height + bnOffset,
                card,
            )
        }.joinToString("\n")
        val s10Bodies = s10.mapIndexed { index, card ->
            Svg.group(
                40.0 + 372.0 * (index % 5),
                330.0 + 150.0 * (index / 5) + b30Height + bnOffset + n20Height + nsOffset,
                card,
            )
        }.joinToString("\n")

        val background = buildString {
            if (b30Height > 0) {
                append(
                    com.now.nowbot.image.draw.PanelDraw.linearGradientRect(
                        0.0, 290.0, 1920.0, cardHeight, 20.0, ColorArray.darkBlue,
                        listOf(100, 0), listOf(80, 20), 1.0,
                    ),
                )
            }
            if (n20Height > 0) {
                val y = 290.0 + 40.0 + b30Height + bnOffset - 40.0
                append(
                    com.now.nowbot.image.draw.PanelDraw.linearGradientRect(
                        0.0, y, 1920.0, n20Height + nsOffset + s10Height + 80.0, 20.0,
                        ColorArray.darkRed, listOf(100, 0), listOf(80, 20), 1.0,
                    ),
                )
            }
            if (s10Height > 0) {
                val y = 290.0 + 40.0 + b30Height + bnOffset + n20Height + nsOffset - 40.0
                append(
                    com.now.nowbot.image.draw.PanelDraw.linearGradientRect(
                        0.0, y, 1920.0, s10Height + 80.0, 20.0,
                        ColorArray.darkGray, listOf(100, 0), listOf(80, 20), 1.0,
                    ),
                )
            }
        }

        val titles = buildString {
            if (b30Height > 0) {
                append(Fonts.torusBold.textPath("Best 30", 960.0, 290.0 + 40.0 - 15.0, 18.0, "center baseline"))
            }
            if (n20Height > 0) {
                append(
                    Fonts.torusBold.textPath(
                        "New 20", 960.0,
                        290.0 + 40.0 + b30Height + bnOffset - 15.0,
                        18.0, "center baseline",
                    ),
                )
            }
            if (s10Height > 0) {
                append(
                    Fonts.torusBold.textPath(
                        "Selection 10", 960.0,
                        290.0 + 40.0 + b30Height + bnOffset + n20Height + nsOffset - 15.0,
                        18.0, "center baseline",
                    ),
                )
            }
        }

        svg = Svg.setText(
            svg,
            background + titles + b30Bodies + "\n" + n20Bodies + "\n" + s10Bodies,
            MARK_CARD_I,
        )

        val cardA1 = MaiChuCardA1.render(param.user, assets)
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
