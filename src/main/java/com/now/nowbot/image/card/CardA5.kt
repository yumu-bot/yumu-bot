package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import kotlin.math.floor
import kotlin.math.pow

object CardA5 {
    fun render(beatmapset: Beatmapset, assets: ImageAssets): String {
        var svg = TEMPLATE
        val labelCount = beatmapset.beatmaps.orEmpty().size
        if (labelCount <= 6) {
            val labelWidth = if (labelCount == 0) 0.0 else (1360.0 / labelCount) - 10.0
            val m1 = beatmapset.beatmaps.orEmpty().map { labelM1(it, labelWidth, false, assets) }
            val m2 = beatmapset.beatmaps.orEmpty().map { labelM2(beatmapset.creatorID, it.mapperID, assets) }
            val m3 = beatmapset.beatmaps.orEmpty().map { searchDiff2LabelM3(it, assets) }
            val strings = mutableListOf<String>()
            for (i in 0 until labelCount) {
                strings += Svg.group(10.0 + (labelWidth + 10.0) * i, 150.0, m1[i])
                strings += Svg.group(10.0 + (labelWidth + 10.0) * i + labelWidth / 2 - 50.0, 10.0, labelM2Svg(m2[i]))
                strings += Svg.group(10.0 + (labelWidth + 10.0) * i + labelWidth / 2 + 5.0, 120.0, labelM3Svg(m3[i]))
            }
            svg = Svg.setText(svg, strings.joinToString("\n"), MARK_LABEL)
        } else if (labelCount <= 18) {
            val slotCount = floor((labelCount - 7) / 2.0).toInt() + 1
            val normalCount = 6 - slotCount
            val compactCount = labelCount - normalCount
            val compactX = slotCount
            val compactY = compactCount / compactX
            val remain = compactCount - compactX * compactY
            val labelWidth = 650.0 / 3.0
            val remainWidth = if (remain > 0) compactX * (labelWidth + 10.0) / remain - 10.0 else 0.0
            val beatmaps = beatmapset.beatmaps.orEmpty()
            val cn = beatmaps.slice(0 until (compactCount - remain).coerceAtLeast(0))
            val cr = beatmaps.slice((compactCount - remain).coerceAtLeast(0) until compactCount.coerceAtMost(beatmaps.size))
            val nm = beatmaps.drop(compactCount.coerceAtMost(beatmaps.size))
            val m1c = cn.map { labelM1(it, labelWidth, true, assets) }
            val m1r = cr.map { labelM1(it, remainWidth, true, assets) }
            val m1s = nm.map { labelM1(it, labelWidth, false, assets) }
            val m2s = nm.map { labelM2(beatmapset.creatorID, it.mapperID, assets) }
            val m3s = nm.map { searchDiff2LabelM3(it, assets) }
            val strings = mutableListOf<String>()
            for (ly in 0 until compactY) {
                for (lx in 0 until compactX) {
                    strings += Svg.group(10.0 + (labelWidth + 10.0) * lx, 150.0 - 60.0 * ly, m1c[lx + ly * compactX])
                }
            }
            for (mx in 0 until remain) {
                strings += Svg.group(10.0 + (remainWidth + 10.0) * mx, 150.0 - 60.0 * compactY, m1r[mx])
            }
            for (n in 0 until normalCount) {
                val nx = n + slotCount
                strings += Svg.group(10.0 + (labelWidth + 10.0) * nx, 150.0, m1s[n])
                strings += Svg.group(10.0 + (labelWidth + 10.0) * nx + labelWidth / 2 - 50.0, 10.0, labelM2Svg(m2s[n]))
                strings += Svg.group(10.0 + (labelWidth + 10.0) * nx + labelWidth / 2 + 5.0, 120.0, labelM3Svg(m3s[n]))
            }
            svg = Svg.setText(svg, strings.joinToString("\n"), MARK_LABEL)
            svg = Svg.setText(svg, Fonts.torus.textPath("Total: ${labelCount}x", 20.0, 21.0, 18.0, "left baseline", "#fff"), MARK_TEXT)
        } else {
            val labelWidth = 650.0 / 3.0
            val m1c = beatmapset.beatmaps.orEmpty().take(18).map { labelM1(it, labelWidth, true, assets) }
            val strings = mutableListOf<String>()
            for (ly in 0 until 3) {
                for (lx in 0 until 6) {
                    val index = lx + ly * 3
                    if (index < m1c.size) strings += Svg.group(10.0 + (labelWidth + 10.0) * lx, 150.0 - 60.0 * ly, m1c[index])
                }
            }
            svg = Svg.setText(svg, strings.joinToString("\n"), MARK_LABEL)
            svg = Svg.setText(svg, Fonts.torus.textPath("Diffs: ${labelCount}x", 20.0, 20.0, 18.0, "left baseline", "#fff"), MARK_TEXT)
        }
        val background = assets.mapBackground(beatmapset, "list@2x")
        return Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 1370.0, 210.0, it, 0.5) }, MARK_BACKGROUND)
    }

    private fun labelM1(beatmap: Beatmap, maxWidth: Double, hasAvatar: Boolean, assets: ImageAssets): String {
        var svg = """
            <clipPath id="clippath-LM1">
              <circle cx="26" cy="25" r="18" style="fill: none;"/>
            </clipPath>
            <g id="RRect_LM1">
            </g>
            <g id="Icon_LM1">
            </g>
            <g id="Avatar_LM1" style="clip-path: url(#clippath-LM1);">
            </g>
            <g id="Text_LM1">
            </g>
        """.trimIndent()
        val diffName = Fonts.torusBold.cutStringTail(GameModes.getKeyDifficulty(beatmap), 18.0, maxWidth - 50.0 - 10.0, true)
        val diffPath = Fonts.torusBold.textPath(diffName, 50.0, 20.0, 18.0, "left baseline", "#fff")
        val srNumber = Numbers.floorParts(beatmap.starRating, 2)
        val starWidth = Fonts.torusBold.width(srNumber.integer, 24.0) + Fonts.torusBold.width(srNumber.decimal, 18.0)
        val starPath = Fonts.torusBold.get2SizeTextPath(
            srNumber.integer, srNumber.decimal, 24.0, 18.0, 50.0, 42.0, "left baseline", "#fff",
        )
        val modeColor = Colors.getStarRatingColor(beatmap.starRating)
        var modePath = Fonts.extra.textPath(
            GameModes.getGameMode(beatmap.modeInt ?: beatmap.mode.safeModeValue.toInt(), -1),
            8.0, 38.5, 38.0, "left baseline", modeColor,
        )
        if (hasAvatar) {
            modePath = ""
            val avatar = assets.remote("https://a.ppy.sh/${beatmap.mapperID}") ?: assets.local("avatar-guest.png")
            svg = Svg.setText(svg, avatar?.let { Svg.image(8.0, 7.0, 36.0, 36.0, it) }, "<g id=\"Avatar_LM1\" style=\"clip-path: url(#clippath-LM1);\">")
        }
        svg = Svg.setTexts(svg, listOf(diffPath, starPath, modePath), "<g id=\"Text_LM1\">")

        val sr = Numbers.floorParts(beatmap.starRating, 1, 3)
        var srB = sr.integer
        var srMScale = (sr.decimal.takeIf { it.isNotEmpty() }?.let { "0.$it".toDoubleOrNull() } ?: 0.0).pow(0.8)
        if ((srB.toDoubleOrNull() ?: 0.0) >= 10.0) {
            srB = "10"
            srMScale = 0.0
        }
        if (maxWidth <= 262.0 && (srB.toDoubleOrNull() ?: 0.0) >= 7.0) {
            srB = "7"
            srMScale = 0.0
            svg = Svg.setText(svg, Fonts.torusBold.textPath("...", maxWidth - 18.0, 44.0, 18.0, "right baseline", "#BE2CFA"), "<g id=\"Icon_LM1\">")
        }
        val srOffset = starWidth + 50.0 + 4.0
        val srBValue = srB.toIntOrNull() ?: 0
        for (i in 1..srBValue) {
            val star = "<g style=\"clip-path: url(#clippath-PE-R$i);\">" +
                "<image id=\"M1Label${i}Star\" width=\"18\" height=\"18\" transform=\"translate(${15.0 * (i - 1) + srOffset} 26)\" xlink:href=\"${assets.local("object-beatmap-star.png").orEmpty()}\"/></g>"
            svg = Svg.setText(svg, star, "<g id=\"Icon_LM1\">")
        }
        val starM = "<g style=\"clip-path: url(#clippath-PE-R${srBValue + 1});\">" +
            "<image id=\"M1Label${srBValue + 1}Star\" width=\"18\" height=\"18\" transform=\"translate(${15.0 * srBValue + srOffset} 26) " +
            "translate(${9.0 * (1.0 - srMScale)} ${9.0 * (1.0 - srMScale)}) scale(${srMScale})\" xlink:href=\"${assets.local("object-beatmap-star.png").orEmpty()}\"/></g>"
        svg = Svg.setText(svg, starM, "<g id=\"Icon_LM1\">")
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, maxWidth, 50.0, 25.0, modeColor, 0.15), "<g id=\"RRect_LM1\">")
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, maxWidth, 50.0, 25.0, "#46393F"), "<g id=\"RRect_LM1\">")
        return svg
    }

    private fun labelM2(hostID: Long, guestID: Long, assets: ImageAssets): LabelM2 {
        val id = guestID
        val avatar = assets.remote("https://a.ppy.sh/$id") ?: assets.local("avatar-guest.png")
        val isHost = hostID == id && id > 0
        val host = if (isHost) "H" else "G"
        val hostColor = if (isHost) "#B8D3E0" else "#382E32"
        val rrectColor = if (isHost) "#382E32" else "#B8D3E0"
        return LabelM2(host, hostColor, rrectColor, avatar)
    }

    private fun labelM2Svg(data: LabelM2): String {
        var svg = """
              <defs>
                <clipPath id="clippath-LM2">
                  <circle cx="50" cy="50" r="50" style="fill: none;"/>
                </clipPath>
              </defs>
              <g id="RRect_LM2">
              </g>
              <g id="Avatar_LM2">
                <circle cx="50" cy="50" r="50" style="fill: #46393f;"/>
                <g style="clip-path: url(#clippath-LM2);">
                </g>
              </g>
              <g id="Bid_LM2">
              </g>
              <g id="Label_LM2">
              </g>
              <g id="Text_LM2">
              </g>
        """.trimIndent()
        svg = Svg.setText(svg, data.avatar?.let { Svg.image(0.0, 0.0, 100.0, 100.0, it) }, "<g style=\"clip-path: url(#clippath-LM2);\">")
        svg = Svg.setText(svg, Fonts.torusBold.textPath(data.host, 15.0, 15.877, 18.0, "center baseline", data.hostColor), "<g id=\"Label_LM2\">")
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 30.0, 20.0, 10.0, data.rrectColor), "<g id=\"Bid_LM2\">")
        return svg
    }

    private fun labelM3Svg(data: LabelM3): String {
        var svg = "<g id=\"Icon_LM3\">\n</g>\n<g id=\"Text_LM3\">\n</g>"
        val labelWidth = 66.0
        val interval = 4.0
        val hasLabel2 = data.label2.iconTitle.isNotEmpty()
        val label1X = if (hasLabel2) -interval - (labelWidth * 3.0 / 2.0) else -interval / 2.0 - labelWidth
        val label2X = -(labelWidth / 2.0)
        val label3X = if (hasLabel2) interval + labelWidth / 2.0 else interval / 2.0
        val text1 = Fonts.torusBold.get2SizeTextPath(data.label1.dataB, data.label1.dataM, 24.0, 18.0, 30.0 + label1X, 20.0, "left baseline", "#fff")
        val text2 = Fonts.torusBold.get2SizeTextPath(data.label2.dataB, data.label2.dataM, 24.0, 18.0, 30.0 + label2X, 20.0, "left baseline", "#fff")
        val text3 = Fonts.torusBold.get2SizeTextPath(data.label3.dataB, data.label3.dataM, 24.0, 18.0, 30.0 + label3X, 20.0, "left baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(text1, text2, text3), "<g id=\"Text_LM3\">")
        svg = Svg.setText(svg, data.label1.icon?.let { Svg.image(label1X, 0.0, 25.0, 25.0, it) }, "<g id=\"Icon_LM3\">")
        svg = Svg.setText(svg, data.label2.icon?.let { Svg.image(label2X, 0.0, 25.0, 25.0, it) }, "<g id=\"Icon_LM3\">")
        svg = Svg.setText(svg, data.label3.icon?.let { Svg.image(label3X, 0.0, 25.0, 25.0, it) }, "<g id=\"Icon_LM3\">")
        return svg
    }

    private fun searchDiff2LabelM3(beatmap: Beatmap, assets: ImageAssets): LabelM3 {
        val cs = Numbers.floorParts(beatmap.cs, 1)
        val ar = Numbers.floorParts(beatmap.ar, 1)
        val od = Numbers.floorParts(beatmap.od, 1)
        val hp = Numbers.floorParts(beatmap.hp, 1)
        return when (GameModes.getGameMode(beatmap.modeInt ?: beatmap.mode.safeModeValue.toInt(), 1)) {
            "t" -> LabelM3(
                LabelM3Item(assets.local("object-score-overalldifficulty.png"), "OD", od.integer, od.decimal),
                LabelM3Item(null, "", "", ""),
                LabelM3Item(assets.local("object-score-healthpoint.png"), "HP", hp.integer, hp.decimal),
            )
            "c" -> LabelM3(
                LabelM3Item(assets.local("object-score-circlesize.png"), "CS", cs.integer, cs.decimal),
                LabelM3Item(assets.local("object-score-approachrate.png"), "AR", ar.integer, ar.decimal),
                LabelM3Item(assets.local("object-score-healthpoint.png"), "HP", hp.integer, hp.decimal),
            )
            "m" -> LabelM3(
                LabelM3Item(assets.local("object-score-circlesize.png"), "CS", cs.integer, cs.decimal),
                LabelM3Item(assets.local("object-score-overalldifficulty.png"), "OD", od.integer, od.decimal),
                LabelM3Item(assets.local("object-score-healthpoint.png"), "HP", hp.integer, hp.decimal),
            )
            else -> LabelM3(
                LabelM3Item(assets.local("object-score-circlesize.png"), "CS", cs.integer, cs.decimal),
                LabelM3Item(assets.local("object-score-approachrate.png"), "AR", ar.integer, ar.decimal),
                LabelM3Item(assets.local("object-score-overalldifficulty.png"), "OD", od.integer, od.decimal),
            )
        }
    }

    private data class LabelM2(val host: String, val hostColor: String, val rrectColor: String, val avatar: String?)
    private data class LabelM3Item(val icon: String?, val iconTitle: String, val dataB: String, val dataM: String)
    private data class LabelM3(val label1: LabelM3Item, val label2: LabelM3Item, val label3: LabelM3Item)

    private const val MARK_LABEL = "<g id=\"Label_CM\">"
    private const val MARK_TEXT = "<g id=\"Text_CM\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CM-1);\" filter=\"url(#blur-CM-1)\">"

    private val TEMPLATE = """
           <defs>
            <clipPath id="clippath-CM-1">
              <rect width="1370" height="210" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <filter id="blur-CM-1" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="5" result="blur"/>
            </filter>
          </defs>
          <g id="Background_CM">
            <rect width="1370" height="210" rx="20" ry="20" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CM-1);" filter="url(#blur-CM-1)">
            </g>
          </g>
          <g id="Label_CM">
          </g>
          <g id="Text_CM">
          </g>
    """.trimIndent()
}
