package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.match.MatchCardSupport
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.LazerScore

object CardF3 {
    fun render(score: LazerScore, maxCombo: Int, compareScore: Long, assets: ImageAssets): String {
        var svg = TEMPLATE
        svg = Svg.setText(svg, MatchCardSupport.avatar(score, assets)?.let { Svg.image(15.0, 15.0, 130.0, 130.0, it) }, "<g style=\"clip-path: url(#clippath-CP2-1);\">")
        svg = Svg.setText(svg, MatchCardSupport.rankBackground(score, assets)?.let { Svg.image(0.0, 0.0, 430.0, 160.0, it, 0.6) }, "<g style=\"clip-path: url(#clippath-CP2-2);\" filter=\"url(#blur-CP2-1)\">")
        val name = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(score.user.username, 36.0, 270.0), 160.0, 48.0, 36.0, "left baseline", "#fff",
        )
        val ranking = PanelDraw.rect(15.0, 15.0, 50.0, 25.0, 12.5, MatchCardSupport.userRankColor(score.ranking ?: 0)) +
            Typography.multipleTextPath(
                listOf(
                    TextPart((score.ranking ?: 0).toString(), Fonts.poppinsBold, 24.0, "#fff"),
                    TextPart(MatchCardSupport.ordinal(score.ranking ?: 0), Fonts.poppinsBold, 16.0, "#fff"),
                ), 40.0, 36.0, "center baseline",
            )
        svg = Svg.setText(svg, MatchCardSupport.rankImage(score, assets)?.let { Svg.image(340.0, 70.0, 90.0, 90.0, it) }, "<g id=\"Text_CP2\">")
        val judgeData = MatchCardSupport.judge(score, maxCombo)
        val judge = PanelDraw.rect(15.0, 120.0, 50.0, 25.0, 12.5, judgeData.second) +
            Fonts.poppinsBold.textPath(judgeData.first, 40.0, 140.0, 22.0, "center baseline", "#fff")
        val miss = score.statistics.miss
        val missText = Fonts.poppinsBold.textPath(
            if (miss >= 1000) Numbers.floorParts(miss, 1, -1).text else miss.toString(),
            306.0, 94.0, 30.0, "right baseline", "#fff",
        )
        svg = Svg.setText(svg, assets.local("object-hit0.png")?.let { Svg.image(310.0, 66.0, 32.0, 32.0, it) }, "<g id=\"Text_CP2\">")
        val scoreNumber = Numbers.floorParts(score.score.takeIf { it > 0 } ?: (score.legacyScore ?: 0), -4)
        val scoreText = Typography.multipleTextPath(
            listOf(
                TextPart(scoreNumber.integer, Fonts.poppinsBold, 40.0, "#fff"),
                TextPart(scoreNumber.decimal, Fonts.poppinsBold, 30.0, "#fff"),
            ), 160.0, 144.0, "left baseline",
        )
        val delta = MatchCardSupport.delta(score.score.takeIf { it > 0 } ?: (score.legacyScore ?: 0), compareScore)
        val deltaText = Fonts.poppinsBold.textPath(
            delta.first + Numbers.floorParts(delta.second, -4).text, 340.0, 114.0, 18.0, "right baseline", delta.third,
        )
        svg = Svg.setText(svg, MatchCardSupport.mods(score, 160.0, 70.0 - 6.0, 40.0, 110.0, "left", assets), "<g id=\"Mod_CP2\">")
        return Svg.setTexts(svg, listOf(name, ranking, judge, missText, scoreText, deltaText), "<g id=\"Text_CP2\">")
    }

    private val TEMPLATE = """
        <defs>
        <clipPath id="clippath-CP2-1"><circle cx="80" cy="80" r="65" style="fill: none;"/></clipPath>
        <clipPath id="clippath-CP2-2"><rect x="0" y="0" rx="20" ry="20" width="430" height="160" style="fill: none;"/></clipPath>
        <filter id="inset-shadow-CP2-1" height="150%" width="150%" x="-25%" y="-25%" filterUnits="userSpaceOnUse"><feFlood flood-color="#000"/><feComposite in2="SourceGraphic" operator="out"/><feMorphology operator="dilate" radius="5" /><feGaussianBlur in="userSpaceOnUse" stdDeviation="5" result="blur"/><feComposite in2="SourceGraphic" operator="atop"/></filter>
        <filter id="blur-CP2-1" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse"><feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/></filter>
        </defs>
        <g id="Base_CP2"><rect x="0" y="0" rx="20" ry="20" width="430" height="160" style="fill: #382E32;"/><g style="clip-path: url(#clippath-CP2-2);" filter="url(#blur-CP2-1)"></g></g>
        <g id="Shadow_CP2" style="clip-path: url(#clippath-CP2-1);" filter="url(#inset-shadow-CP2-1)"><circle cx="80" cy="80" r="65" style="fill: #fff;"/><g style="clip-path: url(#clippath-CP2-1);"></g></g>
        <g id="Mod_CP2"></g><g id="Text_CP2"></g>
    """.trimIndent()
}
