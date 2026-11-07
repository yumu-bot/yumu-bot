package com.now.nowbot.image.scorelist

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardC
import com.now.nowbot.image.card.CardI4
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import kotlin.math.ceil

internal object ScoreListPanelRenderer {
    fun render(
        user: OsuUser?,
        history: OsuUser?,
        scores: List<LazerScore>,
        ranks: List<Int>,
        compact: Boolean,
        panelName: String,
        sign: String,
        requestTime: String,
        bannerIndex: Int?,
        assets: ImageAssets,
    ): String {
        if (compact) {
            return renderCompact(user, history, scores, ranks, panelName, sign, requestTime, bannerIndex, assets)
        }
        return renderFull(user, history, scores, ranks, panelName, sign, requestTime, bannerIndex, assets)
    }

    private fun renderCompact(
        user: OsuUser?,
        history: OsuUser?,
        scores: List<LazerScore>,
        ranks: List<Int>,
        panelName: String,
        sign: String,
        requestTime: String,
        bannerIndex: Int?,
        assets: ImageAssets,
    ): String {
        val cards = scores.mapIndexed { index, score ->
            CardI4.render(
                ScoreListGenerate.score2CardI4(
                    score,
                    ScoreListLayout.rankAt(ranks, index),
                    assets,
                    ScoreListImages.scoreBackground(score, "list@2x", assets),
                ),
                assets,
            )
        }
        val panelHeight = ScoreListLayout.panelHeight(cards.size, 130, 5, interval = 15)
        var svg = ScoreListLayout.panelA4(panelHeight, panelHeight - 290)

        val me = cardA1(user, history, assets, bannerIndex)
        svg = Svg.setText(svg, ScoreListLayout.panelName(panelName, sign, requestTime), ScoreListLayout.MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, me), ScoreListLayout.MARK_ME)
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(ScoreListImages.randomBanner(assets, bannerIndex)),
            ScoreListLayout.MARK_BANNER_A4,
        )

        val bodies = cards.mapIndexed { index, card ->
            val ix = index % 5
            val iy = index / 5
            ScoreListLayout.cardAt(40.0 + ix * 372, 330.0 + iy * 145, card)
        }
        svg = Svg.setText(svg, bodies.joinToString("\n"), ScoreListLayout.MARK_LIST)
        return svg
    }

    private fun cardA1(user: OsuUser?, history: OsuUser?, assets: ImageAssets, bannerIndex: Int?): String {
        val data = CardA1.factory(user, history, assets)
        return CardA1.render(
            data.copy(
                background = assets.resolveRef(data.background) ?: assets.localBanner(bannerIndex),
                avatar = assets.resolveRef(data.avatar) ?: assets.local("avatar-guest.png"),
            ),
            assets,
        )
    }

    private fun renderFull(
        user: OsuUser?,
        history: OsuUser?,
        scores: List<LazerScore>,
        ranks: List<Int>,
        panelName: String,
        sign: String,
        requestTime: String,
        bannerIndex: Int?,
        assets: ImageAssets,
    ): String {
        val cards = scores.mapIndexed { index, score ->
            CardC.render(
                ScoreListGenerate.score2CardC(
                    score,
                    ScoreListLayout.rankAt(ranks, index),
                    assets,
                    ScoreListImages.scoreBackground(score, "list@2x", assets),
                    ScoreListImages.scoreBackground(score, "cover", assets),
                ),
                assets,
            )
        }
        val rowTotal = ceil(cards.size.toDouble() / 2).toInt()
        val panelHeight = ScoreListLayout.panelHeight(cards.size, 110, 2)
        var svg = ScoreListLayout.panelA4(panelHeight, panelHeight - 290)

        val me = cardA1(user, history, assets, bannerIndex)
        svg = Svg.setText(svg, ScoreListLayout.panelName(panelName, sign, requestTime), ScoreListLayout.MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, me), ScoreListLayout.MARK_ME)
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(ScoreListImages.randomBanner(assets, bannerIndex)),
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
        return svg
    }
}
