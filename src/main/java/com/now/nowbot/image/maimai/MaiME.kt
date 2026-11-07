package com.now.nowbot.image.maimai

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.maimai.MaiFit
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import kotlin.math.abs

object MaiME {
    private val RANK_COLORS = listOf(
        "#848484", "#5eb0ac", "#587ec3", "#5967af", "#59519e", "#d56da1",
        "#d56e74", "#d56e4b", "#dd8d52", "#e6ad5a", "#fff767", "#fff996",
        "#56b1ef", "#82cdff",
    )
    private val COMBO_COLORS = listOf("#56b1ef", "#63ae71", "#88bd6f", "#e6ad5a", "#fff767")

    fun difficultyCard(score: MaiScore, assets: ImageAssets): String =
        difficultyCard(score.star, score.difficulty, score.index, difficultyIcon(score.index), assets)

    fun difficultyCard(
        star: Double,
        label: String,
        index: Int,
        iconName: String?,
        assets: ImageAssets,
    ): String {
        val mai = MaiAssets(assets)
        val colors = listOf("#63ae71", "#88bd6f", "#fff767", "#d56e4b", "#925c9f", "#f7d8fe")
        val width = Numbers.clampToInteger(star / 15 * 460, 460.0, 20.0)
        val parts = Numbers.roundParts(star, 0)
        val texts = Typography.multipleTextPath(
            listOf(
                TextPart(parts.integer, Fonts.poppinsBold, 84.0, "#fff"),
                TextPart(parts.decimal, Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(" / " + label.uppercase(), Fonts.poppinsBold, 24.0, "#fff"),
            ),
            105.0, 88.0, "left baseline",
        )
        val icon = iconName?.let { mai.local(it) }
        val title = Fonts.poppinsBold.textPath("Difficulty", 475.0, 28.0, 18.0, "right baseline", "#fff")
        return buildString {
            append("<g id=\"Base_OE1\">")
            append(Svg.rect(0.0, 0.0, 490.0, 150.0, 20.0, "#382e32"))
            append(PanelDraw.linearGradientRect(15.0, 105.0, 460.0, 30.0, 15.0, colors, opacity = 0.2))
            append("</g>")
            append("<g id=\"Star_OE1\">")
            append(PanelDraw.linearGradientRect(15.0, 105.0, width, 30.0, 15.0, colors, opacity = 1.0))
            append("</g>")
            append("<g id=\"Text_OE1\">")
            append(Svg.image(20.0, 20.0, 66.0, 67.0, icon.orEmpty()))
            append(texts)
            append(title)
            append("</g>")
        }
    }

    fun versionCard(song: MaiSong, assets: ImageAssets): String =
        versionCard(song.level, MaiAssets(assets).versionBackground(song.info.version))

    fun versionCard(levels: List<String>, versionImage: String?): String {
        val diffs = levels.joinToString(" / ")
        val text = Fonts.poppinsBold.textPath(diffs, 475.0, 28.0, 18.0, "right baseline", "#fff") +
            Fonts.poppinsBold.textPath("Version", 15.0, 28.0, 18.0, "left baseline", "#fff")
        return buildString {
            append("<g id=\"Base_OE2\">")
            append(Svg.rect(0.0, 0.0, 490.0, 250.0, 20.0, "#382e32"))
            append(Svg.image(58.0, 42.0, 370.0, 185.0, versionImage.orEmpty()))
            append("</g>")
            append("<g id=\"Text_OE2\">")
            append(text)
            append("</g>")
        }
    }

    fun distributionCard(score: MaiScore, diff: MaiFit.DiffData, fit: Double): String {
        val level = Numbers.roundParts(fit, 2)
        val delta = fit - score.star
        val deltaText = if (fit > 0) " (${signNumber(delta)})" else ""
        val avg = Numbers.floorParts(diff.achievements, 4)
        val avgPath = Fonts.poppinsBold.get2SizeTextPath(avg.integer, avg.decimal + " %", 48.0, 30.0, 300.0, 190.0, "right baseline", "#fff")
        val distLevel = Typography.multipleTextPath(
            listOf(
                TextPart(level.integer, Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(level.decimal, Fonts.poppinsBold, 30.0, "#fff"),
                TextPart(deltaText, Fonts.poppinsBold, 20.0, "#fff"),
            ),
            300.0, 245.0, "right baseline",
        )
        val dist = PanelDraw.barChart(
            diff.distribution, x = 15.0, y = 120.0, width = 460.0, height = 80.0,
            radius = 2.0, gap = 4.0, color = RANK_COLORS, maxUndertake = 0.1,
        )
        val combo = PanelDraw.barChart(
            diff.fullComboDistribution, x = 313.0, y = 235.0, width = 162.0, height = 80.0,
            radius = 2.0, gap = 4.0, color = COMBO_COLORS, maxUndertake = 0.1,
        )
        val text = buildString {
            append(Fonts.poppinsBold.textPath("Distribution: Lv.${score.level}", 15.0, 28.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("D", 29.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("C", 63.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("B", 96.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("A", 195.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("S", 294.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("SS", 361.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("SSS", 428.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("C", 327.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("FC", 361.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("AP", 428.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Avg. Ach.", 15.0, 168.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Dist. Level", 15.0, 223.0, 18.0, "left baseline", "#fff"))
            append(avgPath)
            append(distLevel)
        }
        return buildString {
            append("<g id=\"Base_OE3\">")
            append(Svg.rect(0.0, 0.0, 490.0, 270.0, 20.0, "#382e32"))
            append("</g>")
            append("<g id=\"Dist_OE3\">")
            append(dist)
            append("</g>")
            append("<g id=\"Combo_OE3\">")
            append(combo)
            append("</g>")
            append("<g id=\"Text_OE3\">")
            append(text)
            append("</g>")
        }
    }

    fun emptyDistributionCard(title: String): String {
        val titlePath = Fonts.poppinsBold.textPath(title, 15.0, 28.0, 18.0, "left baseline", "#fff")
        return "<g id=\"Base_OE3\">${Svg.rect(0.0, 0.0, 490.0, 270.0, 20.0, "#382e32")}</g>" +
            "<g id=\"Text_OE3\">$titlePath</g>"
    }

    fun typeCard(type: String?, assets: ImageAssets): String {
        val image = MaiAssets(assets).typeImage(type)
        return "<g id=\"Base_OE4\">${Svg.rect(0.0, 0.0, 90.0, 60.0, 20.0, "#382e32")}</g>" +
            "<g id=\"Status_OE4\">${Svg.image(20.0, 5.0, 50.0, 50.0, image.orEmpty())}</g>"
    }

    fun bpmCard(bpm: Int, count: Int, assets: ImageAssets): String =
        bpmCard(bpm, count as Int?, assets)

    fun bpmCard(bpm: Int, count: Int?, assets: ImageAssets): String {
        val mai = MaiAssets(assets)
        val text = Fonts.poppinsBold.textPath(bpm.toString(), 78.0, 25.0, 16.0, "right baseline", "#fff") +
            (count?.let {
                Fonts.poppinsBold.textPath(Numbers.floor(it, 1, -1), 78.0, 47.0, 16.0, "right baseline", "#fff")
            } ?: "")
        return buildString {
            append("<g id=\"Base_OE5\">")
            append(Svg.rect(0.0, 0.0, 90.0, 60.0, 20.0, "#382e32"))
            append("</g>")
            append("<g id=\"Text_OE5\">")
            append(text)
            append(Svg.image(12.0, 10.0, 18.0, 16.0, mai.local("object-beatmap-bpm.png").orEmpty()))
            if (count != null) {
                append(Svg.image(12.0, 32.0, 18.0, 18.0, mai.local("object-beatmap-playcount.png").orEmpty()))
            }
            append("</g>")
        }
    }

    fun songCard(song: MaiSong, index: Int, assets: ImageAssets): String {
        val charter = song.charts.getOrNull(index)?.charter.orEmpty()
        return songCard(
            song.info.title,
            song.info.artist,
            charter,
            MaiAssets.category(song.info.genre),
            song.songID,
            MaiAssets(assets).cover(song.songID.toLong()),
        )
    }

    fun songCard(
        titleText: String,
        artistText: String,
        charter: String,
        categoryText: String,
        songID: Int,
        cover: String?,
    ): String {
        val titleFont = if (Texts.isAscii(titleText)) Fonts.poppinsBold else Fonts.puHuiTi
        val artistFont = if (Texts.isAscii(artistText)) Fonts.poppinsBold else Fonts.puHuiTi
        val charterFont = if (Texts.isAscii(charter)) Fonts.poppinsBold else Fonts.puHuiTi
        val title = titleFont.textPath(
            titleFont.cutStringTail(titleText, 48.0, 800.0),
            410.0, 55.0, 48.0, "center baseline", "#fff",
        )
        val artist = artistFont.textPath(
            artistFont.cutStringTail(artistText, 24.0, 800.0),
            410.0, 98.0, 24.0, "center baseline", "#fff",
        )
        val category = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(categoryText, 30.0, 760.0, true),
            410.0, 142.0, 30.0, "center baseline", "#fff",
        )
        val charterPath = charterFont.textPath(
            charterFont.cutStringTail(charter, 30.0, 240.0, true),
            20.0, 142.0, 24.0, "left baseline", "#fff",
        )
        val id = Fonts.poppinsBold.textPath(songID.toString(), 800.0, 142.0, 24.0, "right baseline", "#fff")
        return buildString {
            append("<g id=\"Base_OE6\">")
            append(Svg.rect(0.0, 0.0, 820.0, 160.0, 20.0, "#382e32"))
            append("</g>")
            append("<g id=\"Background_OE6\" style=\"clip-path: url(#clippath-OE6-1);\">")
            append(Svg.image(0.0, 0.0, 820.0, 160.0, cover.orEmpty(), 0.4))
            append("</g>")
            append("<g id=\"Labels_OE6\">")
            append(title)
            append(artist)
            append(id)
            append(charterPath)
            append(category)
            append("</g>")
        }
    }

    fun ratingCard(score: MaiScore): String =
        ratingCard(score.rating.toDouble(), MaiAssets.maximumRating(score.star).toDouble())

    fun ratingCard(rating: Double, maxRating: Double): String {
        val isPerfect = rating > maxRating - 1e-4
        val percent = if (maxRating > 0) rating / maxRating else 1.0
        val reference = if (isPerfect) {
            " / MAX RATING"
        } else {
            " / ${Numbers.round(maxRating, 0)} RATING [${Math.round(percent * 100)}%]"
        }
        val texts = Typography.multipleTextPath(
            listOf(
                TextPart(Numbers.round(rating, 2), Fonts.poppinsBold, 84.0, "#fff"),
                TextPart(reference, Fonts.poppinsBold, 24.0, "#fff"),
            ),
            20.0, 88.0, "left baseline",
        )
        val title = Fonts.poppinsBold.textPath("DX Rating", 475.0, 28.0, 18.0, "right baseline", "#fff")
        val width = percent * 460
        val progress = PanelDraw.linearGradientRect(
            15.0, 105.0, width, 30.0, 15.0,
            listOf("#4facfe", "#00f2fe"),
        )
        return buildString {
            append("<g id=\"Base_OE7\">")
            append(Svg.rect(0.0, 0.0, 490.0, 150.0, 20.0, "#382e32"))
            append(PanelDraw.linearGradientRect(15.0, 105.0, 460.0, 30.0, 15.0, listOf("#4facfe", "#00f2fe"), opacity = 0.2))
            append("</g>")
            append("<g id=\"Rect_OE7\">")
            append(progress)
            append("</g>")
            append("<g id=\"Text_OE7\">")
            append(texts)
            append(title)
            append("</g>")
        }
    }

    fun comboCard(score: MaiScore, assets: ImageAssets): String {
        val mai = MaiAssets(assets)
        val combo = when (score.combo) {
            "fc" -> "object-score-combo-fullcombo.png"
            "fcp" -> "object-score-combo-fullcomboplus.png"
            "ap" -> "object-score-combo-allperfect.png"
            "app" -> "object-score-combo-allperfectplus.png"
            else -> "object-score-combo-clear.png"
        }
        val sync = when (score.sync) {
            "sync" -> "object-score-sync-sync.png"
            "fs" -> "object-score-sync-fullsync.png"
            "fsp" -> "object-score-sync-fullsyncplus.png"
            "fsd" -> "object-score-sync-fullsyncdx.png"
            "fsdp" -> "object-score-sync-fullsyncdxplus.png"
            else -> "object-score-sync-solo.png"
        }
        return comboCard(mai.local("Maimai/$combo"), mai.local("Maimai/$sync"))
    }

    fun comboCard(comboImage: String?, syncImage: String?): String {
        return "<g id=\"Base_OE8\">${Svg.rect(0.0, 0.0, 490.0, 80.0, 20.0, "#382e32")}</g>" +
            "<g id=\"Text_OE8\">" +
            Svg.image(15.0, 15.0, 225.0, 50.0, comboImage.orEmpty()) +
            Svg.image(250.0, 15.0, 225.0, 50.0, syncImage.orEmpty()) +
            "</g>"
    }

    fun achievementCard(score: MaiScore): String =
        achievementCard(score.achievements, score.score, score.max)

    fun achievementCard(achievement: Double?, dx: Int?, max: Int?): String {
        val achievementPath = if (achievement == null) {
            Fonts.poppinsBold.textPath("-", 470.0, 62.0, 60.0, "right baseline", "#fff")
        } else {
            val parts = Numbers.floorParts(achievement, 4)
            Typography.multipleTextPath(
                listOf(
                    TextPart(parts.integer, Fonts.poppinsBold, 60.0, "#fff"),
                    TextPart(parts.decimal + " %", Fonts.poppinsBold, 36.0, "#fff"),
                ),
                470.0, 62.0, "right baseline",
            )
        }
        val content = if (dx != null && max != null) {
            val combo = Typography.multipleTextPath(
                listOf(
                    TextPart(dx.toString(), Fonts.poppinsBold, 60.0, "#fff"),
                    TextPart(" / $max", Fonts.poppinsBold, 36.0, "#fff"),
                ),
                470.0, 132.0, "right baseline",
            )
            Fonts.poppinsBold.textPath("Achievement", 15.0, 28.0, 18.0, "left baseline", "#fff") +
                Fonts.poppinsBold.textPath("DX Score", 15.0, 98.0, 18.0, "left baseline", "#fff") +
                achievementPath + combo
        } else {
            Fonts.poppinsBold.textPath("Achievement", 15.0, 28.0, 18.0, "left baseline", "#fff") +
                achievementPath
        }
        return "<g id=\"Base_OE9\">${Svg.rect(0.0, 0.0, 490.0, 150.0, 20.0, "#382e32")}</g>" +
            "<g id=\"Text_OE9\">$content</g>"
    }

    fun chartCard(chart: MaiFit.ChartData): String {
        val dist = PanelDraw.barChart(
            chart.distribution, x = 15.0, y = 120.0, width = 460.0, height = 80.0,
            radius = 2.0, gap = 4.0, color = RANK_COLORS, maxUndertake = 0.1,
        )
        val combo = PanelDraw.barChart(
            chart.fullComboDistribution, x = 313.0, y = 235.0, width = 162.0, height = 80.0,
            radius = 2.0, gap = 4.0, color = COMBO_COLORS, maxUndertake = 0.1,
        )
        val avg = Numbers.floorParts(chart.achievements, 4)
        val avgPath = Fonts.poppinsBold.get2SizeTextPath(avg.integer, avg.decimal + " %", 48.0, 30.0, 300.0, 190.0, "right baseline", "#fff")
        val dx = Fonts.poppinsBold.textPath(Math.floor(chart.score).toInt().toString(), 300.0, 245.0, 48.0, "right baseline", "#fff")
        val text = buildString {
            append(Fonts.poppinsBold.textPath("Distribution: this song", 15.0, 28.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("D", 29.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("C", 63.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("B", 96.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("A", 195.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("S", 294.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("SS", 361.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("SSS", 428.0, 138.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("C", 327.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("FC", 361.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("AP", 428.0, 252.0, 18.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Avg. Ach.", 15.0, 168.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Avg. DX Score", 15.0, 223.0, 18.0, "left baseline", "#fff"))
            append(avgPath)
            append(dx)
        }
        return "<g id=\"Base_OE10\">${Svg.rect(0.0, 0.0, 490.0, 270.0, 20.0, "#382e32")}</g>" +
            "<g id=\"Dist_OE10\">$dist</g><g id=\"Combo_OE10\">$combo</g>" +
            "<g id=\"Text_OE10\">$text</g>"
    }

    private fun difficultyIcon(index: Int): String = when (index) {
        4 -> "Maimai/object-score-difficulty-remaster.png"
        3 -> "Maimai/object-score-difficulty-master.png"
        2 -> "Maimai/object-score-difficulty-expert.png"
        1 -> "Maimai/object-score-difficulty-advanced.png"
        else -> "Maimai/object-score-difficulty-basic.png"
    }

    private fun signNumber(value: Double): String {
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        return sign + Numbers.floor(abs(value), 2)
    }
}
