package com.now.nowbot.image.component.e10

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
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.calculate.CalculatePerformance
import com.now.nowbot.model.calculate.FullCalculatePerformance
import com.now.nowbot.model.calculate.RosuPerformance
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object E10UserCard {
    fun render(
        user: OsuUser,
        assets: ImageAssets,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): String {
        var svg = """
            <defs>
            <clipPath id="clippath-CO1-1">
                <rect width="500" height="230" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CO1-2">
                <rect x="25" y="25" width="140" height="140" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CO1-3">
                <rect id="CountryFlag" x="185" y="78" width="60" height="44" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CO1-4">
                <rect id="TeamFlag" x="400" y="80" rx="5" ry="5" width="76" height="38" style="fill: none;"/>
            </clipPath>
        </defs>
        <g id="Background">
            <rect width="500" height="230" rx="20" ry="20" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CO1-1);">
            </g>
        </g>
        <g id="Avatar">
            <rect x="25" y="25" width="140" height="140" rx="20" ry="20" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CO1-2);">
            </g>
        </g>
        <g id="SubIcons">
            <g style="clip-path: url(#clippath-CO1-3);">
            </g>
            <g style="clip-path: url(#clippath-CO1-4);">
            </g>
        </g>
        <g id="Text">
        </g>
        """.trimIndent()

        val top1 = user.username
        val top2 = (user.countryRank.takeIf { it > 0 }?.toString()) ?: "0"
        val left = when {
            user.globalRank > 0 -> user.globalRank.toString()
            user.highestRank != null && user.highestRank!!.rank > 0 ->
                "${user.highestRank!!.rank}^ (${com.now.nowbot.image.util.TimeFormat.difference(user.highestRank!!.updatedAt)})"
            else -> "0"
        }
        val isBot = user.isBot
        val stats = user.statistics
        val level = (stats?.levelCurrent ?: 0).toInt()
        val progress = (stats?.levelProgress ?: 0).toInt()
        val right3b = if (isBot) {
            "Bot"
        } else if (user.pp > 0) {
            Numbers.jsRound(user.pp).toLong().toString()
        } else {
            ""
        }
        val right3m = if (isBot) {
            ""
        } else if (user.pp > 0) {
            "PP"
        } else if (level == 1 && progress == 0) {
            "NOT PLAYED"
        } else {
            "AFK"
        }
        val country = user.country?.code?.ifBlank { user.countryCode } ?: user.countryCode.ifBlank { "CN" }
        val background = profileCard
            ?: user.coverUrl.takeIf { it.isNotBlank() }
            ?: user.cover?.url?.takeIf { it.isNotBlank() }
            ?: assets.localBanner(bannerIndex)
        val avatar = user.avatarUrl.takeIf { it.isNotBlank() } ?: "avatar-guest.png"
        val teamUrl = user.team?.flag

        val top1Path = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(top1, 48.0, 500.0 - 10 - 185),
            185.0, 62.0, 48.0, "left baseline", "#fff",
        )
        val top2Path = Fonts.poppinsBold.get2SizeTextPath("#", top2, 18.0, 30.0, 250.0, 120.0, "left baseline", "#fff")
        val leftPath = Fonts.poppinsBold.get2SizeTextPath("#", left, 18.0, 30.0, 25.0, 214.0, "left baseline", "#fff")
        val rightPath = Fonts.poppinsBold.get2SizeTextPath(right3b, right3m, 72.0, 48.0, 480.0, 215.0, "right baseline", "#fff")

        svg = Svg.setTexts(svg, listOf(top1Path, top2Path, leftPath, rightPath), "<g id=\"Text\">")
        svg = Svg.setText(
            svg,
            assets.resolveRef(background)?.let { Svg.image(0.0, 0.0, 500.0, 230.0, it, 0.6) },
            "<g style=\"clip-path: url(#clippath-CO1-1);\">",
        )
        svg = Svg.setText(
            svg,
            assets.resolveRef(avatar)?.let { Svg.image(25.0, 25.0, 140.0, 140.0, it) },
            "<g style=\"clip-path: url(#clippath-CO1-2);\">",
        )
        svg = Svg.setText(
            svg,
            assets.countryFlag(country, 185.0, 78.0, 44.0),
            "<g style=\"clip-path: url(#clippath-CO1-3);\">",
        )
        svg = Svg.setText(
            svg,
            assets.resolveRef(teamUrl)?.let { Svg.image(405.0, 84.0, 76.0, 38.0, it, 1.0, "xMidYMid meet") },
            "<g style=\"clip-path: url(#clippath-CO1-4);\">",
        )
        return svg
    }
}

object E10Star {
    fun render(score: LazerScore): String {
        val beatmap = score.beatmap
        val ruleset = score.ruleset.toInt()
        val star = beatmap.starRating
        val name = GameModes.getDifficultyIndex(beatmap.difficultyName, star, ruleset, score.mods)
        var svg = "<g id=\"Text_OE2\"></g>"
        val rrect = PanelDraw.rect(0.0, 0.0, 420.0, 80.0, 40.0, "#382E32", 1.0)
        val rulesetPath = Fonts.extra.textPath(
            GameModes.getGameMode(ruleset, -1), 10.0, 64.0, 68.0,
            "left baseline", Colors.getStarRatingColor(star),
        )
        val starFloor = Numbers.floorParts(star, 2)
        val texts = Typography.multipleTextPath(
            listOf(
                TextPart(starFloor.integer, Fonts.poppinsBold, 66.0, "#fff"),
                TextPart(starFloor.decimal, Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(" / $name", Fonts.poppinsBold, 30.0, "#fff"),
            ),
            85.0, 62.0, "left baseline",
        )
        svg = Svg.setTexts(svg, listOf(rulesetPath, texts, rrect), "<g id=\"Text_OE2\">")
        return svg
    }
}

object E10Status {
    fun render(score: LazerScore, assets: ImageAssets): String {
        var svg = "<g id=\"Text_OE3\"></g><g id=\"Image_OE3\"></g>"
        val rrect = PanelDraw.rect(0.0, 0.0, 315.0, 80.0, 40.0, "#382E32", 1.0)
        val status = score.beatmap.status.ifBlank { "graveyard" }
        val statusText = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(status.uppercase(), 30.0, 315.0 - 15 - 85),
            85.0, 35.0, 30.0, "left baseline",
        )
        val rating = score.beatmapset.rating.toString().toDouble()
        val ratingText = Fonts.poppinsBold.textPath(Numbers.round(rating, 1), 85.0, 68.0, 30.0, "left baseline")
        val statusImage = assets.local(GameModes.mapStatusImage(status))
        svg = Svg.setText(svg, statusImage?.let { Svg.image(19.0, 15.0, 50.0, 50.0, it) }, "<g id=\"Image_OE3\">")
        svg = Svg.setTexts(svg, listOf(statusText, ratingText, rrect), "<g id=\"Text_OE3\">")
        return svg
    }
}

object E10Density {
    fun render(score: LazerScore, density: IntArray): String {
        var svg = "<g id=\"Text_OE4\"></g>"
        val rrect = PanelDraw.rect(0.0, 0.0, 315.0, 120.0, 40.0, "#382E32", 1.0)
        val star = score.beatmap.starRating
        val densityValues = density.map { it.toDouble() }
        val densityScale = when {
            star <= 1 -> 0.1
            star <= 8 -> sqrt(((star - 1) / 7 * 0.9) + 0.1)
            else -> 1.0
        }
        val densityMax = if (densityValues.isNotEmpty()) (densityValues.maxOrNull() ?: 0.0) / densityScale else 0.0
        val line = PanelDraw.lineChart(densityValues, densityMax, 0.0, 15.0, 55.0, 285.0, 45.0, Colors.getRankColor(score.rank), 1.0, 0.4, 3.0)
        val retries = score.beatmap.retries
        val fails = score.beatmap.fails
        val retryFail = fails.indices.map { index -> fails[index].toDouble() + retries.getOrElse(index) { 0 } }
        val retryFailMax = retryFail.maxOrNull() ?: 0.0
        val retry = PanelDraw.barChart(retryFail, retryFailMax, 0.0, 15.0, 110.0, 285.0, 45.0, 2.0, 0.0, "#f6d659")
        val fail = PanelDraw.barChart(fails.map { it.toDouble() }, retryFailMax, 0.0, 15.0, 110.0, 285.0, 45.0, 2.0, 0.0, "#ed6c9e")
        svg = Svg.setTexts(svg, listOf(line, fail, retry, rrect), "<g id=\"Text_OE4\">")
        return svg
    }
}

data class LabelE10DData(
    val abbr: String = "",
    val iconTitle: String = "",
    val dataMain: String = "-",
    val dataMid: String = "",
    val barColors: List<String>? = null,
    val barColor: String = "#382E32",
    val isDisplay: Boolean = true,
)

object E10Labels {
    private val BPM = LabelE10DData("BPM", "BPM", barColors = ColorArray.purple)
    private val LENGTH = LabelE10DData("LEN", "Length", barColors = ColorArray.magenta)
    private val CS = LabelE10DData("", "CS", barColors = ColorArray.blue)
    private val KEY = LabelE10DData("", "KEY", barColors = ColorArray.blue)
    private val AR = LabelE10DData("", "AR", barColors = ColorArray.green)
    private val OD = LabelE10DData("", "OD", barColors = ColorArray.yellow)
    private val HP = LabelE10DData("", "HP", barColors = ColorArray.red)
    private val EMPTY = LabelE10DData(dataMain = "-", barColor = "#382E32", isDisplay = false)

    fun render(score: LazerScore): String {
        val beatmap = score.beatmap
        val mode = GameModes.getGameMode(score.ruleset.toInt(), 1)
        val bpm = beatmap.bpm.legacyFloat()
        val bpmData = if (bpm >= 1000) Numbers.floor(bpm, 0, -1) else kotlin.math.floor(bpm).toLong().toString()
        val bpmLabel = BPM.copy(dataMain = bpmData)

        val total = beatmap.totalLength
        val lengthMin = total / 60
        val lengthSec = total % 60
        val lengthData = if (lengthMin >= 100) ">100" else lengthMin.toString()
        val lengthMid = if (lengthMin >= 10) "m" else ":" + lengthSec.toString().padStart(2, '0')
        val lengthLabel = LENGTH.copy(dataMain = lengthData, dataMid = lengthMid)

        val csValue = beatmap.cs?.legacyFloat() ?: 0.0
        val arValue = beatmap.ar?.legacyFloat() ?: 0.0
        val odValue = beatmap.od?.legacyFloat() ?: 0.0
        val hpValue = beatmap.hp?.legacyFloat() ?: 0.0
        val csParts = Numbers.floorParts(csValue, 1)
        val arParts = Numbers.floorParts(arValue, 1)
        val odParts = Numbers.floorParts(odValue, 1)
        val hpParts = Numbers.floorParts(hpValue, 1)

        val csLabel = (if (mode == "m") KEY else CS).copy(dataMain = csParts.integer, dataMid = csParts.decimal)
        val arLabel = AR.copy(dataMain = arParts.integer, dataMid = arParts.decimal)
        val odLabel = OD.copy(dataMain = odParts.integer, dataMid = odParts.decimal)
        val hpLabel = HP.copy(dataMain = hpParts.integer, dataMid = hpParts.decimal)

        val labels = when (mode) {
            "t" -> listOf(bpmLabel, lengthLabel, EMPTY, EMPTY, odLabel, hpLabel)
            "c" -> listOf(bpmLabel, lengthLabel, csLabel, arLabel, EMPTY, hpLabel)
            "m" -> listOf(bpmLabel, lengthLabel, csLabel, EMPTY, odLabel, hpLabel)
            else -> listOf(bpmLabel, lengthLabel, csLabel, arLabel, odLabel, hpLabel)
        }
        val body = labels.mapIndexed { index, label ->
            val x = 165.0 * (index % 2)
            val y = 55.0 * (index / 2)
            Svg.group(x, y, LabelE10D.render(label))
        }.joinToString("\n")
        return "<g id=\"Text_OE5\">$body</g>"
    }

    private fun Float.legacyFloat(): Double = toString().toDouble()
}

object LabelE10D {
    fun render(data: LabelE10DData): String {
        val gradient = data.barColors
        val displayOpacity = if (data.isDisplay) 0.2 else 0.0
        val foregroundOpacity = if (data.isDisplay) 1.0 else 0.2
        val textColor = if (data.isDisplay) "#fff" else "#aaa"

        val base = PanelDraw.rect(0.0, 0.0, 150.0, 40.0, 20.0, "#382E32")
        val background = if (gradient != null) {
            PanelDraw.linearGradientRect(0.0, 0.0, 150.0, 40.0, 20.0, gradient, listOf(0, 100), listOf(0, 0), displayOpacity)
        } else {
            PanelDraw.rect(0.0, 0.0, 150.0, 40.0, 20.0, data.barColor, displayOpacity)
        }
        val foreground = if (gradient != null) {
            PanelDraw.linearGradientRect(0.0, 0.0, 80.0, 40.0, 20.0, gradient, listOf(0, 100), listOf(0, 0), foregroundOpacity)
        } else {
            PanelDraw.rect(0.0, 0.0, 80.0, 40.0, 20.0, data.barColor, foregroundOpacity)
        }

        val title = data.abbr.ifEmpty { data.iconTitle }
        val text = Fonts.poppinsBold.textPath(title, 40.0, 30.0, 30.0, "center baseline", textColor) +
            Fonts.poppinsBold.get2SizeTextPath(data.dataMain, data.dataMid, 30.0, 24.0, 112.0, 30.0, "center baseline", textColor)
        return "<g id=\"Label_E10D\">$base$background$foreground$text</g>"
    }
}

data class LabelE10SData(
    val index: String,
    val stat: Int,
    val colors: List<String>,
    val maxStat: Double,
)

object E10Center {
    fun render(score: LazerScore, progress: Double, assets: ImageAssets): String {
        var svg = "<g id=\"Text_OE6\"></g>"
        val mode = GameModes.getGameMode(score.ruleset.toInt(), 1)
        val stat = score.statistics
        val maxStat = score.maximumStatistics
        val isFc = score.maxCombo.toDouble() / (score.beatmap.maxCombo ?: 0) > 0.98 && stat.miss == 0

        val top: String
        val bottom: String
        if (mode == "m") {
            if (score.lazerAccuracy > 1 - 1e-4 || stat.perfect == maxStat.perfect) {
                top = "AP+"
                bottom = "PERFECT+"
            } else {
                val rate = stat.perfect.toDouble() / stat.great
                val rateText = when {
                    rate >= 10 -> Numbers.round(rate, 0)
                    rate < 1 -> Numbers.round(rate, 2)
                    else -> Numbers.round(rate, 1)
                }
                top = rateText + "x"
                bottom = "PG/GR RATE"
            }
        } else if (score.rank == "X" || score.rank == "XH") {
            top = "AP"
            bottom = "PERFECT"
        } else if (score.fullCombo || score.perfectCombo) {
            top = "FC+"
            bottom = "FULLCOMBO+"
        } else if (isFc) {
            top = "FC"
            bottom = "FULLCOMBO"
        } else if (stat.miss <= 20 || (stat.miss.toDouble() / (score.totalHit.takeIf { it != 0 } ?: 1)) <= 0.1) {
            top = "${stat.miss}x"
            bottom = "MISS"
        } else if (score.rank != "F") {
            top = "PASS"
            bottom = ""
        } else {
            top = "${Numbers.jsRound(progress * 100).toLong()}%"
            bottom = "PROGRESS"
        }

        val texts = Fonts.poppinsBold.textPath(top, 960.0, 130.0, 128.0, "center baseline", "#fff") +
            Fonts.poppinsBold.textPath(bottom, 960.0, 205.0, 48.0, "center baseline", "#fff")
        val mods = Mods.drawLazerMods(score.mods, 960.0, 254.0, 70.0, 350.0, "center", 6.0, true, true, assets).svg
        val rank = assets.local(GameModes.rankImageFile(score.rank))
        svg = Svg.setText(svg, rank?.let { Svg.image(665.0, 250.0, 590.0, 590.0, it) }, "<g id=\"Text_OE6\">")
        svg = Svg.setTexts(svg, listOf(texts, mods), "<g id=\"Text_OE6\">")
        return svg
    }
}

object E10Performance {
    fun render(score: LazerScore, attributes: Any?, assets: ImageAssets): String {
        var svg = """
            <defs>
            <clipPath id="clippath-OE7-1">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-2">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-3">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-4">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-5">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-6">
                <rect id="SR_Base" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE7-7">
                <rect id="BG_Base" x="0" y="0" width="500" height="230" rx="40" ry="40" style="fill: none;"/>
            </clipPath>
            <linearGradient id="grad-OE7-12" x1="0%" y1="50%" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(79,172,254); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(0,242,254); stop-opacity:1" />
            </linearGradient>
            <linearGradient id="grad-OE7-13" x1="0%" y1="0" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(94,220,91); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(202,248,129); stop-opacity:1" />
            </linearGradient>
            <linearGradient id="grad-OE7-14" x1="0%" y1="0" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(252,172,70); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(254,220,69); stop-opacity:1" />
            </linearGradient>
            <linearGradient id="grad-OE7-15" x1="0%" y1="50%" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(231,72,138); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(255,120,107); stop-opacity:1" />
            </linearGradient>
            <linearGradient id="grad-OE7-16" x1="0%" y1="50%" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(79,172,254); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(0,242,254); stop-opacity:1" />
            </linearGradient>
            </defs>
            <g id="Base_OE7">
                <rect id="Base" x="0" y="0" width="500" height="230" rx="40" ry="40" style="fill: #382e32;"/>
            </g>
            <g id="Background_OE7" style="clip-path: url(#clippath-OE7-7)">
            </g>
            <g id="Rect_OE7">
                <rect id="Star" x="15" y="125" width="470" height="30" rx="15" ry="15" style="fill: url(#grad-OE7-12); fill-opacity: 0.2"/>
                <g id="Clip_OE7-6" style="clip-path: url(#clippath-OE7-6);"></g>
                <g id="Clip_OE7-5" style="clip-path: url(#clippath-OE7-5);"></g>
                <g id="Clip_OE7-4" style="clip-path: url(#clippath-OE7-4);"></g>
                <g id="Clip_OE7-3" style="clip-path: url(#clippath-OE7-3);"></g>
                <g id="Clip_OE7-2" style="clip-path: url(#clippath-OE7-2);"></g>
            </g>
            <g id="Text_OE7">
            </g>
        """.trimIndent()

        val performance = attributes as? CalculatePerformance
        val full = attributes as? FullCalculatePerformance
        val rosu = attributes as? RosuPerformance
        val pp = performance?.pp ?: 0.0
        val fullPp = full?.fullPP ?: 0.0
        val perfectPp = full?.perfectPP ?: 0.0
        val aimPp = rosu?.ppAim ?: 0.0
        val speedPp = rosu?.ppSpeed ?: 0.0
        val accuracyPp = rosu?.ppAccuracy ?: 0.0
        val flashlightPp = rosu?.ppFlashlight ?: 0.0
        val difficultyPp = rosu?.ppDifficulty ?: 0.0

        val mode = GameModes.getGameMode(score.ruleset.toInt(), 1)
        val isFc = score.maxCombo.toDouble() / (score.beatmap.maxCombo ?: 0) > 0.98 || score.fullCombo || score.perfectCombo
        val isPf = score.lazerRank == "X" || score.lazerRank == "XH"
        val indexFcText: String
        val indexPpText: String
        if (isPf) {
            indexFcText = ""
            indexPpText = "MAX"
        } else if (isFc) {
            indexFcText = "PP IF SS"
            indexPpText = Numbers.jsRound(perfectPp).toLong().toString()
        } else {
            indexFcText = "PP IF FC"
            indexPpText = Numbers.jsRound(fullPp).toLong().toString()
        }

        val ppText = Fonts.poppinsBold.get2SizeTextPath(
            Numbers.jsRound(pp).toLong().toString(), "PP", 108.0, 72.0, 235.0, 106.0, "center baseline", "#fff",
        )
        val indexText = Fonts.poppinsBold.get2SizeTextPath(indexPpText, indexFcText, 60.0, 48.0, 235.0, 214.0, "center baseline", "#fff")
        val referencePp = if (isPf || isFc) perfectPp else fullPp

        when (mode) {
            "o" -> {
                val sum = aimPp + speedPp + accuracyPp + flashlightPp
                val aimWidth = childPpWidth(aimPp, sum, pp, referencePp)
                val spdWidth = childPpWidth(speedPp, sum, pp, referencePp)
                val accWidth = childPpWidth(accuracyPp, sum, pp, referencePp)
                val flWidth = childPpWidth(flashlightPp, sum, pp, referencePp)
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, aimWidth, 30.0, 15.0, "url(#grad-OE7-12)", 1.0), "<g id=\"Clip_OE7-2\" style=\"clip-path: url(#clippath-OE7-2);\">")
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, aimWidth + spdWidth, 30.0, 15.0, "url(#grad-OE7-13)", 1.0), "<g id=\"Clip_OE7-3\" style=\"clip-path: url(#clippath-OE7-3);\">")
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, aimWidth + spdWidth + accWidth, 30.0, 15.0, "url(#grad-OE7-14)", 1.0), "<g id=\"Clip_OE7-4\" style=\"clip-path: url(#clippath-OE7-4);\">")
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, aimWidth + spdWidth + accWidth + flWidth, 30.0, 15.0, "url(#grad-OE7-15)", 1.0), "<g id=\"Clip_OE7-5\" style=\"clip-path: url(#clippath-OE7-5);\">")
                val texts = listOf(
                    childPpPath("AIM", 15.0, 148.0, 24.0, aimWidth, aimWidth, 10.0),
                    childPpPath("SPD", 15.0, 148.0, 24.0, aimWidth + spdWidth, spdWidth, 10.0),
                    childPpPath("ACC", 15.0, 148.0, 24.0, aimWidth + spdWidth + accWidth, accWidth, 10.0),
                    childPpPath("FL", 15.0, 148.0, 24.0, aimWidth + spdWidth + accWidth + flWidth, flWidth, 10.0),
                )
                svg = Svg.setTexts(svg, texts, "<g id=\"Text_OE7\">")
            }
            "t" -> {
                val sum = difficultyPp + accuracyPp
                val diffWidth = childPpWidth(difficultyPp, sum, pp, referencePp)
                val accWidth = childPpWidth(accuracyPp, sum, pp, referencePp)
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, diffWidth, 30.0, 15.0, "url(#grad-OE7-13)", 1.0), "<g id=\"Clip_OE7-3\" style=\"clip-path: url(#clippath-OE7-3);\">")
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, diffWidth + accWidth, 30.0, 15.0, "url(#grad-OE7-14)", 1.0), "<g id=\"Clip_OE7-4\" style=\"clip-path: url(#clippath-OE7-4);\">")
                val texts = listOf(
                    childPpPath("DIFF", 15.0, 148.0, 24.0, diffWidth, diffWidth, 10.0),
                    childPpPath("ACC", 15.0, 148.0, 24.0, diffWidth + accWidth, accWidth, 10.0),
                )
                svg = Svg.setTexts(svg, texts, "<g id=\"Text_OE7\">")
            }
        }

        svg = Svg.setTexts(svg, listOf(ppText, indexText), "<g id=\"Text_OE7\">")
        val ppWidth = if (referencePp > 0) (pp / referencePp) * 470 else 470.0
        svg = Svg.setText(svg, PanelDraw.rect(15.0, 125.0, ppWidth, 30.0, 15.0, "url(#grad-OE7-16)", 1.0), "<g id=\"Clip_OE7-6\" style=\"clip-path: url(#clippath-OE7-6);\">")
        val background = assets.local(GameModes.rankBackgroundFile(score.rank, score.passed))
        svg = Svg.setText(
            svg,
            background?.let { Svg.image(0.0, 0.0, 500.0, 230.0, it, 0.4) },
            "<g id=\"Background_OE7\" style=\"clip-path: url(#clippath-OE7-7)\">",
        )
        return svg
    }

    private fun childPpWidth(childPp: Double, childSum: Double, pp: Double, referencePp: Double): Double {
        if (childPp <= 0) return 0.0
        val ppWidth = if (referencePp > 0) (pp / referencePp) * 470 else 470.0
        val childWidth = if (childSum > 0) (childPp / childSum) * ppWidth else 0.0
        return max(30.0, childWidth)
    }

    private fun childPpPath(type: String, x: Double, y: Double, size: Double, offset: Double, width: Double, interval: Double): String {
        val typeWidth = Fonts.poppinsBold.width(type, size)
        val shown = width > 0 && width - 2 * interval >= typeWidth
        val slight = width > 0 && width + interval / 2 >= typeWidth
        return if (shown) {
            Fonts.poppinsBold.textPath(type, x + min(offset, 470.0) - interval, y, size, "right baseline", "#382c32")
        } else if (slight) {
            Fonts.poppinsBold.textPath(type, x + min(offset, 470.0) - width / 2, y, size, "center baseline", "#382c32")
        } else {
            ""
        }
    }
}

object E10Combo {
    fun render(score: LazerScore): String {
        var svg = "<g id=\"Text_OE8\"></g>"
        val text = Fonts.poppinsBold.get2SizeTextPath(
            "${score.maxCombo}x", " / ${score.beatmap.maxCombo ?: 0}x", 66.0, 30.0, 210.0, 62.0,
        )
        val rrect = PanelDraw.rect(0.0, 0.0, 420.0, 80.0, 40.0, "#382E32")
        svg = Svg.setTexts(svg, listOf(text, rrect), "<g id=\"Text_OE8\">")
        return svg
    }
}

object E10Accuracy {
    fun render(score: LazerScore): String {
        var svg = "<g id=\"Text_OE9\"></g>"
        val accuracy = Numbers.roundParts(score.accuracy * 100, 2)
        val text = Fonts.poppinsBold.get2SizeTextPath(
            accuracy.integer, accuracy.decimal + "%", 66.0, 30.0, 315.0 / 2, 62.0,
        )
        val rrect = PanelDraw.rect(0.0, 0.0, 315.0, 80.0, 40.0, "#382E32")
        svg = Svg.setTexts(svg, listOf(text, rrect), "<g id=\"Text_OE9\">")
        return svg
    }
}

object E10Statistics {
    fun render(score: LazerScore): String {
        val mode = GameModes.getGameMode(score.ruleset.toInt(), 1)
        val stat = score.statistics
        val maxStat = score.maximumStatistics
        val slots = mutableListOf<LabelE10SData?>()
        val maxValues = mutableListOf<Double>()

        when (mode) {
            "o" -> {
                slots += null
                slots += null
                slots += LabelE10SData("GR", stat.great, ColorArray.cyan, 0.0)
                slots += LabelE10SData("OK", stat.ok, ColorArray.green, 0.0)
                slots += LabelE10SData("MH", stat.meh, ColorArray.yellow, 0.0)
                slots += LabelE10SData("MS", stat.miss, ColorArray.red, 0.0)
                maxValues += listOf(0.0, 0.0, maxStat.great.toDouble(), maxStat.great.toDouble(), maxStat.great.toDouble(), maxStat.great.toDouble())
            }
            "t" -> {
                slots += null
                slots += null
                slots += null
                slots += LabelE10SData("GR", stat.great, ColorArray.cyan, 0.0)
                slots += LabelE10SData("OK", stat.ok, ColorArray.green, 0.0)
                slots += LabelE10SData("MS", stat.miss, ColorArray.red, 0.0)
                maxValues += listOf(0.0, 0.0, 0.0, maxStat.great.toDouble(), maxStat.great.toDouble(), maxStat.great.toDouble())
            }
            "c" -> {
                slots += null
                slots += LabelE10SData("L", stat.great, ColorArray.cyan, 0.0)
                slots += LabelE10SData("M", stat.largeTickHit, ColorArray.green, 0.0)
                slots += LabelE10SData("S", stat.smallTickHit, ColorArray.yellow, 0.0)
                slots += LabelE10SData("MS", stat.miss, ColorArray.red, 0.0)
                slots += LabelE10SData("MD", stat.smallTickMiss, ColorArray.gray, 0.0)
                val maxValue = max(maxStat.great, maxStat.smallTickHit).toDouble()
                maxValues += listOf(0.0, maxValue, maxValue, maxValue, maxValue, maxStat.smallTickHit.toDouble())
            }
            "m" -> {
                slots += LabelE10SData("PG", stat.perfect, ColorArray.cyan, 0.0)
                slots += LabelE10SData("GR", stat.great, ColorArray.yellow, 0.0)
                slots += LabelE10SData("GD", stat.good, ColorArray.green, 0.0)
                slots += LabelE10SData("OK", stat.ok, ColorArray.blue, 0.0)
                slots += LabelE10SData("MH", stat.meh, ColorArray.gray, 0.0)
                slots += LabelE10SData("MS", stat.miss, ColorArray.red, 0.0)
                val maxValue = max(
                    max(stat.great + stat.perfect, stat.good),
                    max(max(stat.ok, stat.meh), stat.miss),
                ).toDouble()
                maxValues += List(6) { maxValue }
            }
        }

        val body = slots.mapIndexedNotNull { index, slot ->
            if (slot == null) return@mapIndexedNotNull null
            val maxStatValue = maxValues.getOrElse(index) { 0.0 }
            Svg.group(0.0, 50.0 * index, LabelE10S.render(slot.copy(maxStat = maxStatValue)))
        }.joinToString("\n")
        return body
    }
}

object LabelE10S {
    fun render(data: LabelE10SData): String {
        val base = PanelDraw.rect(0.0, 0.0, 315.0, 40.0, 20.0, "#382E32")
        val percent = data.stat / (if (data.maxStat > 0) data.maxStat else 1.0)
        val colors = data.colors.asReversed()
        val progress = PanelDraw.linearGradientRect(
            (315.0 - 80.0) * (1 - percent), 0.0, (315.0 - 80.0) * percent + 80.0, 40.0, 20.0,
            colors, listOf(0, 100), listOf(0, 0), 0.4,
        )
        val top = PanelDraw.linearGradientRect(
            315.0 - 80.0, 0.0, 80.0, 40.0, 20.0,
            colors, listOf(0, 100), listOf(0, 0), 1.0,
        )
        val index = Fonts.poppinsBold.textPath(data.index, 315.0 - 40.0, 30.0, 30.0, "center baseline", "#fff")
        val stat = Fonts.poppinsBold.textPath(data.stat.toString(), 15.0, 30.0, 30.0, "left baseline", "#fff")
        return "<g id=\"Label_E10S\">$base$progress$top$stat$index</g>"
    }
}

object E10Title {
    fun render(score: LazerScore, background: String?): String {
        var svg = """
            <defs>
            <filter id="blur-OE11-BG" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
            </filter>
            <clipPath id="clippath-OE11-1">
                <rect id="BG_Base" x="0" y="0" width="1370" height="220" rx="40" ry="40" style="fill: none;"/>
            </clipPath>
            </defs>
            <g id="Base_OE11"></g>
            <g id="Background_OE11" filter="url(#blur-OE11-BG)" style="clip-path: url(#clippath-OE11-1);"></g>
            <g id="Text_OE11"></g>
        """.trimIndent()

        val beatmap = score.beatmap
        val beatmapset = score.beatmapset
        var difficulty = GameModes.getKeyDifficulty(beatmap)
        val creators = if (beatmap.owners.isNullOrEmpty()) {
            beatmapset.creator
        } else {
            beatmap.owners!!.joinToString(", ") { owner ->
                val username: String? = owner.username
                username ?: "U${owner.userID}"
            }.also { difficulty = GameModes.removeGuest(difficulty) }
        }
        val unicode = beatmapset.aliases?.firstOrNull()?.toString() ?: beatmapset.titleUnicode
        val title = Typography.beatMapTitlePath(
            Fonts.poppinsBold, Fonts.puHuiTi,
            beatmapset.title, unicode, beatmapset.artist,
            685.0, 70.0, 134.0, 60.0, 40.0, 1350.0,
        )
        val creatorText = Fonts.poppinsBold.cutStringTail(creators, 36.0, 320.0)
        val bidText = "b${beatmap.beatmapID}"
        val reserved = 2 * max(
            Fonts.poppinsBold.width(creatorText, 24.0),
            Fonts.poppinsBold.width(bidText, 36.0),
        )
        val diffText = Fonts.poppinsBold.cutStringTail(difficulty, 40.0, 1370.0 - 40 - 10 - reserved)
        val diff = Fonts.poppinsBold.textPath(diffText, 685.0, 194.0, 40.0, "center baseline", "#fff")
        val creator = Fonts.poppinsBold.textPath(creatorText, 20.0, 194.0, 36.0, "left baseline", "#fff")
        val bid = Fonts.poppinsBold.textPath(bidText, 1350.0, 194.0, 36.0, "right baseline", "#fff")
        val rect = PanelDraw.rect(0.0, 0.0, 1370.0, 220.0, 40.0, "#382E32", 1.0)

        svg = Svg.setTexts(svg, listOf(title.title, title.titleUnicode, bid, creator, diff), "<g id=\"Text_OE11\">")
        svg = Svg.setText(
            svg,
            background?.let { Svg.image(0.0, 0.0, 1370.0, 220.0, it, 0.6) },
            "<g id=\"Background_OE11\" filter=\"url(#blur-OE11-BG)\" style=\"clip-path: url(#clippath-OE11-1);\">",
        )
        svg = Svg.setText(svg, rect, "<g id=\"Base_OE11\">")
        return svg
    }
}

private fun Float.legacyFloat(): Double = toString().toDouble()
