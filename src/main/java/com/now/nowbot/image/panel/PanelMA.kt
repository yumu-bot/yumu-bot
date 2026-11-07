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
import com.now.nowbot.image.maimai.MaiMultipleRenderParam
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.MaiScoreStatsView
import com.now.nowbot.image.maimai.readMaiMultiple
import com.now.nowbot.image.maimai.toMultipleRenderParam
import com.now.nowbot.image.maimai.toRenderParam
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.service.messageServiceImpl.MaiBestScoreService
import com.now.nowbot.service.messageServiceImpl.MaiScoreService
import kotlin.math.ceil
import kotlin.math.max

object PanelMA {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"
    private const val CARD_HEIGHT_TOKEN = "\${cardheight}"

    fun toSvg(
        param: MaiMultipleRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        param: MaiBestScoreService.PanelMAParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        param: MaiScoreService.MaiScoreParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(body, lease, remote).svg

    fun render(
        param: MaiBestScoreService.PanelMAParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(param.toRenderParam(), lease, remote)

    fun render(
        param: MaiScoreService.MaiScoreParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(param.toMultipleRenderParam(), lease, remote)

    fun render(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(readMaiMultiple(body), lease, remote)

    fun render(
        param: MaiMultipleRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiMultipleRenderParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val standardCards = param.scores.map { CardI3.render(MaiCards.scoreToCardI3(it, assets)) }
        val deluxeCards = param.scoresLatest.map { CardI3.render(MaiCards.scoreToCardI3(it, assets)) }

        val standardHeight = max(ceil(standardCards.size / 5.0).toInt() * 150.0 - 20.0, 0.0)
        val deluxeHeight = max(ceil(deluxeCards.size / 5.0).toInt() * 150.0 - 20.0, 0.0)
        val deluxeOffset = if (standardCards.isNotEmpty() && deluxeCards.isNotEmpty()) 80.0 else 0.0

        val cardHeight = standardHeight + deluxeOffset + deluxeHeight + 80.0
        val panelHeight = cardHeight + 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, Svg.number(panelHeight))
        svg = svg.replace(CARD_HEIGHT_TOKEN, Svg.number(cardHeight))

        svg = Svg.setText(svg, panelName(param), MARK_INDEX)

        val standardBodies = standardCards.mapIndexed { index, card ->
            Svg.group(40.0 + 372.0 * (index % 5), 330.0 + 150.0 * (index / 5), card)
        }.joinToString("\n")
        val deluxeBodies = deluxeCards.mapIndexed { index, card ->
            Svg.group(
                40.0 + 372.0 * (index % 5),
                330.0 + 150.0 * (index / 5) + standardHeight + deluxeOffset,
                card,
            )
        }.joinToString("\n")

        val sectionBackground = buildString {
            if (standardHeight > 0 && param.panel == "MB") {
                append(
                    PanelDrawGradient.linearGradientRect(
                        0.0, 290.0, 1920.0, cardHeight, 20.0, ColorArray.darkBlue,
                        listOf(100, 0), listOf(80, 20), 1.0,
                    ),
                )
            }
            if (deluxeHeight > 0 && param.panel == "MB") {
                val y = 290.0 + 40.0 + standardHeight + deluxeOffset - 40.0
                append(
                    PanelDrawGradient.linearGradientRect(
                        0.0, y, 1920.0, deluxeHeight + 80.0, 20.0, ColorArray.darkRed,
                        listOf(100, 0), listOf(80, 20), 1.0,
                    ),
                )
            }
        }

        val sectionTitles = buildString {
            if (standardHeight > 0 && param.panel == "MB") {
                append(
                    Fonts.torusBold.textPath(
                        "Best 35", 960.0, 290.0 + 40.0 - 15.0,
                        18.0, "center baseline",
                    ),
                )
            }
            if (deluxeHeight > 0 && param.panel == "MB") {
                append(
                    Fonts.torusBold.textPath(
                        "New 15", 960.0,
                        290.0 + 40.0 + standardHeight + deluxeOffset - 15.0,
                        18.0, "center baseline",
                    ),
                )
            }
        }

        val page = if (param.panel == "MS") {
            Fonts.torusBold.textPath(
                "page: ${param.page} of ${param.maxPage}",
                960.0, panelHeight - 15.0, 20.0, "center baseline", "#fff", 0.6,
            )
        } else {
            ""
        }

        svg = Svg.setText(svg, sectionBackground + sectionTitles + page + standardBodies + "\n" + deluxeBodies, MARK_CARD_I)

        val cardA1 = MaiCardA1.render(param.user, param.statistics, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_CARD_A1)

        val banner = MaiAssets(assets).randomBanner()
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }

    private fun panelName(param: MaiMultipleRenderParam): String {
        return when (param.panel) {
            "MB" -> MaiDraw.panelName("Maimai Multiple Best Scores (!ymmb)", "MB")
            "MS" -> {
                val stats = param.statistics
                val requestTime = if (stats != null) {
                    "avg. ra.: ${Numbers.floor(stats.averageRating, 1)}" +
                        " // avg. diff.: ${Numbers.floor(stats.averageStar, 2)}" +
                        " // avg. ach.: ${Numbers.floor(stats.averageAchievement, 4)}%" +
                        " // request time: ${TimeFormat.nowTimestamp()}"
                } else {
                    "request time: ${TimeFormat.nowTimestamp()}"
                }
                MaiDraw.panelName("Maimai Scores (!ymms)", "MS", requestTime)
            }
            else -> MaiDraw.panelName("Maimai Multiple Scores (!ymx)", "X")
        }
    }

    private object PanelDrawGradient {
        fun linearGradientRect(
            x: Double,
            y: Double,
            width: Double,
            height: Double,
            radius: Double,
            colors: List<String>,
            positionX: List<Int>,
            positionY: List<Int>,
            opacity: Double,
        ): String = com.now.nowbot.image.draw.PanelDraw.linearGradientRect(
            x, y, width, height, radius, colors, positionX, positionY, opacity,
        )
    }
}
