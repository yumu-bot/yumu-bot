package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.LazerScore
import kotlin.math.max
import kotlin.math.roundToLong

object CardA4 {
    private const val MARK_TEXT = "<g id=\"Text_CN_2\">"
    private const val MARK_AVATAR = "<g style=\"clip-path: url(#clippath-CN2-2);\">"
    private const val MARK_RANK = "<g id=\"Background_CN_2\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CN2-1);\" filter=\"url(#blur-CN-1)\">"
    private const val MARK_LABEL = "<g id=\"Label_CN_2\">"
    private const val MARK_MOD = "<g id=\"Mod_CN_2\">"

    fun render(
        score: LazerScore,
        viewerId: Long?,
        scoreRank: Int,
        compareScore: Long,
        isLegacy: Boolean,
        assets: ImageAssets,
    ): String {
        var svg = TEMPLATE

        val rank = if (isLegacy) score.rank else score.lazerRank
        val totalScore = if (isLegacy) {
            score.legacyScore?.takeIf { it > 0 } ?: score.score
        } else {
            score.score
        }
        val ranks = rank.replace("X", "SS").replace("H", "").toCharArray()
        val rankText = if (ranks.size == 2) {
            Fonts.torusBold.textPath(ranks[0].toString(), 884.0 - 8.0, 44.0, 40.0, "center baseline", "#2A2226", 1.0) +
                Fonts.torusBold.textPath(ranks[1].toString(), 884.0 + 8.0, 44.0, 40.0, "center baseline", "#2A2226", 0.8)
        } else {
            Fonts.torusBold.textPath(ranks.firstOrNull()?.toString() ?: "F", 884.0, 44.0, 40.0, "center baseline", "#2A2226", 1.0)
        }
        val rankColors = Colors.getRankColors(rank)

        val user = score.user
        val userID = user.userID
        val avatar = assets.remote(user.avatarUrl) ?: assets.remote("https://a.ppy.sh/$userID") ?: assets.local("avatar-guest.png")
        val cover = assets.local(GameModes.rankBackgroundFile(rank, score.passed))
        val timeDifference = TimeFormat.difference(score.endedTime, 2)

        val name = Typography.multipleTextPath(
            listOf(
                TextPart(user.username + "  ", Fonts.poppinsBold, 24.0, "#fff"),
                TextPart("[$timeDifference]", Fonts.poppinsBold, 14.0, "#fff"),
            ),
            176.0, 26.0, "left baseline",
        )
        val nameWidth = Fonts.poppinsBold.width(user.username + "  ", 24.0) +
            Fonts.poppinsBold.width("[$timeDifference]", 14.0)
        val flag = assets.countryFlag(user.countryCode, 138.0, 8.0 - 2.0, 20.0)

        val acc = score.lazerAccuracy * 100.0
        val combo = score.maxCombo
        val performance = score.pp.roundToLong()
        val deltaScore = if (compareScore - totalScore != 0L) totalScore - compareScore else 0L
        val accNumber = Numbers.roundParts(acc, 2)
        val comboIndex = when {
            rank == "XH" || rank == "X" -> " [PF]"
            score.perfectCombo -> " [FC]"
            score.ruleset.toInt() == 3 && (score.beatmap.maxCombo ?: 0) <= combo && combo > 0 -> " [FC]"
            (score.beatmap.maxCombo ?: 0) == combo && combo > 0 -> " [FC]"
            else -> ""
        }

        val n2Acc = Svg.group(138.0, 36.0, labelN2(
            assets.local("object-score-accuracy2.png"), accNumber.integer, accNumber.decimal + "%",
        ))
        val n2Combo = Svg.group(138.0 + 92.0, 36.0, labelN2(
            assets.local("object-score-combo2.png"), combo.toString(), "x$comboIndex",
        ))

        val ppText = Typography.multipleTextPath(
            listOf(
                TextPart(performance.toString(), Fonts.poppinsBold, 28.0, "#fff"),
                TextPart(" PP", Fonts.poppinsBold, 20.0, "#fff"),
            ),
            834.0, 31.0, "right baseline",
        )
        val scoreWidth = Fonts.poppinsBold.width(deltaScore.toString() + "  ", 12.0) +
            Fonts.poppinsBold.width(totalScore.toString(), 18.0)
        val deltaText = if (scoreWidth > 146.0) {
            if (deltaScore > 0) "+" + Numbers.round(deltaScore, 2) else Numbers.round(deltaScore, 2)
        } else {
            if (deltaScore > 0) "+$deltaScore" else deltaScore.toString()
        }
        val scoreText = Typography.multipleTextPath(
            listOf(
                TextPart(deltaText + "  ", Fonts.poppinsBold, 12.0, "#aaa"),
                TextPart(totalScore.toString(), Fonts.poppinsBold, 18.0, "#aaa"),
            ),
            834.0, 53.0, "right baseline",
        )

        val rankingColor: String
        val versionColors: List<String>
        if (viewerId != null && viewerId == score.userID) {
            rankingColor = "#2A2226"
            versionColors = if (score.isLazer) ColorArray.lightYellow else ColorArray.cyan
        } else if (score.isLazer) {
            rankingColor = "#FFF"
            versionColors = ColorArray.amber
        } else {
            rankingColor = "#FFF"
            versionColors = ColorArray.blue
        }
        val ranking = Fonts.poppinsBold.textPath(scoreRank.toString(), 32.0 + 2.0, 38.0, 20.0, "center baseline", rankingColor)

        val statArr = statArray(score)
        val statWidthArr = statWidthArray(score.totalHit.toDouble(), score.passed, statArr, 10.0, 325.0, 5.0)
        val statColors = statColors(score.ruleset.toInt())
        val statSvg = mutableListOf<String>()
        var widthSum = 0.0
        for (i in statArr.indices) {
            if (statArr[i] > 0) {
                statSvg += PanelDraw.linearGradientRect(
                    360.0 + widthSum, 46.0, statWidthArr[i], 10.0, 5.0, statColors[i],
                )
                widthSum += statWidthArr[i] + 5.0
            }
        }
        for (item in layoutStatTexts(statArr, statWidthArr, 360.0, 5.0, 14.0, 2.0)) {
            statSvg += Fonts.torus.textPath(item.text, item.x, 46.0 - 5.0, 14.0, "center baseline", "#fff")
        }

        val modsSvg = Mods.drawLazerMods(
            score.mods, 690.0, 3.0, 25.0, 510.0 - nameWidth, "right", 4.0, true, true, assets,
        ).svg
        val rankRRect = PanelDraw.linearGradientRect(810.0, 0.0, 105.0, 62.0, 20.0, rankColors, listOf(0, 100), listOf(20, 80), 1.0)
        val versionRRect = PanelDraw.linearGradientRect(0.0, 0.0, 65.0 + 62.0, 62.0, 20.0, versionColors, listOf(100, 0), listOf(80, 20), 1.0)

        svg = Svg.setText(svg, avatar?.let { Svg.image(65.0, 0.0, 62.0, 62.0, it) }, MARK_AVATAR)
        svg = Svg.setText(svg, cover?.let { Svg.image(65.0, 0.0, 790.0, 62.0, it, 0.2) }, MARK_BACKGROUND)
        svg = Svg.setText(svg, modsSvg, MARK_MOD)
        svg = Svg.setText(svg, statSvg.joinToString("\n"), MARK_LABEL)
        svg = Svg.setTexts(svg, listOf(rankRRect, versionRRect), MARK_RANK)
        return Svg.setTexts(
            svg,
            listOf(name, flag.orEmpty(), n2Combo, n2Acc, ppText, scoreText, ranking, rankText),
            MARK_TEXT,
        )
    }

    private fun labelN2(icon: String?, dataB: String, dataM: String): String {
        var svg = "<g id=\"Icon_LN2\">\n</g>\n<g id=\"Text_LN2\">\n</g>"
        svg = Svg.setText(svg, Fonts.poppinsBold.get2SizeTextPath(dataB, dataM, 18.0, 14.0, 26.0, 17.0, "left baseline", "#fff"), "<g id=\"Text_LN2\">")
        svg = Svg.setText(svg, icon?.let { Svg.image(0.0, 0.0, 20.0, 20.0, it) }, "<g id=\"Icon_LN2\">")
        return svg
    }

    private fun statArray(score: LazerScore): List<Int> {
        val s = score.statistics
        return when (score.ruleset.toInt()) {
            1 -> listOf(s.great, s.ok, s.miss)
            2 -> listOf(s.great, s.largeTickHit, s.smallTickHit, s.miss, s.smallTickMiss)
            3 -> listOf(s.perfect, s.great, s.good, s.ok, s.meh, s.miss)
            else -> listOf(s.great, s.ok, s.meh, s.miss)
        }
    }

    private fun statColors(mode: Int): List<List<String>> {
        return when (mode) {
            1 -> listOf(ColorArray.cyan, ColorArray.green, ColorArray.pink)
            2 -> listOf(ColorArray.cyan, ColorArray.green, ColorArray.yellow, ColorArray.pink, ColorArray.gray)
            3 -> listOf(ColorArray.rainbow, ColorArray.yellow, ColorArray.green, ColorArray.blue, ColorArray.gray, ColorArray.pink)
            else -> listOf(ColorArray.cyan, ColorArray.green, ColorArray.yellow, ColorArray.pink)
        }
    }

    private fun statWidthArray(
        statSum: Double,
        passed: Boolean,
        statArr: List<Int>,
        minWidth: Double,
        fullWidth: Double,
        interval: Double,
    ): List<Double> {
        if (statArr.isEmpty()) return emptyList()
        var remainWidth = fullWidth
        val hitSum = statArr.sum().toDouble()
        val sum = if (passed) hitSum else max(hitSum, statSum)
        var statSumCalc = sum
        val result = MutableList(statArr.size) { 0.0 }
        if (sum > 0) {
            remainWidth -= interval * (statArr.size - 1)
            statArr.forEach { if (it == 0) remainWidth += interval }
            var remainWidthCalc = remainWidth
            statArr.forEach { v ->
                if (v / sum < minWidth / remainWidth && v > 0) {
                    statSumCalc -= v
                    remainWidthCalc -= minWidth
                }
            }
            statArr.forEachIndexed { index, v ->
                result[index] = when {
                    v == 0 -> 0.0
                    v / sum < minWidth / remainWidth -> minWidth
                    else -> v / statSumCalc * remainWidthCalc
                }
            }
        }
        return result
    }

    private fun layoutStatTexts(
        statArr: List<Int>,
        statWidthArr: List<Double>,
        x: Double,
        interval: Double,
        fontSize: Double,
        minMargin: Double,
    ): List<StatText> {
        val items = mutableListOf<StatText>()
        var currentX = x
        for (i in statArr.indices) {
            val value = statArr[i]
            if (value > 0) {
                val text = value.toString()
                val width = Fonts.torus.width(text, fontSize)
                items += StatText(text, width, currentX + statWidthArr[i] / 2)
                currentX += statWidthArr[i] + interval
            }
        }
        if (items.isEmpty()) return items
        for (i in items.size - 2 downTo 0) {
            val current = items[i]
            val next = items[i + 1]
            if (current.x + current.width / 2 + minMargin > next.x - next.width / 2) {
                items[i] = current.copy(x = next.x - next.width / 2 - minMargin - current.width / 2)
            }
        }
        if (items.first().x - items.first().width / 2 < x) {
            var cursor = x
            for (i in items.indices) {
                items[i] = items[i].copy(x = cursor + items[i].width / 2)
                cursor += items[i].width + minMargin
            }
        }
        return items
    }

    private data class StatText(val text: String, val width: Double, val x: Double)

    private val TEMPLATE = """
           <defs>
            <clipPath id="clippath-CN2-1">
              <rect x="65" width="790" height="62" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CN2-2">
              <rect x="65" y="0" width="62" height="62" rx="10" ry="10" style="fill: none;"/>
            </clipPath>
            <filter id="blur-CN-1" height="120%" width="120%" x="-10%" y="-10%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="5" result="blur"/>
            </filter>
          </defs>
          <g id="Base_CN_2">
            <rect width="915" height="62" rx="20" ry="20" style="fill: #382E32;"/>
          </g>
          <g id="Background_CN_2">
            <rect x="65" width="790" height="62" rx="20" ry="20" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CN2-1);" filter="url(#blur-CN-1)">
            </g>
          </g>
          <g id="Avatar_CN_2">
            <rect x="65" width="62" height="62" rx="10" ry="10" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CN2-2);">
            </g>
          </g>
          <g id="Text_CN_2">
          </g>
          <g id="Mod_CN_2">
          </g>
          <g id="Label_CN_2">
          </g>
    """.trimIndent()
}
