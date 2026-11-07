package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.image.util.duration
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class GammaViewData(
    val background: String? = null,
    val avatar: String? = null,
    val mode: String = "",
    val left1: String = "",
    val left2: String = "",
    val left3: String = "",
    val down1: String = "",
    val down2: String = "",
    val downLeft1: String = "",
    val downLeft2: String = "",
    val downLeft3: String = "",
    val downRight1: String = "",
    val downRight2: String = "",
    val downRight3: String = "",
    val downLeftColor: String = "none",
    val backgroundColor: String = "",
    val center0b: String = "",
    val center0m: String = "",
    val center1b: String = "",
    val center1m: String = "",
    val center2: String = "",
    val panel: String = "",
)

object PanelGamma {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_BACKGROUND = "<g id=\"Background\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PGamma-BG);\" filter=\"url(#blur-PGamma-BG)\">"
    private const val MARK_AVATAR = "<g style=\"clip-path: url(#clippath-PGamma-MC);\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"
    private const val MARK_INDEX = "<g id=\"Index\">"

    fun toSvg(user: OsuUser, assets: ImageAssets, profileCard: String? = null, bannerIndex: Int? = null): String {
        return render(infoData(user, assets, profileCard, bannerIndex), assets)
    }

    fun toSvg(score: LazerScore, assets: ImageAssets): String {
        return render(scoreData(score, assets), assets)
    }

    fun toSvgSanity(data: Map<String, Any?>, assets: ImageAssets): String {
        return render(sanityData(data, assets), assets)
    }

    fun render(data: GammaViewData, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_Gamma.svg"))
        val isSanity = data.panel == "sanity"
        val isScore = data.panel == "score"

        val left1 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.left1, 16.0, 190.0, true),
            40.0, 40.0, 16.0, "left baseline", "#fff",
        )
        val left2 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.left2, 16.0, 190.0, true),
            40.0, 60.0, 16.0, "left baseline", "#fff",
        )
        val left3 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.left3, 16.0, 190.0, true),
            40.0, 150.0, 16.0, "left baseline", "#fff",
        )
        val down1 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.down1, 16.0, 190.0, true),
            200.0, 310.0, 16.0, "right baseline", "#fff",
        )
        val down2 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.down2, 16.0, 190.0, true),
            200.0, 330.0, 16.0, "right baseline", "#fff",
        )
        val downLeft1 = Fonts.torusRegular.textPath(data.downLeft1, 40.0, 340.0, 16.0, "left baseline", "#fff")
        val downLeft23 = Fonts.torus.get2SizeTextPath(
            data.downLeft2, " ${data.downLeft3}", 36.0, 24.0, 100.0, 340.0, "left baseline", data.downLeftColor,
        )
        val downRight1 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.downRight1, 16.0, 190.0, true),
            200.0, 255.0, 16.0, "right baseline", "#fff",
        )
        val downRight2 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.downRight2, 16.0, 190.0, true),
            200.0, 275.0, 16.0, "right baseline", "#fff",
        )
        val downRight3 = Fonts.torusRegular.textPath(
            Fonts.torusRegular.cutStringTail(data.downRight3, 16.0, 190.0, true),
            200.0, 295.0, 16.0, "right baseline", "#fff",
        )
        val center0 = Fonts.torus.get2SizeTextPath(data.center0b, data.center0m, 24.0, 18.0, 440.0, 65.0, "center baseline", "#fff")
        val center1 = Fonts.torus.get2SizeTextPath(data.center1b, data.center1m, 36.0, 24.0, 440.0, 280.0, "center baseline", "#fff")
        val center2 = Fonts.torus.textPath(data.center2, 440.0, 310.0, 18.0, "center baseline", "#fff")
        val mode = Fonts.extra.textPath(
            GameModes.getGameMode(data.mode, -1), 29.0, 220.0, 324.0, "center baseline", "#fff", 0.1,
        )

        svg = Svg.setTexts(
            svg,
            listOf(left1, left2, left3, down1, down2, downLeft1, downLeft23, downRight1, downRight2, downRight3, center0, center1, center2, mode),
            MARK_TEXT,
        )

        val bar1 = PanelDraw.rect(28.0, 30.0, 2.0, 120.0, 1.0, "#fff", 1.0)
        val bar2 = if (isSanity) {
            PanelDraw.rect(210.0, 243.0, 2.0, 52.0, 1.0, "#fff", 1.0)
        } else {
            PanelDraw.rect(210.0, 298.0, 2.0, 32.0, 1.0, "#fff", 1.0)
        }
        val bar3 = if (isSanity) PanelDraw.rect(28.0, 310.0, 2.0, 32.0, 1.0, "#fff", 1.0) else ""
        svg = Svg.setTexts(svg, listOf(bar1, bar2, bar3), MARK_INDEX)

        val base = PanelDraw.rect(0.0, 0.0, 240.0, 360.0, 0.0, "#2A2226", 1.0)
        val backgroundColor = PanelDraw.rect(0.0, 0.0, 240.0, 360.0, 0.0, data.backgroundColor.ifBlank { "none" }, 0.1)
        svg = Svg.setText(svg, base + backgroundColor, MARK_BACKGROUND)
        svg = Svg.setText(svg, backgroundColor, MARK_BANNER)
        svg = Svg.setText(svg, assets.resolveRef(data.avatar)?.let { Svg.image(366.0, 80.0, 148.0, 160.0, it) }, MARK_AVATAR)
        svg = Svg.setText(
            svg,
            assets.resolveRef(data.background)?.let { Svg.image(240.0, 0.0, 400.0, 360.0, it, if (isScore) 1.0 else 0.6) },
            MARK_BANNER,
        )
        svg = Svg.setText(svg, assets.local("object-beatmap-hexagon2.png")?.let { Svg.image(366.0, 80.0, 148.0, 160.0, it) }, MARK_HEXAGON)
        return svg
    }

    private fun infoData(user: OsuUser, assets: ImageAssets, profileCard: String?, bannerIndex: Int?): GammaViewData {
        val background = assets.resolveRef(profileCard)
            ?: assets.remote(user.coverUrl.takeIf { it.isNotBlank() })
            ?: assets.remote(user.cover?.url)
            ?: assets.localBanner(bannerIndex)
        val avatar = assets.remote(user.avatarUrl) ?: assets.local("avatar-guest.png")
        val join = user.joinDate
        val date = join?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd").format(it.atZoneSameInstant(ZoneOffset.ofHours(8))) } ?: "Unknown"
        val difference = if (join != null) " (${TimeFormat.difference(join)})" else ""
        val country = user.country?.code?.ifBlank { user.countryCode } ?: user.countryCode.ifBlank { "UN" }
        return GammaViewData(
            background = background,
            avatar = avatar,
            mode = user.rankHistory?.mode ?: "osu",
            left1 = "RANK / #${user.globalRank}",
            left2 = "$country / #${user.countryRank}",
            left3 = "JOIN / $date$difference",
            down1 = "FAN / ${user.followerCount}",
            down2 = "UID / ${user.userID}",
            center0b = Numbers.jsRound(user.pp).toLong().toString(),
            center0m = "PP",
            center1b = user.username,
            center1m = "",
            center2 = "${Numbers.floor(user.accuracy, 2)}% // Lv.${user.levelCurrent}",
            backgroundColor = "#2a2226",
            panel = "info",
        )
    }

    private fun scoreData(score: LazerScore, assets: ImageAssets): GammaViewData {
        val mode = GameModes.getGameMode(score.mode.shortName, 1)
        val mods = score.mods
        val modStr = if (mods.isEmpty()) "" else " +" + mods.joinToString(", ") { it.acronym }
        val star = Numbers.floorParts(score.beatmap.starRating, 2)
        return GammaViewData(
            background = assets.local("object-score-backimage-${score.rank}.webp"),
            avatar = assets.mapBackground(score, "list@2x"),
            mode = score.mode.shortName,
            left1 = score.beatmapset.title,
            left2 = score.beatmapset.artist,
            left3 = score.beatmap.difficultyName,
            down1 = stats(score, mode),
            down2 = "b${score.beatmap.beatmapID}",
            center0b = star.integer,
            center0m = star.decimal + "*",
            center1b = Numbers.jsRound(score.pp).toLong().toString(),
            center1m = "PP",
            center2 = "${Numbers.floor(score.accuracy * 100, 2)}% // ${score.maxCombo}x // ${score.rank}$modStr",
            backgroundColor = "#2a2226",
            panel = "score",
        )
    }

    private fun sanityData(data: Map<String, Any?>, assets: ImageAssets): GammaViewData {
        val users = data["users"]
        val user = when (users) {
            is OsuUser -> users
            is List<*> -> users.firstOrNull() as? OsuUser
            else -> null
        }
        val my = data["my"] as? Map<*, *>
        val sanity = ((my?.get("SAN") as? Number)?.toDouble() ?: 1.2) * 100
        val sanityRank = Rankings.getRankFromValue(
            sanity,
            listOf(120.0, 100.0, 95.0, 90.0, 80.0, 70.0, 60.0, 0.0),
            listOf("?", "++", "+", "-", "--", "!?", "!", "!!", "X"),
        )
        val sanityColorRank = Rankings.getRankFromValue(sanity)
        val sanityColor = Colors.getRankColor(sanityColorRank)
        val join = user?.joinDate
        val date = join?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd").format(it.atZoneSameInstant(ZoneOffset.ofHours(8))) } ?: "Unknown"
        val difference = if (join != null) " (${TimeFormat.difference(join)})" else ""
        val country = user?.country?.code?.ifBlank { user.countryCode } ?: user?.countryCode?.ifBlank { "UN" } ?: "UN"

        return GammaViewData(
            background = user?.coverUrl?.takeIf { it.isNotBlank() }?.let { assets.remote(it) } ?: assets.localBanner(null),
            avatar = user?.avatarUrl?.let { assets.remote(it) } ?: assets.local("avatar-guest.png"),
            mode = user?.rankHistory?.mode ?: "osu",
            left1 = "RANK / #${user?.globalRank ?: 0}",
            left2 = "$country / #${user?.countryRank ?: 0}",
            left3 = "JOIN / $date$difference",
            downLeft1 = "SANITY",
            downLeft2 = Numbers.jsRound(sanity).toLong().toString(),
            downLeft3 = sanityRank,
            downLeftColor = sanityColor,
            downRight1 = "PC / ${user?.playCount ?: 0}",
            downRight2 = "TTH / ${user?.totalHits ?: 0}",
            downRight3 = "PT / ${TimeFormat.duration((user?.playTime ?: 0).toDouble(), true)}",
            center0b = Numbers.jsRound(user?.pp ?: 0.0).toLong().toString(),
            center0m = "PP",
            center1b = user?.username ?: "Unknown",
            center1m = "",
            center2 = "${Numbers.floor(user?.accuracy ?: 0.0, 2)}% // Lv.${user?.levelCurrent ?: 0}",
            backgroundColor = sanityColor,
            panel = "sanity",
        )
    }

    private fun stats(score: LazerScore, mode: String): String {
        val s = score.statistics
        return when (mode) {
            "o" -> "${s.great} / ${s.ok} / ${s.meh} / ${s.miss}"
            "t" -> "${s.great} / ${s.ok} / ${s.miss}"
            "c" -> "${s.great} / ${s.largeTickHit} / ${s.smallTickHit} / ${s.miss} (-${s.smallTickMiss})"
            "m" -> "${s.great + s.perfect} (+${s.perfect}) / ${s.good} / ${s.ok} / ${s.meh} / ${s.miss}"
            else -> ""
        }
    }
}
