package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.mappool.old.ModPool

data class CardHData(
    val background: String? = null,
    val title: String = "",
    val titleUnicode: String = "",
    val artist: String = "",
    val mapper: String = "",
    val difficulty: String = "",
    val bid: Long = 0,
    val mod: String = "NM",
    val modColor: String? = null,
    val cs: Double = 0.0,
    val ar: Double = 0.0,
    val od: Double = 0.0,
    val hp: Double = 0.0,
    val starRating: Double = 0.0,
    val modeInt: Int = 0,
    val fontTitle2: String = "PuHuiTi",
)

object CardH {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CD-1);\">"
    private const val MARK_SD_TEXT = "<g id=\"SDText\">"

    fun render(data: CardHData, assets: ImageAssets): String {
        var svg = Templates.read("Card_D.svg")

        val titleWidth = Fonts.torus.width(data.title, 36.0)
        val title2Width = 415.0 - titleWidth - 10.0
        val title2Font = if (data.fontTitle2 == "PuHuiTi") Fonts.puHuiTi else Fonts.torus
        val title2 = if (data.titleUnicode.isNotEmpty() && data.title != data.titleUnicode && title2Width > 0) {
            title2Font.cutStringTail(data.titleUnicode, 18.0, title2Width, true)
        } else {
            ""
        }

        val title = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(data.title, 36.0, 420.0),
            20.0, 33.754, 36.0, "left baseline", "#fff",
        )
        val titleUnicode = title2Font.textPath(
            title2,
            20.0 + 10.0 + titleWidth,
            if (data.fontTitle2 == "PuHuiTi") 32.0 else 33.754,
            18.0, "left baseline", "#bbb",
        )

        val artist = Fonts.torus.cutStringTail(
            data.artist,
            24.0,
            360.0 - Fonts.torus.width(" // ${data.mapper}", 24.0),
        )
        val artistAndMapper = Fonts.torus.textPath(
            "$artist // ${data.mapper}", 20.0, 65.836, 24.0, "left baseline", "#bbb",
        )

        val difficultyName = Fonts.torus.cutStringTail(
            data.difficulty,
            24.0,
            360.0 - Fonts.torus.width("[] - b${data.bid}", 24.0),
        )
        val info = Fonts.torus.textPath(
            "[$difficultyName] - b${data.bid}", 20.0, 96.836, 24.0, "left baseline", "#bbb",
        )

        val mod = Fonts.torus.textPath(data.mod, 500.0, 30.754, 36.0, "center baseline", "#fff")

        val star = Numbers.floorParts(data.starRating, 1)
        val starM = star.decimal + "*"
        val starMWidth = Fonts.torus.width(starM, 36.0)
        val starText = Fonts.torus.textPath(star.integer, 550.0 - starMWidth, 96.59, 60.0, "right baseline", "#fff") +
            Fonts.torus.textPath(starM, 550.0, 96.59, 36.0, "right baseline", "#fff")

        val modColor = data.modColor?.takeIf { isHexColor(it) } ?: Colors.getModColor(data.mod)

        val csColor = colorInSpectrum(data.cs, CS, 1)
        val arColor = colorInSpectrum(data.ar, AR, 1)
        val odColor = colorInSpectrum(data.od, OD, 1)
        val hpColor = colorInSpectrum(data.hp, HP, 1)

        var sdName1 = ""
        var sdName2 = ""
        var sdName3 = ""
        var sdCircle1 = "none"
        var sdCircle2 = "none"
        var sdCircle3 = "none"
        when (data.modeInt) {
            0 -> {
                sdName1 = "CS"; sdName2 = "AR"; sdName3 = "OD"
                sdCircle1 = csColor; sdCircle2 = arColor; sdCircle3 = odColor
            }
            1 -> {
                sdName1 = "OD"; sdName2 = "HP"
                sdCircle1 = odColor; sdCircle2 = hpColor
            }
            2 -> {
                sdName1 = "CS"; sdName2 = "AR"; sdName3 = "HP"
                sdCircle1 = csColor; sdCircle2 = arColor; sdCircle3 = hpColor
            }
            3 -> {
                sdName2 = "OD"; sdName3 = "HP"
                sdCircle2 = odColor; sdCircle3 = hpColor
            }
        }

        val sd1 = Fonts.torus.textPath(sdName1, 385.5, 77.571, 14.0, "center baseline", "#fff")
        val sd2 = Fonts.torus.textPath(sdName2, 406.5, 77.571, 14.0, "center baseline", "#fff")
        val sd3 = Fonts.torus.textPath(sdName3, 427.5, 77.571, 14.0, "center baseline", "#fff")

        svg = Svg.setTexts(svg, listOf(sd1, sd2, sd3), MARK_SD_TEXT)
        svg = Svg.setText(svg, sdCircle1, "\${sd_circle1}")
        svg = Svg.setText(svg, sdCircle2, "\${sd_circle2}")
        svg = Svg.setText(svg, sdCircle3, "\${sd_circle3}")
        svg = Svg.setTexts(svg, listOf(title, titleUnicode, artistAndMapper, info, mod, starText), MARK_TEXT)
        svg = Svg.setText(svg, modColor, "\${mod_color}")
        val background = assets.resolveRef(data.background)
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 560.0, 110.0, it, 0.4) }, MARK_BACKGROUND)
        return svg
    }

    fun fromBeatmap(beatmap: Beatmap, mod: ModPool?, assets: ImageAssets, mode: OsuMode? = null): CardHData {
        val set = beatmap.beatmapset
        return CardHData(
            background = assets.mapBackground(beatmap, "cover"),
            title = set?.title.orEmpty(),
            titleUnicode = set?.titleUnicode.orEmpty(),
            artist = set?.artist.orEmpty(),
            mapper = set?.creator.orEmpty(),
            difficulty = com.now.nowbot.image.util.GameModes.getKeyDifficulty(beatmap),
            bid = beatmap.beatmapID,
            mod = mod?.modStr ?: "NM",
            modColor = mod?.modColor,
            cs = beatmap.cs?.toDouble() ?: 0.0,
            ar = beatmap.ar?.toDouble() ?: 0.0,
            od = beatmap.od?.toDouble() ?: 0.0,
            hp = beatmap.hp?.toDouble() ?: 0.0,
            starRating = beatmap.starRating,
            modeInt = mode?.safeModeValue?.toInt() ?: beatmap.modeInt ?: beatmap.mode.safeModeValue.toInt(),
            fontTitle2 = "PuHuiTi",
        )
    }

    private fun isHexColor(value: String): Boolean {
        return Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$").matches(value)
    }

    private fun colorInSpectrum(base: Double, staff: DoubleArray, brightness: Int): String {
        val colors = when (brightness) {
            1 -> SPECTRUM_B1
            0 -> SPECTRUM_B0
            else -> SPECTRUM_B1
        }
        if (base >= staff[12]) return colors[13]
        for (i in 0 until 13) if (base < staff[i]) return colors[i]
        return colors[13]
    }

    private val CS = doubleArrayOf(1.0, 2.0, 3.0, 3.5, 4.0, 4.2, 4.6, 5.0, 5.3, 5.8, 6.2, 6.5, 7.0)
    private val AR = doubleArrayOf(4.0, 6.0, 8.0, 8.5, 9.0, 9.3, 9.7, 10.0, 10.1, 10.3, 10.5, 10.8, 11.0)
    private val OD = doubleArrayOf(3.0, 5.0, 7.0, 7.5, 8.0, 8.3, 8.7, 9.0, 9.1, 9.6, 10.0, 10.5, 11.0)
    private val HP = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 8.6, 9.3, 10.0, 10.5, 11.0)

    private val SPECTRUM_B1 = listOf(
        "#00B7EE", "#12B4B1", "#31B16C", "#7FC269",
        "#B3D465", "#FFF45C", "#F7B551", "#F19149",
        "#EC6841", "#EB6877", "#EA68A2", "#AD5DA1",
        "#8957A1", "#5F52A0",
    )
    private val SPECTRUM_B0 = listOf(
        "#00A1E9", "#009E97", "#009944", "#23AC39",
        "#8FC41F", "#FFF100", "#F39800", "#EB6101",
        "#E60013", "#E50050", "#E4007F", "#930883",
        "#601986", "#1D2088",
    )
}
