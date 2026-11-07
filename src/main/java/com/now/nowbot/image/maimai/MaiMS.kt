package com.now.nowbot.image.maimai

import com.now.nowbot.image.card.CardA2Data
import com.now.nowbot.image.card.CardMSData
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.diamond
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import java.util.Locale
import kotlin.math.max

object MaiMS {
    fun songToCardA2(song: MaiSong, version: String?, assets: ImageAssets): CardA2Data {
        val mai = MaiAssets(assets)
        val background = mai.cover(song.songID.toLong())
        var rawID = song.songID
        val right3b: String
        if (version == "ANY") {
            if (rawID >= 10000) rawID %= 10000
            right3b = when {
                rawID >= 1000 -> "(1)"
                rawID >= 100 -> "(10)"
                rawID >= 10 -> "(100)"
                else -> "(1000)"
            }
        } else {
            right3b = ""
        }
        val versionText = song.info.version
            .replace("でらっくす", "DX")
            .replace(" PLUS", "+")

        return CardA2Data(
            background = background,
            mapStatus = "",
            title1 = song.info.title.ifBlank { "-" },
            title2 = song.info.artist.ifBlank { "-" },
            title3 = "",
            left1 = song.alias.orEmpty(),
            left2 = versionText.ifBlank { "-" },
            left3 = MaiAssets.category(song.info.genre),
            right1 = if (song.info.bpm != 0) "BPM ${song.info.bpm}" else "",
            right2 = "songID:",
            right3b = right3b,
            right3m = rawID.toString(),
            right3bSize = 36.0,
            right3mSize = 60.0,
        )
    }

    fun scoreToCardMS(song: MaiSong, index: Int, score: MaiScore?, assets: ImageAssets): CardMSData {
        val mai = MaiAssets(assets)
        val hasScore = (score?.achievements ?: 0.0) > 0.0
        val doubleCabinet = score?.title?.contains("[協]") == true
        val double = if (doubleCabinet) 2 else 1
        val achievements = score?.achievements ?: 0.0
        val rank = MaiAssets.maimaiRankFromAchievements(achievements / double)
        val difficulty = song.star.getOrElse(index) { 0.0 }
        val maxRating = MaiAssets.maximumRating(difficulty)
        val rating = score?.rating ?: 0
        val isUtage = song.isUtage
        val diffColors = MaiAssets.difficultyColors(index, isUtage)
        val comboIcon = mai.comboIcon(score?.combo)
        val syncIcon = mai.syncIcon(score?.sync)
        val rankImage = if (hasScore) mai.rankImage(rank) else null
        val achievementsText = String.format(Locale.ROOT, "%.4f", achievements)
        val mainB = if (hasScore) achievementsText.dropLast(3) else "-"
        val mainM = if (hasScore) achievementsText.takeLast(3) else ""
        val mainL = if (hasScore) "%" else ""
        val additionalB = if (hasScore) rating.toString() else ""
        val additionalM = if (hasScore) {
            if (MaiAssets.isMaximumRating(rating, difficulty)) "" else " [$maxRating]"
        } else {
            "[$maxRating]"
        }

        val div = (score?.score ?: 0).toDouble() / ((score?.max ?: 1).toDouble()) / double
        val progress = progress(div, rating, maxRating)

        return CardMSData(
            background = mai.rankBackground(rank),
            cover = mai.cover(song.songID.toLong()),
            overlay = if (rank == "sssp") mai.overlayFoil() else null,
            type = mai.typeImage(song.type),
            icon1 = comboIcon,
            icon2 = syncIcon,
            icon3 = rankImage,
            label1 = MaiAssets.difficultyName(index, isUtage),
            label1Colors = diffColors,
            label2 = Svg.number(difficulty),
            label2Colors = diffColors,
            left = "Charter:",
            right = song.charts.getOrNull(index)?.charter ?: "-",
            title = "Achievements",
            mainB = mainB,
            mainM = mainM,
            mainL = mainL,
            additionalB = additionalB,
            additionalM = additionalM,
            rrect1Percent = Numbers.clamp(progress.percent, 1.0, 0.0),
            rrect1Color1 = progress.color1,
            rrect1Color2 = progress.color2,
            rrect1BaseOpacity = progress.baseOpacity,
            stars = drawStars(score?.score ?: 0, (score?.max ?: 0) * double),
            component = componentG1(
                song.charts.getOrNull(index)?.notes.orEmpty(),
                achievements,
                score?.combo.orEmpty(),
                doubleCabinet,
                mai,
            ),
            completed = false,
        )
    }

    fun componentG2(user: MaiBestScore.User, assets: ImageAssets): String {
        val mai = MaiAssets(assets)
        val avatar = mai.local("Maimai/avatar-guest2.png")
        val nameFont = if (Texts.isAscii(user.name)) Fonts.poppinsBold else Fonts.puHuiTi
        val text = Fonts.poppinsBold.textPath("Player", 1790.0, 193.0, 18.0, "right baseline", "#fff") +
            nameFont.textPath(user.name.orEmpty(), 1790.0, 225.0, 30.0, "right baseline", "#fff") +
            Fonts.poppinsBold.textPath(
                (user.rating ?: 0).toString(),
                1790.0, 265.0, 40.0, "right baseline", "#fff",
            )
        return buildString {
            append("<defs><clipPath id=\"clippath-LG2-1\"><circle cx=\"1850\" cy=\"225\" r=\"45\" style=\"fill: #382E32;\"/></clipPath></defs>")
            append("<g id=\"Base_LG2\">")
            append(PanelDraw.circle(1850.0, 225.0, 45.0, "#382E32", 1.0))
            append("</g>")
            append("<g id=\"Icon_LG2\" style=\"clip-path: url(#clippath-LG2-1);\">")
            append(Svg.image(1805.0, 180.0, 90.0, 90.0, avatar.orEmpty()))
            append("</g>")
            append("<g id=\"Text_LG2\">")
            append(text)
            append("</g>")
        }
    }

    private fun progress(div: Double, rating: Int, maxRating: Int): Progress {
        return when {
            div >= 0.97 -> Progress("#F7B551", "#FFF45C", (1 - div) / (1 - 0.97), 0.3)
            div >= 0.95 -> Progress("#FF554E", "#FAD129", (0.97 - div) / (0.97 - 0.95), 0.3)
            div >= 0.93 -> Progress("#FF554E", "#F28D29", (0.95 - div) / (0.95 - 0.93), 0.3)
            div >= 0.9 -> Progress("#F0EF47", "#26D94F", (0.93 - div) / (0.93 - 0.9), 0.3)
            div >= 0.85 -> Progress("#38F8D4", "#43EB81", (0.9 - div) / (0.9 - 0.85), 0.3)
            div > 0 -> Progress("#4facfe", "#00f2fe", div / 0.85, 0.1)
            else -> Progress("#4facfe", "#00f2fe", rating.toDouble() / maxRating, 0.1)
        }
    }

    private fun drawStars(dx: Int, max: Int): String {
        if (max <= 0) return ""
        val level = MaiAssets.starLevel(dx, max)
        if (level <= 0) return ""
        val color = MaiAssets.starColor(level)
        val delta = if (level >= 5) 14.0 else 16.0
        return buildString {
            for (index in 0 until level) {
                append(PanelDraw.diamond(-12.0, -12.0 - delta * index, 12.0, 12.0, color))
            }
        }
    }

    private fun componentG1(
        notes: List<Int>,
        achievements: Double,
        fc: String,
        doubleCabinet: Boolean,
        mai: MaiAssets,
    ): String {
        val tap = notes.getOrElse(0) { 0 }
        val hold = notes.getOrElse(1) { 0 }
        val slide = notes.getOrElse(2) { 0 }
        val touch = notes.getOrElse(3) { 0 }
        val breakNote = notes.getOrElse(4) { 0 }
        val baseScoreSum = (tap + 2 * hold + 3 * slide + 5 * breakNote + touch).takeIf { it != 0 } ?: -1
        val double = if (doubleCabinet) 2 else 1

        val equivalentText = when {
            fc == "app" -> "= AP+"
            fc == "ap" -> "= " + approximateGreat((double * 101 - achievements) / (0.25 / breakNote), 1) + " BREAK PF."
            achievements > 0 -> {
                val great = approximateGreat(
                    (double * 101 - achievements) / 100 / (0.2 / baseScoreSum), 1,
                )
                if (great.contains(".")) "≈ $great GR." else "= $great GR."
            }
            else -> "-"
        }
        val equivalent = Fonts.poppinsBold.textPath(equivalentText, 280.0, 20.0, 14.0, "right baseline", "#fff")
        val title = Fonts.poppinsBold.textPath("Notes & Tolerance", 10.0, 20.0, 14.0, "left baseline", "#fff")

        val tapCount = Fonts.poppinsBold.textPath(tap, 37.0, 72.0, 14.0, "center baseline", "#fff")
        val holdCount = Fonts.poppinsBold.textPath(hold, 37.0 + 54, 72.0, 14.0, "center baseline", "#fff")
        val slideCount = Fonts.poppinsBold.textPath(slide, 37.0 + 108, 72.0, 14.0, "center baseline", "#fff")
        val touchCount = Fonts.poppinsBold.textPath(touch, 37.0 + 162, 72.0, 14.0, "center baseline", "#fff")
        val breakCount = Fonts.poppinsBold.textPath(breakNote, 37.0 + 216, 72.0, 14.0, "center baseline", "#fff")

        val s3pLoss = Fonts.poppinsBold.textPath(
            "<" + approximateGreat((1.01 - 0.98) * baseScoreSum / 0.2),
            37.0, 137.0, 14.0, "center baseline", "#fff",
        )
        val s3Loss = Fonts.poppinsBold.textPath(
            "<" + approximateGreat((1.01 - 0.99) * baseScoreSum / 0.2),
            37.0 + 54, 137.0, 14.0, "center baseline", "#fff",
        )
        val s2pLoss = Fonts.poppinsBold.textPath(
            "<" + approximateGreat((1.01 - 0.995) * baseScoreSum / 0.2),
            37.0 + 108, 137.0, 14.0, "center baseline", "#fff",
        )
        val s2Loss = Fonts.poppinsBold.textPath(
            "<" + approximateGreat((1.01 - 1) * baseScoreSum / 0.2),
            37.0 + 162, 137.0, 14.0, "center baseline", "#fff",
        )
        val spLoss = Fonts.poppinsBold.textPath(
            "<" + approximateGreat((1.01 - 1.005) * baseScoreSum / 0.2),
            37.0 + 216, 137.0, 14.0, "center baseline", "#fff",
        )

        val break2550Percent = 0.01 * 0.25 / breakNote
        val break2550Text = "≈  " + judgeScore(-100 * break2550Percent) + "%  ≈  " +
            approximateGreat(break2550Percent / (0.2 / baseScoreSum), 3)
        val break2000Percent = 1.0 / baseScoreSum + (0.01 * 0.6 / breakNote)
        val break2000Text = "≈  " + judgeScore(-100 * break2000Percent) + "%  ≈  " +
            approximateGreat(break2000Percent / (0.2 / baseScoreSum), 3)

        val break2550 = Fonts.poppinsBold.textPath(break2550Text, 62.0, 162.0, 14.0, "left baseline", "#fff")
        val break2000 = Fonts.poppinsBold.textPath(break2000Text, 62.0, 192.0, 14.0, "left baseline", "#fff")

        val colors = when {
            achievements >= double * 100.5 -> ColorArray.rainbow
            achievements >= double * 100 -> ColorArray.iridescent
            achievements >= double * 99 -> ColorArray.yellow
            achievements >= double * 97 -> ColorArray.amber
            achievements >= double * 80 -> ColorArray.pink
            achievements >= double * 60 -> ColorArray.indigo
            achievements >= double * 50 -> ColorArray.green
            else -> ColorArray.deepGray
        }
        val rrectWidth = when {
            achievements >= double * 100.5 -> 270 * 0.9 + 27 * (achievements / double - 100.5) / 0.5
            achievements >= double * 100 -> 270 * 0.7 + 27 * (achievements / double - 100) / 0.5
            achievements >= double * 99.5 -> 270 * 0.5 + 27 * (achievements / double - 99.5) / 0.5
            achievements >= double * 99 -> 270 * 0.3 + 27 * (achievements / double - 99) / 0.5
            achievements >= double * 98 -> 270 * 0.1 + 27 * (achievements / double - 98)
            achievements > 0 -> max(27 * (achievements / double - 95) / 3, 10.0)
            else -> 0.0
        }
        val progressRect = if (achievements > 0) {
            PanelDraw.linearGradientRect(10.0, 80.0, rrectWidth, 10.0, 5.0, colors)
        } else {
            ""
        }
        val progressBase = PanelDraw.linearGradientRect(
            10.0, 80.0, 270.0, 10.0, 5.0, colors,
            listOf(0, 100), listOf(0, 0), 0.2,
        )

        return buildString {
            append("<g id=\"Base_LG1\">")
            append(Svg.rect(0.0, 0.0, 290.0, 210.0, 20.0, "#382E32", 1.0))
            append(progressBase)
            append(progressRect)
            append("</g>")
            append("<g id=\"Icon_LG1\">")
            append(Svg.image(17.0, 30.0, 40.0, 25.0, noteIcon(mai, "tap").orEmpty()))
            append(Svg.image(71.0, 30.0, 40.0, 25.0, noteIcon(mai, "hold").orEmpty()))
            append(Svg.image(125.0, 30.0, 40.0, 25.0, noteIcon(mai, "slide").orEmpty()))
            append(Svg.image(179.0, 30.0, 40.0, 25.0, noteIcon(mai, "touch").orEmpty()))
            append(Svg.image(233.0, 30.0, 40.0, 25.0, noteIcon(mai, "break").orEmpty()))
            append(Svg.image(16.5, 98.0, 40.0, 19.0, mai.local("Maimai/object-score-sp2.png").orEmpty()))
            append(Svg.image(67.0, 95.0, 47.0, 25.0, mai.local("Maimai/object-score-ss2.png").orEmpty()))
            append(Svg.image(121.0, 95.0, 47.0, 25.0, mai.local("Maimai/object-score-ssp2.png").orEmpty()))
            append(Svg.image(175.0, 95.0, 47.0, 25.0, mai.local("Maimai/object-score-sss2.png").orEmpty()))
            append(Svg.image(229.0, 95.0, 47.0, 25.0, mai.local("Maimai/object-score-sssp2.png").orEmpty()))
            append(Svg.image(13.0, 145.0, 47.0, 25.0, mai.local("Maimai/object-note-break-perfect.png").orEmpty()))
            append(Svg.image(13.0, 175.0, 47.0, 25.0, mai.local("Maimai/object-note-break-great.png").orEmpty()))
            append(Svg.image(35.0 + Math.round(Fonts.poppinsBold.width(break2550Text, 14.0)) + 30.0, 145.0, 47.0, 25.0, mai.local("Maimai/object-note-tap-great.png").orEmpty()))
            append(Svg.image(35.0 + Math.round(Fonts.poppinsBold.width(break2000Text, 14.0)) + 30.0, 175.0, 47.0, 25.0, mai.local("Maimai/object-note-tap-great.png").orEmpty()))
            append("</g>")
            append("<g id=\"Text_LG1\">")
            append(title)
            append(equivalent)
            append(tapCount)
            append(holdCount)
            append(slideCount)
            append(touchCount)
            append(breakCount)
            append(s3pLoss)
            append(s3Loss)
            append(s2pLoss)
            append(s2Loss)
            append(spLoss)
            append(break2550)
            append(break2000)
            append("</g>")
        }
    }

    private fun noteIcon(mai: MaiAssets, name: String): String? = mai.local("Maimai/object-note-$name.png")

    private fun approximateGreat(score: Double, level: Int = 1): String = Numbers.floor(kotlin.math.abs(score), level)

    private fun judgeScore(score: Double): String {
        val sign = if (score > 0) "+" else if (score < 0) "-" else ""
        val parts = Numbers.floorParts(kotlin.math.abs(score), 4)
        return sign + parts.integer + parts.decimal
    }

    private data class Progress(
        val color1: String,
        val color2: String,
        val percent: Double,
        val baseOpacity: Double,
    )
}
