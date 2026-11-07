package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardC
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.scorelist.PanelA7Param
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.scorelist.ScoreListImages
import com.now.nowbot.image.scorelist.ScoreListLayout
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.scorelist.ScoreListRenderResult
import com.now.nowbot.image.scorelist.renderScoreList
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import kotlin.math.ceil

object PanelA7 {
    val outputFormat: ScoreListOutputFormat = ScoreListOutputFormat.DEFAULT

    fun toSvg(param: PanelA7Param, assets: ImageAssets): String {
        val user = param.user
        val base = CardA1.factory(user, null, assets, param.profileCard, param.bannerIndex)
        val theoretical = if (param.pp != 0.0) param.pp else user?.pp ?: 0.0
        val delta = Numbers.jsRound(theoretical - (user?.pp ?: 0.0)).toLong()
        val meData = base.copy(
            right1 = if (delta > 1) "+${delta}PP" else "",
            right2 = "fixed \"choked S / miss <= 1%\"",
            right3b = Numbers.jsRound(theoretical).toLong().toString(),
            right3m = "PP",
        )
        val me = CardA1.render(meData, assets)

        val cards = param.scores.map { score ->
            val card = ScoreListGenerate.fixedBestScore2CardC(score, score.index, score.indexAfter, assets)
            val deltaPP = Numbers.jsRound(score.fcPP - score.score.pp).toLong()
            CardC.render(
                card.copy(
                    indexB = if (score.fcPP > 0) Numbers.jsRound(score.fcPP).toLong().toString() else "",
                    indexL = (if (deltaPP > 0) "+" else "") + deltaPP + "PP",
                    indexLSize = 24.0,
                ),
                assets,
            )
        }
        val rowTotal = ceil(cards.size.toDouble() / 2).toInt()
        val panelHeight = ScoreListLayout.panelHeight(cards.size, 110, 2, interval = 40, spacing = 40)
        var svg = ScoreListLayout.panelA4(panelHeight, panelHeight - 290)

        svg = Svg.setText(
            svg,
            ScoreListLayout.panelName(
                "BP Fixed (!ymbf)",
                "BF",
                "request time: ${TimeFormat.nowTimestamp()}",
            ),
            ScoreListLayout.MARK_INDEX,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, me), ScoreListLayout.MARK_ME)
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(ScoreListImages.banner(param.banner, assets, param.bannerIndex)),
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
        param: PanelA7Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
