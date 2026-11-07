package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardC
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.scorelist.PanelA12Param
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.scorelist.ScoreListImages
import com.now.nowbot.image.scorelist.ScoreListLayout
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.scorelist.ScoreListRenderResult
import com.now.nowbot.image.scorelist.renderScoreList
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import kotlin.math.ceil

object PanelA12 {
    val outputFormat: ScoreListOutputFormat = ScoreListOutputFormat.DEFAULT

    fun toSvg(param: PanelA12Param, assets: ImageAssets): String {
        val cards = param.mostPlayed.mapIndexed { index, beatmap ->
            CardC.render(
                ScoreListGenerate.mostPlayed2CardC(
                    beatmap,
                    (param.page - 1) * 50 + index + 1,
                    assets,
                ),
                assets,
            )
        }
        val rowTotal = ceil(cards.size.toDouble() / 2).toInt()
        val panelHeight = ScoreListLayout.panelHeight(cards.size, 110, 2)
        var svg = ScoreListLayout.panelA4(panelHeight, panelHeight - 290)

        svg = Svg.setText(
            svg,
            ScoreListLayout.panelName(
                "Player Most Played (!yme)",
                "EM",
                "request time: ${TimeFormat.nowTimestamp()}",
            ),
            ScoreListLayout.MARK_INDEX,
        )
        val me = CardA1.render(CardA1.factory(param.user, null, assets), assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, me), ScoreListLayout.MARK_ME)
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(ScoreListImages.randomBanner(assets, param.bannerIndex)),
            ScoreListLayout.MARK_BANNER_A4,
        )

        val luckyDog = if (cards.size % 2 == 1) cards.last() else null
        val remaining = if (luckyDog != null) cards.dropLast(1) else cards
        if (luckyDog != null) {
            svg = Svg.setText(
                svg,
                ScoreListLayout.cardAt(510.0, 330.0 + (rowTotal - 1) * 150, luckyDog),
                ScoreListLayout.MARK_LIST,
            )
        }

        val bodies = remaining.mapIndexed { index, card ->
            val ix = (index + 1) % 2
            val iy = index / 2
            val x = if (ix == 1) 40.0 else 980.0
            ScoreListLayout.cardAt(x, 330.0 + iy * 150, card)
        }
        svg = Svg.setText(svg, bodies.joinToString("\n"), ScoreListLayout.MARK_LIST)

        val page = Fonts.torusBold.textPath(
            "page: ${param.page} of ${param.maxPage}",
            1920.0 / 2,
            panelHeight - 15.0,
            20.0,
            "center baseline",
            "#fff",
            0.6,
        )
        svg = Svg.setText(svg, page, ScoreListLayout.MARK_LIST)
        return svg
    }

    fun render(
        param: PanelA12Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
