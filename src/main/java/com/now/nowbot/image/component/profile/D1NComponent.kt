package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers

data class D1NData(
    val playCount: Long = 0,
    val playTime: Long = 0,
    val totalHits: Long = 0,
    val deltaPlayCount: Long = 0,
    val deltaPlayTime: Long = 0,
    val deltaTotalHits: Long = 0,
    val mode: String = "osu",
    val levelCurrent: Int = 0,
    val levelProgress: Int = 0,
)

object D1NComponent {
    fun render(data: D1NData, assets: ImageAssets): String {
        var svg = """
            <defs>
            <clipPath id="clippath-CF1-1">
                <rect x="0" y="0" width="380" height="710" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CF1-2">
                <rect x="360" y="0" width="560" height="710" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <filter id="inset-shadow-CF1-1" height="150%" width="150%" x="-25%" y="-25%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="30" result="blur"/>
            </filter>
            </defs>
            <g id="Base_CF1"></g>
            <g id="Glass_CF1"><g style="clip-path: url(#clippath-CF1-1);" filter="url(#inset-shadow-CF1-1)"></g></g>
            <g id="Mascot_CF1"><g style="clip-path: url(#clippath-CF1-2);"></g></g>
            <g id="RRect_CF1"></g>
            <g id="Text_CF1"></g>
        """.trimIndent()

        val pc = Numbers.floorParts(data.playCount, -4)
        val tth = Numbers.floorParts(data.totalHits, -4)
        val pt = ProfileTime.parts(data.playTime.toDouble(), false)
        val labels = listOf(
            ProfileLabels.d1(ProfileLabelData(assets.local("object-score-combo.png"), "Play Count", "PC", pc.integer, pc.decimal)),
            ProfileLabels.d1(ProfileLabelData(assets.local("object-score-overalldifficulty.png"), "Total Hits", "TTH", tth.integer, tth.decimal)),
            ProfileLabels.d1(ProfileLabelData(assets.local("object-score-length.png"), "Play Time", "PT", pt.large, pt.small)),
        )
        labels.forEachIndexed { index, label ->
            svg = Svg.setText(svg, Svg.group(0.0, 20.0 + 240.0 * index, label), "<g id=\"Text_CF1\">")
        }

        val deltas = if (data.deltaPlayTime == 0L) {
            listOf("", "", "")
        } else {
            listOf(
                deltaPath(data.deltaPlayCount), deltaPath(data.deltaTotalHits),
                if (data.deltaPlayTime > 0) "+" + ProfileTime.full(data.deltaPlayTime.toDouble()) else "",
            )
        }
        deltas.forEachIndexed { index, delta ->
            if (delta.isNotEmpty()) {
                svg = Svg.setText(
                    svg,
                    Svg.group(0.0, 195.0 + 240.0 * index, Fonts.torus.textPath(delta, 180.0, 0.0, 18.0, "center baseline", deltaColor(index, data))),
                    "<g id=\"Text_CF1\">",
                )
            }
        }

        val mascotName = MascotAssets.name(data.mode)
        val mascot = MascotAssets.path(mascotName, assets)
        svg = Svg.setText(svg, mascot?.let { Svg.image(0.0, 0.0, 380.0, 710.0, it, 0.6) }, "<g style=\"clip-path: url(#clippath-CF1-1);\" filter=\"url(#inset-shadow-CF1-1)\">")
        svg = Svg.setText(svg, mascot?.let { Svg.image(360.0, 0.0, 560.0, 710.0, it) }, "<g style=\"clip-path: url(#clippath-CF1-2);\">")

        val nameWidth = Fonts.torus.width(mascotName, 36.0)
        val lvTextWidth = Fonts.torus.width(" Lv.", 24.0)
        val lvWidth = Fonts.torus.width(data.levelCurrent, 36.0)
        val progressWidth = Fonts.torus.width(data.levelProgress, 36.0) + Fonts.torus.width("%", 24.0)
        val mark1Width = nameWidth + lvWidth + lvTextWidth + 30.0
        val mark2Width = progressWidth + 30.0
        val mark1 = Fonts.torus.textPath(mascotName, 395.0, 50.754, 36.0, "left baseline", "#fff") +
            Fonts.torus.textPath(" Lv.", 395.0 + nameWidth, 50.754, 24.0, "left baseline", "#fff") +
            Fonts.torus.textPath(data.levelCurrent.toString(), 395.0 + nameWidth + lvTextWidth, 50.754, 36.0, "left baseline", "#fff")
        val mark2 = Fonts.torus.get2SizeTextPath(data.levelProgress.toString(), "%", 36.0, 24.0, 885.0, 50.754, "right baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(mark1, mark2), "<g id=\"Text_CF1\">")
        svg = Svg.setTexts(
            svg,
            listOf(
                PanelDraw.rect(380.0, 20.0, mark1Width, 40.0, 12.0, "#54454c", 0.7),
                PanelDraw.rect(900.0 - mark2Width, 20.0, mark2Width, 40.0, 12.0, "#54454c", 0.7),
                PanelDraw.rect(380.0, 686.0, 520.0 * data.levelProgress / 100.0, 4.0, 2.0, "#FFCC22"),
                PanelDraw.rect(380.0, 686.0, 520.0, 4.0, 2.0, "#54454c"),
            ),
            "<g id=\"RRect_CF1\">",
        )
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 920.0, 710.0, 20.0, "#382e32"), "<g id=\"Base_CF1\">")
        return svg
    }

    private fun deltaPath(value: Long): String {
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        return sign + kotlin.math.abs(value)
    }

    private fun deltaColor(index: Int, data: D1NData): String {
        val value = when (index) {
            0 -> data.deltaPlayCount
            1 -> data.deltaTotalHits
            else -> data.deltaPlayTime
        }
        return when {
            value > 0 -> "#c2e5c3"
            value < 0 -> "#ffcdd2"
            else -> "none"
        }
    }
}
