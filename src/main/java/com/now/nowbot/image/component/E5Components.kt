package com.now.nowbot.image.component

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
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object E1Component {
    private const val MARK_TEXT = "<g id=\"Text_OE1\">"
    private const val MARK_STAR = "<clipPath id=\"clippath-OE1-2\">"

    fun render(score: LazerScore): String {
        return render(score.beatmap, score.ruleset.toInt(), score.mods, score.beatmap.originalStar)
    }

    fun render(beatmap: Beatmap, ruleset: Int, mods: List<LazerMod>, originalStar: Double): String {
        val star = beatmap.starRating.legacyFloat()
        var svg = """
            <defs>
            <clipPath id="clippath-OE1-1">
                <rect id="SR_Base" x="15" y="105" width="460" height="30" rx="15" ry="15" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-OE1-2">
            </clipPath>
            <linearGradient id="grad-OE1-3" x1="0%" y1="0" x2="100%" y2="0">
                <stop offset="0%" style="stop-color:rgb(66,144,251); stop-opacity:1" />
                <stop offset="14%" style="stop-color:rgb(79,192,255); stop-opacity:1" />
                <stop offset="22%" style="stop-color:rgb(79,255,213); stop-opacity:1" />
                <stop offset="28%" style="stop-color:rgb(124,255,79); stop-opacity:1" />
                <stop offset="37%" style="stop-color:rgb(246,240,92); stop-opacity:1" />
                <stop offset="47%" style="stop-color:rgb(255,104,104); stop-opacity:1" />
                <stop offset="55%" style="stop-color:rgb(255,78,111); stop-opacity:1" />
                <stop offset="65%" style="stop-color:rgb(198,69,184); stop-opacity:1" />
                <stop offset="75%" style="stop-color:rgb(101,99,222); stop-opacity:1" />
                <stop offset="86%" style="stop-color:rgb(24,21,142); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(0,0,0); stop-opacity:1" />
            </linearGradient>
            </defs>
            <g id="Base_OE1">
                <rect id="Base" x="0" y="0" width="490" height="150" rx="20" ry="20" style="fill: #382e32;"/>
                <rect id="Star" x="15" y="105" width="460" height="30" rx="15" ry="15" style="fill: url(#grad-OE1-3); fill-opacity: 0.2"/>
            </g>
            <g id="Star_OE1" style="clip-path: url(#clippath-OE1-2);">
                <rect id="Star" x="15" y="105" width="460" height="30" rx="15" ry="15" style="fill: url(#grad-OE1-3); fill-opacity: 0.9"/>
            </g>
            <g id="Text_OE1">
            </g>
            <g id="Overlay_OE1">
            </g>
        """.trimIndent()

        val name = GameModes.getDifficultyIndex(beatmap.difficultyName, star, ruleset, mods)
        val starRect = PanelDraw.rect(15.0, 105.0, Numbers.clampToInteger((star / 9) * 460, 460.0, 20.0), 30.0, 15.0, "none")
        val rulesetPath = Fonts.extra.textPath(
            GameModes.getGameMode(ruleset, -1), 18.0, 78.0, 72.0, "left baseline",
            Colors.getStarRatingColor(star),
        )
        val starFloor = Numbers.floorParts(star, 2)
        val texts = Typography.multipleTextPath(
            listOf(
                TextPart(starFloor.integer, Fonts.poppinsBold, 84.0, "#fff"),
                TextPart(starFloor.decimal, Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(" / $name", Fonts.poppinsBold, 24.0, "#fff"),
            ),
            105.0, 88.0, "left baseline",
        )
        val original = originalStar.legacyFloat()
        val showOriginal = original > 0 && abs(original - star) > 1e-4
        val titleText = (if (showOriginal) "(${Numbers.floor(original, 2)}) " else "") + "Star Rating"
        val title = Fonts.poppinsBold.textPath(titleText, 475.0, 28.0, 18.0, "right baseline", "#fff")

        svg = Svg.setText(svg, starRect, MARK_STAR)
        svg = Svg.setTexts(svg, listOf(rulesetPath, texts, title), MARK_TEXT)
        return svg
    }
}

object E2Component {
    fun render(score: LazerScore, density: IntArray, progress: Double): String {
        val beatmap = score.beatmap
        val beatmapset = score.beatmapset
        val densityValues = density.map { it.toDouble() }
        val retries = beatmap.retries
        val fails = beatmap.fails
        val pass = beatmap.passCount ?: 0
        val play = beatmap.playCount
        val baseStar = if (beatmap.originalStar != 0.0) beatmap.originalStar.legacyFloat() else beatmap.starRating
        val rating = beatmapset.rating.legacyFloat()
        val hasLeaderboard = GameModes.hasLeaderBoard(beatmap.ranked.toInt())

        val passPercent = if (play > 0) Numbers.jsRound(pass.toDouble() / play * 100).toLong() else 0
        val densityScale = when {
            baseStar <= 1 -> 0.1
            baseStar <= 8 -> sqrt(((baseStar - 1) / 7 * 0.9) + 0.1)
            else -> 1.0
        }
        val densityMax = if (densityValues.isNotEmpty()) (densityValues.maxOrNull() ?: 0.0) / densityScale else 0.0
        val retryFail = fails.indices.map { index -> fails[index].toDouble() + retries.getOrElse(index) { 0 } }
        val retryFailMax = retryFail.maxOrNull() ?: 0.0
        val publicRating = if (hasLeaderboard) Numbers.floor(rating, 1) else "-"
        val progressLine = if (progress < 1) {
            PanelDraw.rect(20 + 457 * progress + 1.5, 35.0, 3.0, 80.0, 1.5, "#ed6c9e")
        } else {
            ""
        }

        return PanelDraw.rect(0.0, 0.0, 490.0, 250.0, 20.0, "#382e32") +
            Fonts.poppinsBold.textPath("Density", 15.0, 28.0, 18.0, "left baseline", "#fff") +
            Fonts.poppinsBold.textPath("Retry & Fail", 15.0, 138.0, 18.0, "left baseline", "#fff") +
            Fonts.poppinsBold.textPath("P. Rating: $publicRating", 475.0, 28.0, 18.0, "right baseline", "#fff") +
            Fonts.poppinsBold.textPath("Pass: $pass / $play [$passPercent%]", 475.0, 138.0, 18.0, "right baseline", "#fff") +
            PanelDraw.lineChart(densityValues, densityMax, 0.0, 15.0, 115.0, 460.0, 80.0, Colors.getRankColor(score.rank), 1.0, 0.4, 3.0) +
            progressLine +
            PanelDraw.barChart(retryFail, retryFailMax, 0.0, 15.0, 235.0, 460.0, 80.0, 2.0, 0.0, "#f6d659") +
            PanelDraw.barChart(fails.map { it.toDouble() }, retryFailMax, 0.0, 15.0, 235.0, 460.0, 80.0, 2.0, 0.0, "#ed6c9e")
    }
}

object E3Component {
    private const val MARK_LABELS = "<g id=\"Labels_OE3\">"

    fun render(score: LazerScore, original: Map<String, Any>, assets: ImageAssets): String {
        val beatmap = score.beatmap
        val ruleset = score.ruleset.toInt()
        val bpm = beatmap.bpm.legacyFloat()
        val bpmValue = Numbers.roundParts(bpm, 2)
        val bpmRemark = if (bpm > 0) "${Numbers.jsRound(60000 / bpm).toLong()}ms" else "-"
        val bpmProgress = Numbers.normalize(bpm, 270.0, 90.0, 1.0, 1e-4)

        val totalLength = beatmap.totalLength
        val hitLength = beatmap.hitLength ?: 0
        val lengthRemark = "${totalLength / 60}:${Numbers.jsRound((totalLength % 60).toDouble()).toLong().toString().padStart(2, '0')}"
        val lengthMain = "${hitLength / 60}:"
        val lengthMid = (hitLength % 60).toString().padStart(2, '0')
        val lengthProgress = Numbers.normalize(hitLength.toDouble(), 270.0, 30.0, 1.0, 1e-4)

        var csMin = 2.0
        var csMid = 4.0
        var csMax = 6.0
        var arMin = 7.5
        var arMid = 9.0
        var arMax = 10.5
        var odMin = 5.5
        var odMid = 8.0
        var odMax = 10.5
        var hpMin = 4.0
        var hpMid = 6.0
        var hpMax = 8.0
        var displayCs = true
        var displayAr = true
        var displayOd = true
        var index = ""
        val circles = beatmap.circles ?: 0
        val sliders = beatmap.sliders ?: 0
        val ratio = if (sliders > 0) circles.toDouble() / sliders else if (circles > 0) Double.POSITIVE_INFINITY else 0.0

        when (ruleset) {
            0 -> index = "CIR & SLD: $circles / $sliders [${Numbers.ratioString(ratio)}]"
            1 -> {
                csMin = 0.0; csMid = 0.0; csMax = 0.0
                arMin = 0.0; arMid = 0.0; arMax = 0.0
                odMin = 4.0; odMid = 6.0; odMax = 8.0
                displayAr = false
                displayCs = false
            }
            2 -> {
                odMin = 0.0; odMid = 0.0; odMax = 0.0
                displayOd = false
            }
            3 -> {
                csMin = 4.0; csMid = 6.0; csMax = 8.0
                arMin = 0.0; arMid = 0.0; arMax = 0.0
                hpMin = 7.0; hpMid = 8.0; hpMax = 9.0
                displayAr = false
                index = "RC & LN: $circles / $sliders [${Numbers.ratioString(ratio)}]"
            }
        }

        val cs = beatmap.cs?.legacyFloat() ?: 0.0
        val ar = beatmap.ar?.legacyFloat() ?: 0.0
        val od = beatmap.od?.legacyFloat() ?: 0.0
        val hp = beatmap.hp?.legacyFloat() ?: 0.0
        val originalCs = number(original["cs"])
        val originalAr = number(original["ar"])
        val originalOd = number(original["od"])
        val originalHp = number(original["hp"])

        val csSpec = (if (ruleset == 3) LabelCatalog.key else LabelCatalog.cs).copy(
            barMin = csMin, barMid = csMid, barMax = csMax,
        )
        val arSpec = LabelCatalog.ar.copy(barMin = arMin, barMid = arMid, barMax = arMax)
        val odSpec = LabelCatalog.od.copy(barMin = odMin, barMid = odMid, barMax = odMax)
        val hpSpec = LabelCatalog.hp.copy(barMin = hpMin, barMid = hpMid, barMax = hpMax)

        val labels = listOf(
            LabelCatalog.build(
                LabelCatalog.bpm, bpmRemark, bpmValue.integer, bpmValue.decimal, "", bpmProgress,
                LabelE5.icon(assets, LabelCatalog.bpm),
            ),
            LabelCatalog.build(
                LabelCatalog.length, lengthRemark, lengthMain, lengthMid, "", lengthProgress,
                LabelE5.icon(assets, LabelCatalog.length),
            ),
            LabelCatalog.stat(
                csSpec, cs, GameModes.cs2px(cs, ruleset),
                Numbers.normalize(cs, csMax, csMin, 1.0, 1e-4), originalCs, displayCs, LabelE5.icon(assets, csSpec),
            ),
            LabelCatalog.stat(
                arSpec, ar, GameModes.ar2ms(ar, ruleset),
                Numbers.normalize(ar, arMax, arMin, 1.0, 1e-4), originalAr, displayAr, LabelE5.icon(assets, arSpec),
            ),
            LabelCatalog.stat(
                odSpec, od, GameModes.od2ms(od, ruleset),
                Numbers.normalize(od, odMax, odMin, 1.0, 1e-4), originalOd, displayOd, LabelE5.icon(assets, odSpec),
            ),
            LabelCatalog.stat(
                hpSpec, hp, "-",
                Numbers.normalize(hp, hpMax, hpMin, 1.0, 1e-4), originalHp, true, LabelE5.icon(assets, hpSpec),
            ),
        )

        var svg = "<g id=\"Labels_OE3\"></g>"
        val title = Fonts.poppinsBold.textPath("Statistics", 15.0, 28.0, 18.0, "left baseline", "#fff")
        val indexPath = Fonts.poppinsBold.textPath(index, 475.0, 28.0, 18.0, "right baseline", "#fff")
        val labelSvg = labels.take(6).mapIndexed { position, label ->
            val x = 15.0 + 235.0 * (position % 2)
            val y = 38.0 + 76.0 * (position / 2)
            Svg.group(x, y, LabelE5.render(label))
        }.joinToString("")
        val rect = PanelDraw.rect(0.0, 0.0, 490.0, 270.0, 20.0, "#382e32", 1.0)
        svg = Svg.setTexts(svg, listOf(labelSvg, title, indexPath, rect), MARK_LABELS)
        return svg
    }

    private fun number(value: Any?): Double {
        return when (value) {
            is Float -> value.legacyFloat()
            is Number -> value.toDouble()
            else -> 0.0
        }
    }
}

object E4Component {
    private const val MARK_BASE = "<g id=\"Base_OE4\">"
    private const val MARK_STATUS = "<g id=\"Status_OE4\">"

    fun render(score: LazerScore, assets: ImageAssets): String = render(score.beatmap, assets)

    fun render(beatmap: Beatmap, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE4\"></g><g id=\"Status_OE4\"></g>"
        val rect = PanelDraw.rect(0.0, 0.0, 90.0, 60.0, 20.0, "#382e32", 1.0)
        val image = assets.local(GameModes.mapStatusFile(beatmap.ranked.toInt()))
        svg = Svg.setText(svg, image?.let { Svg.image(20.0, 5.0, 50.0, 50.0, it) }, MARK_STATUS)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }
}

object E5StatsComponent {
    private const val MARK_BASE = "<g id=\"Base_OE5\">"
    private const val MARK_TEXT = "<g id=\"Text_OE5\">"

    fun render(score: LazerScore, assets: ImageAssets): String {
        return render(score.beatmapset.favouriteCount, score.beatmapset.playCount, assets)
    }

    fun render(favoriteCount: Long, playCount: Long, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE5\"></g><g id=\"Text_OE5\"></g>"
        val favorite = Fonts.poppinsBold.textPath(Numbers.floor(favoriteCount, 1, -1), 78.0, 25.0, 16.0, "right baseline", "#fff")
        val playcount = Fonts.poppinsBold.textPath(Numbers.floor(playCount, 1, -1), 78.0, 47.0, 16.0, "right baseline", "#fff")
        val rect = PanelDraw.rect(0.0, 0.0, 90.0, 60.0, 20.0, "#382e32", 1.0)
        val favoriteIcon = assets.local("object-beatmap-favorite.png")?.let { Svg.image(12.0, 9.0, 18.0, 18.0, it) }
        val playcountIcon = assets.local("object-beatmap-playcount.png")?.let { Svg.image(12.0, 32.0, 18.0, 18.0, it) }
        svg = Svg.setTexts(svg, listOf(favorite, playcount, favoriteIcon, playcountIcon), MARK_TEXT)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }
}

object E6Component {
    private const val MARK_BASE = "<g id=\"Base_OE6\">"
    private const val MARK_BACKGROUND = "<g id=\"Background_OE6\" style=\"clip-path: url(#clippath-OE6-1);\">"
    private const val MARK_LABELS = "<g id=\"Labels_OE6\">"

    fun render(score: LazerScore, background: String?): String {
        return render(score.beatmap, score.beatmapset, background)
    }

    fun render(beatmap: Beatmap, beatmapset: Beatmapset, background: String?): String {
        var svg = """
            <defs>
            <clipPath id="clippath-OE6-1">
                <rect id="BG_Base" x="0" y="0" width="820" height="160" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            </defs>
            <g id="Base_OE6"></g>
            <g id="Background_OE6" style="clip-path: url(#clippath-OE6-1);"></g>
            <g id="Labels_OE6"></g>
        """.trimIndent()

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
            410.0, 55.0, 98.0, 48.0, 24.0, 800.0,
        )
        val creatorText = Fonts.poppinsBold.cutStringTail(creators, 24.0, 240.0)
        val bidText = "b${beatmap.beatmapID}"
        val reserved = 2 * max(Fonts.poppinsBold.width(creatorText, 24.0), Fonts.poppinsBold.width(bidText, 24.0))
        val diffText = Fonts.poppinsBold.cutStringTail(difficulty, 30.0, 770.0 - reserved)
        val difficultyPath = Fonts.poppinsBold.textPath(diffText, 410.0, 142.0, 30.0, "center baseline", "#fff")
        val creatorPath = Fonts.poppinsBold.textPath(creatorText, 20.0, 142.0, 24.0, "left baseline", "#fff")
        val bidPath = Fonts.poppinsBold.textPath(bidText, 800.0, 142.0, 24.0, "right baseline", "#fff")
        val backgroundPath = background?.let { Svg.image(0.0, 0.0, 820.0, 160.0, it, 0.4) }.orEmpty()
        val rect = PanelDraw.rect(0.0, 0.0, 820.0, 160.0, 20.0, "#382e32", 1.0)

        svg = Svg.setTexts(svg, listOf(title.title, title.titleUnicode, bidPath, creatorPath, difficultyPath), MARK_LABELS)
        svg = Svg.setText(svg, backgroundPath, MARK_BACKGROUND)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }
}

object E7Component {
    private const val MARK_TEXT = "<g id=\"Text_OE7\">"
    private const val MARK_CLIP2 = "<g id=\"Clip_OE7-2\" style=\"clip-path: url(#clippath-OE7-1);\">"
    private const val MARK_CLIP3 = "<g id=\"Clip_OE7-3\" style=\"clip-path: url(#clippath-OE7-1);\">"
    private const val MARK_CLIP4 = "<g id=\"Clip_OE7-4\" style=\"clip-path: url(#clippath-OE7-1);\">"
    private const val MARK_CLIP5 = "<g id=\"Clip_OE7-5\" style=\"clip-path: url(#clippath-OE7-1);\">"
    private const val MARK_CLIP6 = "<g id=\"Clip_OE7-6\" style=\"clip-path: url(#clippath-OE7-1);\">"

    fun render(score: LazerScore, performance: PerformanceValues): String {
        var svg = """
            <defs>
            <clipPath id="clippath-OE7-1">
                <rect id="SR_Base" x="15" y="105" width="460" height="30" rx="15" ry="15" style="fill: none;"/>
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
            <linearGradient id="grad-OE7-20" x1="0%" y1="50%" x2="100%" y2="50%">
                <stop offset="0%" style="stop-color:rgb(99,99,99); stop-opacity:1" />
                <stop offset="100%" style="stop-color:rgb(204,204,204); stop-opacity:1" />
            </linearGradient>
            </defs>
            <g id="Base_OE7">
                <rect id="Base" x="0" y="0" width="490" height="150" rx="20" ry="20" style="fill: #382e32;"/>
                <rect id="Star" x="15" y="105" width="460" height="30" rx="15" ry="15" style="fill: url(#grad-OE7-12); fill-opacity: 0.2"/>
            </g>
            <g id="Rect_OE7">
                <g id="Clip_OE7-6" style="clip-path: url(#clippath-OE7-1);"></g>
                <g id="Clip_OE7-5" style="clip-path: url(#clippath-OE7-1);"></g>
                <g id="Clip_OE7-4" style="clip-path: url(#clippath-OE7-1);"></g>
                <g id="Clip_OE7-3" style="clip-path: url(#clippath-OE7-1);"></g>
                <g id="Clip_OE7-2" style="clip-path: url(#clippath-OE7-1);"></g>
            </g>
            <g id="Text_OE7"></g>
        """.trimIndent()

        val pp = score.pp
        val fullPp = performance.fullPp
        val perfectPp = performance.perfectPp
        val pfPercent = if (perfectPp > 0) pp / perfectPp else 1.0
        val fcPercent = if (fullPp > 0) pp / fullPp else 1.0
        val ruleset = score.ruleset.toInt()
        var fc = performance.isFc(score)
        val over = 460 - Fonts.poppinsBold.width(Numbers.jsRound(perfectPp).toLong().toString(), 24.0) - 30 < 460 * fcPercent
        if (over) fc = true

        val referencePp: Double
        val percent: Double
        val percentType: String
        val fcPpText: String
        if (ruleset == 3 && fcPercent < 0.95 && pfPercent < 0.95) {
            referencePp = fullPp
            percent = fcPercent
            percentType = "FC"
            fcPpText = Numbers.jsRound(perfectPp).toLong().toString()
        } else if (fc) {
            referencePp = perfectPp
            percent = pfPercent
            percentType = if (ruleset == 3) "PF" else "SS"
            fcPpText = ""
        } else {
            referencePp = fullPp
            percent = fcPercent
            percentType = "FC"
            fcPpText = Numbers.jsRound(perfectPp).toLong().toString()
        }

        val isPerfect = (percentType == "SS" && Numbers.jsRound(percent * 100) > 100 - 1e-7) ||
            (Numbers.jsRound(perfectPp) - Numbers.jsRound(pp)) < 1e-4
        val referenceText = if (isPerfect) {
            " / PERFECT"
        } else {
            " / ${Numbers.jsRound(referencePp).toLong()} $percentType [${Numbers.jsRound(percent * 100).toLong()}%]"
        }
        val fcPp = Fonts.poppinsBold.textPath(fcPpText, 460.0, 128.0, 24.0, "right baseline", "#fff")
        val ppText = Numbers.jsRound(pp).toLong().toString()
        val maxWidth = Fonts.poppinsBold.width(ppText, 84.0) +
            Fonts.poppinsBold.width(" PP", 48.0) +
            Fonts.poppinsBold.width(referenceText, 24.0)
        val textParts: List<TextPart>
        if (maxWidth <= 465) {
            textParts = listOf(
                TextPart(ppText, Fonts.poppinsBold, 84.0, "#fff"),
                TextPart(" PP", Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(referenceText, Fonts.poppinsBold, 24.0, "#fff"),
            )
        } else {
            val ppRound = Numbers.roundParts(pp, -4)
            val midWidth = Fonts.poppinsBold.width(ppRound.integer, 84.0) +
                Fonts.poppinsBold.width(ppRound.decimal + " PP", 48.0) +
                Fonts.poppinsBold.width(referenceText, 24.0)
            textParts = if (midWidth <= 465) {
                listOf(
                    TextPart(ppRound.integer, Fonts.poppinsBold, 84.0, "#fff"),
                    TextPart(ppRound.decimal + " PP", Fonts.poppinsBold, 48.0, "#fff"),
                    TextPart(referenceText, Fonts.poppinsBold, 24.0, "#fff"),
                )
            } else {
                listOf(
                    TextPart(ppRound.integer, Fonts.poppinsBold, 84.0, "#fff"),
                    TextPart(ppRound.decimal + " PP", Fonts.poppinsBold, 48.0, "#fff"),
                    TextPart(" / ${Numbers.round(Numbers.jsRound(referencePp).toLong(), 2, -1)} $percentType", Fonts.poppinsBold, 24.0, "#fff"),
                )
            }
        }
        val texts = Typography.multipleTextPath(textParts, 20.0, 88.0, "left baseline")
        val title = Fonts.poppinsBold.textPath("Performance Points", 475.0, 28.0, 18.0, "right baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(texts, title, fcPp), MARK_TEXT)

        val skills: List<Triple<String, String, String>>
        val values: Map<String, Double>
        if (ruleset == 0) {
            skills = listOf(
                Triple("aim", "url(#grad-OE7-12)", MARK_CLIP2),
                Triple("spd", "url(#grad-OE7-13)", MARK_CLIP3),
                Triple("reading", "url(#grad-OE7-14)", MARK_CLIP4),
                Triple("fl", "url(#grad-OE7-20)", MARK_CLIP5),
                Triple("acc", "url(#grad-OE7-15)", MARK_CLIP6),
            )
            values = mapOf(
                "aim" to performance.aimPp, "spd" to performance.speedPp, "reading" to performance.readingPp,
                "fl" to performance.flashlightPp, "acc" to performance.accuracyPp,
            )
        } else if (ruleset == 1) {
            skills = listOf(
                Triple("diff", "url(#grad-OE7-13)", MARK_CLIP3),
                Triple("acc", "url(#grad-OE7-15)", MARK_CLIP4),
            )
            values = mapOf("diff" to performance.difficultyPp, "acc" to performance.accuracyPp)
        } else {
            skills = emptyList()
            values = emptyMap()
        }

        if (skills.isNotEmpty()) {
            val sum = values.values.sum()
            var accumulated = 0.0
            val skillTexts = mutableListOf<String>()
            for ((key, gradient, clip) in skills) {
                val value = values[key] ?: 0.0
                if (value.isNaN() || value < 1e-4) continue
                val width = childPpWidth(value, sum, pp, referencePp)
                accumulated += width
                svg = Svg.setText(svg, PanelDraw.rect(15.0, 105.0, accumulated, 30.0, 15.0, gradient, 1.0), clip)
                skillTexts += childPpPath(Numbers.jsRound(value).toLong().toString(), 15.0, 128.0, 24.0, accumulated, width, 10.0)
            }
            svg = Svg.setTexts(svg, skillTexts, MARK_TEXT)
        }

        val ppWidth = if (referencePp > 0) (pp / referencePp) * 460 else 460.0
        svg = Svg.setText(svg, PanelDraw.rect(15.0, 105.0, ppWidth, 30.0, 15.0, "url(#grad-OE7-16)", 1.0), MARK_CLIP6)
        return svg
    }

    private fun childPpWidth(childPp: Double, childSum: Double, pp: Double, referencePp: Double): Double {
        if (childPp <= 0) return 0.0
        val totalWidth = if (referencePp > 0) (pp / referencePp) * 460 else 460.0
        val childWidth = if (childSum > 0) (childPp / childSum) * totalWidth else 0.0
        return max(30.0, childWidth)
    }

    private fun childPpPath(text: String, x: Double, y: Double, size: Double, offset: Double, width: Double, interval: Double): String {
        val textWidth = Fonts.poppinsBold.width(text, size)
        val shown = Texts.isNotBlank(text) && width > 0 && width - 2 * interval >= textWidth
        val slight = Texts.isNotBlank(text) && width > 0 && width + interval / 2 >= textWidth
        return if (shown) {
            Fonts.poppinsBold.textPath(text, x + min(offset, 460.0) - interval, y, size, "right baseline", "#382c32")
        } else if (slight) {
            Fonts.poppinsBold.textPath(text, x + min(offset, 460.0) - width / 2, y, size, "center baseline", "#382c32")
        } else {
            ""
        }
    }
}

object E8Component {
    private const val MARK_BASE = "<g id=\"Base_OE8\">"
    private const val MARK_MODS = "<g id=\"Mods_OE8\">"
    private const val MARK_TEXT = "<g id=\"Text_OE8\">"

    fun render(score: LazerScore, isLazer: Boolean, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE8\"></g><g id=\"Mods_OE8\"></g><g id=\"Text_OE8\"></g>"
        val total = if (isLazer) score.score else score.legacyScore ?: 0
        val scoreData = Numbers.floorParts(total, -4, if (score.mods.size <= 4) 1 else 0)
        val scoreWidth = Fonts.poppinsBold.width(scoreData.integer, 56.0) + Fonts.poppinsBold.width(scoreData.decimal, 36.0)
        val mods = Mods.drawLazerMods(
            score.mods, 480.0, 4.0, 70.0, 450.0 - scoreWidth, "right", 6.0, true, true, assets,
        ).svg
        val scorePath = Typography.multipleTextPath(
            listOf(
                TextPart(scoreData.integer, Fonts.poppinsBold, 56.0, "#fff"),
                TextPart(scoreData.decimal, Fonts.poppinsBold, 36.0, "#fff"),
            ),
            20.0, 62.0, "left baseline",
        )
        val title = if (score.mods.isEmpty()) Fonts.poppinsBold.textPath("Score", 475.0, 28.0, 18.0, "right baseline", "#fff") else ""
        val rect = PanelDraw.rect(0.0, 0.0, 490.0, 80.0, 20.0, "#382e32", 1.0)
        svg = Svg.setText(svg, mods, MARK_MODS)
        svg = Svg.setTexts(svg, listOf(title, scorePath), MARK_TEXT)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }
}

object E9Component {
    private const val MARK_BASE = "<g id=\"Base_OE9\">"
    private const val MARK_TEXT = "<g id=\"Text_OE9\">"

    fun render(score: LazerScore): String {
        var svg = "<g id=\"Base_OE9\"></g><g id=\"Text_OE9\"></g>"
        val title = Fonts.poppinsBold.textPath("Accuracy", 15.0, 28.0, 18.0, "left baseline", "#fff")
        val title2 = Fonts.poppinsBold.textPath("Combo", 15.0, 98.0, 18.0, "left baseline", "#fff")
        val accuracy = Numbers.floorParts(score.accuracy * 100, 2)
        val accuracyPath = Typography.multipleTextPath(
            listOf(
                TextPart(accuracy.integer, Fonts.poppinsBold, 60.0, "#fff"),
                TextPart(accuracy.decimal + " %", Fonts.poppinsBold, 36.0, "#fff"),
            ),
            470.0, 62.0, "right baseline",
        )
        val maxCombo = when {
            score.ruleset.toInt() == 3 -> score.maximumStatistics.perfect + score.maximumStatistics.legacyComboIncrease
            score.beatmap.convert == true && score.ruleset.toInt() == 2 -> (score.beatmap.maxCombo ?: 0) - (score.beatmap.spinners ?: 0)
            else -> score.beatmap.maxCombo ?: 0
        }
        val comboPath = Typography.multipleTextPath(
            listOf(
                TextPart(score.maxCombo.toString(), Fonts.poppinsBold, 60.0, "#fff"),
                TextPart(" / $maxCombo", Fonts.poppinsBold, 36.0, "#fff"),
            ),
            470.0, 132.0, "right baseline",
        )
        val rect = PanelDraw.rect(0.0, 0.0, 490.0, 150.0, 20.0, "#382e32", 1.0)
        svg = Svg.setTexts(svg, listOf(title, title2, accuracyPath, comboPath), MARK_TEXT)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }
}

data class JudgementSlot(
    val index: String,
    val stat: Int,
    val colors: List<String>,
)

object E10Component {
    private const val MARK_BASE = "<g id=\"Base_OE10\">"
    private const val MARK_TEXT = "<g id=\"Text_OE10\">"

    fun render(score: LazerScore, performance: PerformanceValues): String {
        var svg = "<g id=\"Base_OE10\"></g><g id=\"Text_OE10\"></g>"
        val ruleset = score.ruleset.toInt()
        val isLazer = score.isLazer
        val slots = statisticsSlots(score, isLazer)
        val maxStatistics = statisticsMax(score, isLazer)
        val fullStatistics = if (ruleset == 2) {
            listOf(
                score.maximumStatistics.great.toDouble(),
                score.maximumStatistics.largeTickHit.toDouble(),
                score.maximumStatistics.smallTickHit.toDouble(),
            )
        } else {
            emptyList<Double>()
        }

        val title = Fonts.poppinsBold.textPath("Judgment", 15.0, 28.0, 18.0, "left baseline", "#fff")
        val statisticsPath = statisticsSvg(slots, maxStatistics, fullStatistics)
        val ratioText = if (ruleset == 3) {
            val statistics = score.statistics
            val ratio = when {
                statistics.great == 0 && statistics.perfect == 0 -> 0.0
                statistics.great == 0 -> Double.POSITIVE_INFINITY
                statistics.perfect == 0 -> 0.0
                else -> statistics.perfect.toDouble() / statistics.great
            }
            Fonts.poppinsBold.textPath("MAX : 300 = ${Numbers.ratioString(ratio)}", 475.0, 28.0, 18.0, "right baseline", "#fff")
        } else {
            ""
        }
        val effectiveMiss = performance.effectiveMissCount
        val showMiss = (ruleset == 0 || ruleset == 1) && effectiveMiss != null && effectiveMiss.isFinite() &&
            abs(effectiveMiss - Numbers.jsRound(effectiveMiss)) > 1e-3
        val missText = if (showMiss) {
            Fonts.poppinsBold.textPath("equivalent miss: ${Numbers.floor(effectiveMiss, 2)}", 475.0, 28.0, 18.0, "right baseline", "#fff")
        } else {
            ""
        }
        val rect = PanelDraw.rect(0.0, 0.0, 490.0, 270.0, 20.0, "#382e32", 1.0)
        svg = Svg.setTexts(svg, listOf(title, statisticsPath, ratioText, missText), MARK_TEXT)
        svg = Svg.setText(svg, rect, MARK_BASE)
        return svg
    }

    private fun statisticsSvg(slots: List<JudgementSlot?>, maximum: List<Double?>, full: List<Double>): String {
        val builder = StringBuilder()
        val standard = min(slots.size, 6)
        for (index in 0 until standard) {
            val slot = slots[index] ?: continue
            val maxValue = maximum.getOrNull(index) ?: 0.0
            val y = 45.0 + index * (20.0 + 16.0)
            val textY = y + 16.0
            builder.append(Fonts.poppinsBold.textPath(slot.index, 54.0, textY, 18.0, "right baseline", "#fff"))
            builder.append(Fonts.poppinsBold.textPath(slot.stat.toString(), 414.0 + (if (slot.stat < 10000) 10 else 5), textY, 18.0, "left baseline", "#fff"))
            if (slot.stat > 0) builder.append(bar(64.0, y, 360.0, 20.0, slot, maxValue, 1.0))
            val fullValue = full.getOrNull(index)
            val fullWidth = if (fullValue != null && maxValue > 0 && fullValue.isFinite()) min(360.0 * fullValue / maxValue, 360.0) else 360.0
            builder.append(background(64.0, y, fullWidth, 20.0, slot, 0.1))
        }

        if (slots.size > 6) {
            val end = min(slots.size, 12)
            for (index in 6 until end) {
                val slot = slots[index] ?: continue
                val maxValue = maximum.getOrNull(index) ?: 0.0
                val deluxeWidth = 360.0 / 2 - 50
                val deluxeX = if (index < 9) 64.0 else 64.0 + 180.0 + 50.0
                val y = if (index < 9) 45.0 + (index - 6 + 3) * 36.0 else 45.0 + (index - 9 + 3) * 36.0
                val textY = y + 16.0
                builder.append(Fonts.poppinsBold.textPath(slot.index, deluxeX - 10, textY, 18.0, "right baseline", "#fff"))
                builder.append(Fonts.poppinsBold.textPath(slot.stat.toString(), deluxeX + deluxeWidth + (if (slot.stat < 10000) 10 else 5), textY, 18.0, "left baseline", "#fff"))
                if (slot.stat > 0) builder.append(bar(deluxeX, y, deluxeWidth, 20.0, slot, maxValue, 0.4))
                builder.append(background(deluxeX, y, deluxeWidth, 20.0, slot, 0.1))
            }
        }
        return builder.toString()
    }

    private fun bar(x: Double, y: Double, width: Double, height: Double, slot: JudgementSlot, maxValue: Double, opacity: Double): String {
        val ratio = if (maxValue > 0) slot.stat / maxValue else 0.0
        val barWidth = max(width * ratio, height)
        return if (slot.colors.size > 1) {
            PanelDraw.linearGradientRect(x, y, barWidth, height, height / 2, slot.colors, listOf(0, 100), listOf(0, 0), opacity)
        } else {
            PanelDraw.rect(x, y, barWidth, height, height / 2, slot.colors.firstOrNull() ?: "#fff", opacity)
        }
    }

    private fun background(x: Double, y: Double, width: Double, height: Double, slot: JudgementSlot, opacity: Double): String {
        return if (slot.colors.size > 1) {
            PanelDraw.linearGradientRect(x, y, width, height, height / 2, slot.colors, listOf(0, 100), listOf(0, 0), opacity)
        } else {
            PanelDraw.rect(x, y, width, height, height / 2, slot.colors.firstOrNull() ?: "#fff", opacity)
        }
    }

    private fun slot(index: String, stat: Int, colors: List<String>): JudgementSlot = JudgementSlot(index, stat, colors)

    private fun statisticsSlots(score: LazerScore, lazer: Boolean): List<JudgementSlot?> {
        val s = score.statistics
        val slots = mutableListOf<JudgementSlot?>()
        when (score.ruleset.toInt()) {
            0 -> if (lazer) {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("100", s.ok, ColorArray.lightGreen)
                slots += slot("50", s.meh, ColorArray.yellow)
                slots += slot("0", s.miss, ColorArray.pink)
                slots += null; slots += null; slots += null
                slots += slot("TICK", s.largeTickHit, ColorArray.cyan)
                slots += slot("END", s.sliderTailHit, ColorArray.blue)
                slots += null
                slots += slot("O+", s.largeBonus, ColorArray.orange)
                slots += slot("O?", s.smallBonus, ColorArray.yellow)
            } else {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("100", s.ok, ColorArray.lightGreen)
                slots += slot("50", s.meh, ColorArray.yellow)
                slots += null
                slots += slot("0", s.miss, ColorArray.pink)
            }
            1 -> if (lazer) {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("150", s.ok, ColorArray.lightGreen)
                slots += slot("0", s.miss, ColorArray.pink)
                slots += null; slots += null; slots += null; slots += null; slots += null; slots += null
                slots += slot("O+", s.largeBonus, ColorArray.cyan)
                slots += slot("==", s.smallBonus, ColorArray.yellow)
                slots += slot("()", s.ignoreHit, ColorArray.gray)
            } else {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("150", s.ok, ColorArray.lightGreen)
                slots += slot("0", s.miss, ColorArray.pink)
            }
            2 -> if (lazer) {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("100", s.largeTickHit, ColorArray.lightGreen)
                slots += slot("50", s.smallTickHit, ColorArray.yellow)
                slots += null; slots += null; slots += null; slots += null
                slots += slot("MF", s.miss, ColorArray.pink)
                slots += slot("ML", s.smallTickMiss, ColorArray.gray)
                slots += null
                slots += slot("MD", s.largeTickMiss, ColorArray.deepRed)
                slots += slot("BNN", s.largeBonus, ColorArray.yellow)
            } else {
                slots += slot("300", s.great, ColorArray.cyan)
                slots += slot("100", s.largeTickHit, ColorArray.lightGreen)
                slots += slot("50", s.smallTickHit, ColorArray.yellow)
                slots += null
                slots += slot("0", s.miss, ColorArray.pink)
                slots += slot("ML", s.smallTickMiss, ColorArray.gray)
            }
            3 -> {
                slots += slot("MAX", s.perfect, ColorArray.rainbow)
                slots += slot("300", s.great, ColorArray.yellow)
                slots += slot("200", s.good, ColorArray.lightGreen)
                slots += slot("100", s.ok, ColorArray.blue)
                slots += slot("50", s.meh, ColorArray.gray)
                slots += slot("0", s.miss, ColorArray.pink)
            }
        }
        return slots
    }

    private fun statisticsMax(score: LazerScore, lazer: Boolean): List<Double?> {
        val m = score.maximumStatistics
        val s = score.statistics
        return when (score.ruleset.toInt()) {
            0 -> if (lazer) {
                val max = m.great.toDouble()
                listOf(
                    max, max, max, max, 0.0, 0.0, 0.0,
                    m.largeTickHit.toDouble(), m.sliderTailHit.toDouble(), 0.0,
                    m.largeBonus.toDouble(), m.smallBonus.toDouble(),
                )
            } else {
                val max = m.great.toDouble()
                listOf(max, max, max, 0.0, max)
            }
            1 -> if (lazer) {
                val max = m.great.toDouble()
                listOf(
                    max, max, max, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    m.largeBonus.toDouble(), m.smallBonus.toDouble(), m.ignoreHit.toDouble(),
                )
            } else {
                val max = m.great.toDouble()
                listOf(max, max, max)
            }
            2 -> if (lazer) {
                val max = maxOf(m.great, m.largeTickHit, m.smallTickHit).toDouble()
                val fruit = max(m.great - m.largeTickHit, 0).toDouble()
                listOf(
                    max, max, max, 0.0, 0.0, 0.0, 0.0,
                    fruit, m.smallTickHit.toDouble(), 0.0,
                    m.largeTickHit.toDouble(), m.largeBonus.toDouble(),
                )
            } else {
                val max = max(m.great, m.smallTickHit).toDouble()
                listOf(max, max, max, 0.0, max, m.smallTickHit.toDouble())
            }
            3 -> {
                val max = max(
                    max(s.great + s.perfect, s.good),
                    max(max(s.ok, s.meh), s.miss),
                ).toDouble()
                List(6) { max }
            }
            else -> emptyList()
        }
    }
}

object E10PComponent {
    private const val MARK_RANK = "<g id=\"Rank_OE11\">"
    private const val MARK_CROWN = "<g id=\"Crown_OE11\">"

    fun render(score: LazerScore, progress: Double, assets: ImageAssets): String {
        if (score.ruleset.toInt() != 1) return ""
        val rankKey = rankKey(score, progress)
        val crownKey = crownKey(score)
        val rank = assets.local(rankKey)?.let { Svg.image(20.0, 160.0, 100.0, 100.0, it) }
        val crownX = if (rankKey.isNotBlank()) 140.0 else 20.0
        val crown = assets.local(crownKey)?.let { Svg.image(crownX, 160.0, 100.0, 100.0, it) }
        var svg = "<g id=\"Crown_OE11\"></g><g id=\"Rank_OE11\"></g>"
        svg = Svg.setText(svg, rank, MARK_RANK)
        svg = Svg.setText(svg, crown, MARK_CROWN)
        return svg
    }

    private fun rankKey(score: LazerScore, progress: Double): String {
        val statistics = score.statistics
        val similarCombo = max(score.beatmap.maxCombo ?: 1, 1)
        val comboRate = Numbers.clamp(score.maxCombo.toDouble() / similarCombo, 1.0, 0.0)
        val rainbowRating = if (score.type == "sb_score" || !score.isLazer) {
            (250000 * comboRate + 750000 * score.lazerAccuracy.pow(3.6) + statistics.largeBonus * 300) / 1000000
        } else if (score.rawScore > 0) {
            score.rawScore / 1000000.0
        } else {
            val total = if (score.score > 0) score.score else score.legacyScore ?: 0
            total / (1000000.0 * Mods.multiplier(score.mods, score.ruleset.toInt()))
        }

        val rank = score.rank
        return when {
            rank == "X" || rank == "XH" -> "object-score-kiwami-rainbow.png"
            rainbowRating < 0.5 - 1e-4 && progress >= 0.98 -> "object-score-don-disqualified.png"
            rainbowRating < 0.5 - 1e-4 -> "object-score-jimaodan.png"
            rank != "F" && (statistics.miss == 1 || (statistics.miss == 0 && statistics.ok == 1) ||
                (statistics.miss == 0 && statistics.meh == 1)) -> "object-score-sei-rainbow.png"
            rainbowRating < 0.6 - 1e-4 -> "object-score-iki-iron.png"
            rainbowRating < 0.7 - 1e-4 -> "object-score-iki-bronze.png"
            rainbowRating < 0.8 - 1e-4 -> "object-score-iki-silver.png"
            rainbowRating < 0.9 - 1e-4 -> "object-score-miyabi-gold.png"
            rainbowRating < 0.95 - 1e-4 -> "object-score-miyabi-pink.png"
            rainbowRating < 1.0 - 1e-4 -> "object-score-miyabi-purple.png"
            else -> "object-score-kiwami-rainbow.png"
        }
    }

    private fun crownKey(score: LazerScore): String {
        val statistics = score.statistics
        return when {
            !score.passed || score.rank == "F" -> "object-score-don-failed.png"
            statistics.miss > 0 -> "object-score-crown-silver.png"
            statistics.ok > 0 -> "object-score-crown-gold.png"
            else -> "object-score-crown-rainbow.png"
        }
    }
}

private fun Float.legacyFloat(): Double = toString().toDouble()

private fun Double.legacyFloat(): Double = this

private fun PerformanceValues.isFc(score: LazerScore): Boolean {
    val beatmapCombo = score.beatmap.maxCombo ?: 0
    return (score.maxCombo > 0 && beatmapCombo > 0 && score.maxCombo.toDouble() / beatmapCombo > 0.98) ||
        score.ruleset.toInt() == 1 || score.ruleset.toInt() == 3
}
