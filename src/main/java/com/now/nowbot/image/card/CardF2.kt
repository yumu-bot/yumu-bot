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

object CardF2 {
    fun render(score: LazerScore, maxCombo: Int, compareScore: Long, assets: ImageAssets): String {
        var svg = TEMPLATE
        svg = Svg.setText(svg, MatchCardSupport.avatar(score, assets)?.let { Svg.image(65.0, 55.0, 300.0, 300.0, it) }, "<g style=\"clip-path: url(#clippath-CF1-1);\">")
        svg = Svg.setText(svg, MatchCardSupport.rankBackground(score, assets)?.let { Svg.image(0.0, 0.0, 430.0, 550.0, it, 0.6) }, "<g style=\"clip-path: url(#clippath-CF1-2);\" filter=\"url(#blur-CF1-1)\">")
        val name = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(score.user.username, 48.0, 430.0),
            215.0, 426.0, 48.0, "center baseline", "#fff",
        )
        val ranking = PanelDraw.rect(20.0, 26.0, 84.0, 48.0, 20.0, MatchCardSupport.userRankColor(score.ranking ?: 0)) +
            Typography.multipleTextPath(
                listOf(
                    TextPart((score.ranking ?: 0).toString(), Fonts.poppinsBold, 40.0, "#fff"),
                    TextPart(MatchCardSupport.ordinal(score.ranking ?: 0), Fonts.poppinsBold, 30.0, "#fff"),
                ),
                62.0, 64.0, "center baseline",
            )
        svg = Svg.setText(svg, MatchCardSupport.rankImage(score, assets)?.let { Svg.image(330.0, 10.0, 90.0, 90.0, it) }, "<g id=\"Text_CF1\">")
        val judgeData = MatchCardSupport.judge(score, maxCombo)
        val judge = PanelDraw.rect(20.0, 332.0, 84.0, 40.0, 20.0, judgeData.second) +
            Fonts.poppinsBold.textPath(judgeData.first, 62.0, 362.0, 30.0, "center baseline", "#fff")
        val miss = Fonts.poppinsBold.textPath(score.statistics.miss.toString(), 366.0, 362.0, 30.0, "right baseline", "#fff")
        svg = Svg.setText(svg, assets.local("object-hit0.png")?.let { Svg.image(372.0, 336.0, 32.0, 32.0, it) }, "<g id=\"Text_CF1\">")
        val scoreNumber = Numbers.floorParts(score.score.takeIf { it > 0 } ?: (score.legacyScore ?: 0), -4)
        val scoreText = Typography.multipleTextPath(
            listOf(
                TextPart(scoreNumber.integer, Fonts.poppinsBold, 56.0, "#fff"),
                TextPart(scoreNumber.decimal, Fonts.poppinsBold, 40.0, "#fff"),
            ),
            215.0, 490.0, "center baseline",
        )
        val delta = MatchCardSupport.delta(score.score.takeIf { it > 0 } ?: (score.legacyScore ?: 0), compareScore)
        val deltaNumber = Numbers.floorParts(delta.second, -4)
        val deltaText = Typography.multipleTextPath(
            listOf(
                TextPart(delta.first + deltaNumber.integer, Fonts.poppinsBold, 30.0, delta.third),
                TextPart(deltaNumber.decimal, Fonts.poppinsBold, 24.0, delta.third),
            ),
            215.0, 530.0, "center baseline",
        )
        svg = Svg.setText(svg, MatchCardSupport.mods(score, 215.0, 332.0 - 4.0, 46.0, 160.0, "center", assets), "<g id=\"Mod_CF1\">")
        return Svg.setTexts(svg, listOf(name, ranking, judge, miss, scoreText, deltaText), "<g id=\"Text_CF1\">")
    }

    private val TEMPLATE = """
        <defs>
        <clipPath id="clippath-CF1-1"><circle cx="215" cy="205" r="150" style="fill: none;"/></clipPath>
        <clipPath id="clippath-CF1-2"><rect x="0" y="0" rx="20" ry="20" width="430" height="550" style="fill: none;"/></clipPath>
        <filter id="inset-shadow-CF1-1" height="150%" width="150%" x="-25%" y="-25%" filterUnits="userSpaceOnUse"><feFlood flood-color="#000"/><feComposite in2="SourceGraphic" operator="out"/><feMorphology operator="dilate" radius="10" /><feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/><feComposite in2="SourceGraphic" operator="atop"/></filter>
        <filter id="blur-CF1-1" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse"><feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/></filter>
        </defs>
        <g id="Base_CF1"><rect x="0" y="0" rx="20" ry="20" width="430" height="550" style="fill: #382E32;"/><g style="clip-path: url(#clippath-CF1-2);" filter="url(#blur-CF1-1)"></g></g>
        <g id="Shadow_CF1" style="clip-path: url(#clippath-CF1-1);" filter="url(#inset-shadow-CF1-1)"><circle cx="215" cy="205" r="150" style="fill: #fff;"/><g style="clip-path: url(#clippath-CF1-1);"></g></g>
        <g id="Mod_CF1"></g><g id="Text_CF1"></g>
    """.trimIndent()
}
