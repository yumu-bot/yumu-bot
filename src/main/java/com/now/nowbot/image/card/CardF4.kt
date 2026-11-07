package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.match.MatchShapes
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor

object CardF4 {
    fun render(
        score: com.now.nowbot.model.osu.LazerScore,
        maxCombo: Int,
        maxScore: Long,
        minScore: Long,
        compareScore: Long,
        assets: ImageAssets,
    ): String {
        val user = score.user
        val avatar = assets.remote(user.avatarUrl) ?: assets.local("avatar-guest.png")
        val skew0 = MatchShapes.calculateTan(160.0, 10.0)
        val skew32 = MatchShapes.calculateTan(160.0 - 32.0, 10.0)
        val skew70 = MatchShapes.calculateTan(160.0 - 70.0, 10.0)
        val skew100 = MatchShapes.calculateTan(160.0 - 100.0, 10.0)
        val skewAvatar = MatchShapes.calculateTan(100.0, 10.0)

        val avatarClip = MatchShapes.roundedParallelogram(skew100, 0.0, 140.0, 100.0, skewAvatar, 0.0, 5.0, "none", 1.0)
        val avatarImage = avatar?.let { "<image width=\"156\" height=\"100\" transform=\"translate(${com.now.nowbot.image.svg.Svg.number(skew100 - 2)} 0)\" href=\"${com.now.nowbot.image.svg.Svg.escape(it)}\" preserveAspectRatio=\"xMidYMid slice\"/>" }.orEmpty()
        val name = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(user.username, 30.0, 230.0),
            150.0 + skew32, 32.0, 30.0, "left baseline", "#fff", 1.0, true,
        )
        val scoreBase = MatchShapes.roundedParallelogram(150.0, 0.0, 230.0, 20.0, skew70, 0.0, 5.0, PanelColor.top(), 1.0)
        val scoreWidth = MatchShapes.normalize((score.score.takeIf { it > 0 } ?: score.legacyScore ?: 0).toDouble(), maxScore.toDouble(), (minScore - 1).toDouble(), 230.0, 20.0)
        val scoreColors = if ((score.score.takeIf { it > 0 } ?: score.legacyScore ?: 0) >= maxScore) ColorArray.iridescent else ColorArray.cyan
        val scoreRect = MatchShapes.linearGradientParallelogram(150.0, 0.0, scoreWidth, 20.0, skew70, 0.0, 5.0, scoreColors)
        val base = MatchShapes.roundedParallelogram(0.0, 0.0, 400.0, 160.0, skew0, 0.0, 5.0, PanelColor.middle(), 1.0)
        val baseClip = MatchShapes.roundedParallelogram(0.0, 0.0, 400.0, 160.0, skew0, 0.0, 5.0, "none", 1.0)
        val baseShadow = MatchShapes.shadow(base, 2.0, 2.0, 1.0, PanelColor.base())
        return "<clipPath id=\"clippath-CF3-1\">$avatarClip</clipPath>" +
            "<clipPath id=\"clippath-CF3-2\">$baseClip</clipPath>" +
            "<g>$baseShadow$scoreBase$scoreRect$name<g id=\"Avatar_CF3\" clip-path=\"url(#clippath-CF3-1)\">$avatarImage</g></g>"
    }
}
