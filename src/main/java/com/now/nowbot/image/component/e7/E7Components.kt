package com.now.nowbot.image.component.e7

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.MicroUser
import kotlin.math.sqrt

data class E2Data(val density: IntArray, val beatmap: Beatmap, val rank: String)
data class E5Data(val favorite: Long, val playCount: Long)
data class E8Data(val mods: List<LazerMod>)
data class E11Data(val background: String?)
data class E12Data(val scoringType: String, val teamType: String, val vsType: String, val playerCount: Int)
data class E13Data(val players: List<MicroUser>)

object E7Components {
    fun e2(data: E2Data): String {
        val beatmap = data.beatmap
        val density = data.density.map { it.toDouble() }
        val fails = beatmap.fails
        val retries = beatmap.retries
        val pass = beatmap.passCount ?: 0
        val play = beatmap.playCount
        val passPercent = if (play > 0) Numbers.jsRound(pass.toDouble() / play * 100).toLong() else 0
        val scale = when {
            beatmap.starRating <= 1 -> 0.1
            beatmap.starRating <= 8 -> sqrt(((beatmap.starRating - 1) / 7 * 0.9) + 0.1)
            else -> 1.0
        }
        val densityMax = if (density.isNotEmpty()) density.max() / scale else 0.0
        val rf = fails.indices.map { it -> fails[it].toDouble() + retries.getOrElse(it) { 0 } }
        val rfMax = rf.maxOrNull() ?: 0.0
        val color = Colors.getRankColor(data.rank)
        return PanelDraw.rect(0.0, 0.0, 490.0, 250.0, 20.0, "#382e32") +
            Fonts.poppinsBold.textPath("Density", 15.0, 28.0, 18.0, "left baseline", "#fff") +
            Fonts.poppinsBold.textPath("Retry & Fail", 15.0, 138.0, 18.0, "left baseline", "#fff") +
            Fonts.poppinsBold.textPath(Numbers.floor(beatmap.beatmapset?.rating ?: 0.0, 1).toString() + " / 10", 475.0, 28.0, 18.0, "right baseline", "#fff") +
            Fonts.poppinsBold.textPath("$pass / $play [$passPercent%]", 475.0, 138.0, 18.0, "right baseline", "#fff") +
            PanelDraw.lineChart(density, densityMax, 0.0, 15.0, 115.0, 460.0, 80.0, color, 1.0, 0.4, 3.0) +
            PanelDraw.barChart(rf, rfMax, 0.0, 15.0, 235.0, 460.0, 80.0, 2.0, 0.0, "#f6d659") +
            PanelDraw.barChart(fails.map { it.toDouble() }, rfMax, 0.0, 15.0, 235.0, 460.0, 80.0, 2.0, 0.0, "#ed6c9e")
    }

    fun e5(data: E5Data, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE5\"></g><g id=\"Text_OE5\"></g>"
        val fav = Fonts.poppinsBold.textPath(Numbers.floor(data.favorite, 1, -1).toString(), 78.0, 25.0, 16.0, "right baseline", "#fff")
        val pc = Fonts.poppinsBold.textPath(Numbers.floor(data.playCount, 1, -1).toString(), 78.0, 47.0, 16.0, "right baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(fav, pc), "<g id=\"Text_OE5\">")
        svg = Svg.setText(svg, assets.local("object-beatmap-favorite.png")?.let { Svg.image(12.0, 9.0, 18.0, 18.0, it) }, "<g id=\"Text_OE5\">")
        svg = Svg.setText(svg, assets.local("object-beatmap-playcount.png")?.let { Svg.image(12.0, 32.0, 18.0, 18.0, it) }, "<g id=\"Text_OE5\">")
        return Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 90.0, 60.0, 20.0, "#382e32", 1.0), "<g id=\"Base_OE5\">")
    }

    fun e8(data: E8Data, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE8\"></g><g id=\"Mods_OE8\"></g><g id=\"Text_OE8\"></g>"
        val mods = Mods.drawLazerMods(data.mods, 480.0, 4.0, 70.0, 450.0, "right", 8.0, true, true, assets).svg
        svg = Svg.setText(svg, mods, "<g id=\"Mods_OE8\">")
        svg = Svg.setText(svg, Fonts.poppinsBold.textPath("Mods", 15.0, 28.0, 18.0, "left baseline", "#fff"), "<g id=\"Text_OE8\">")
        return Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 490.0, 80.0, 20.0, "#382e32", 1.0), "<g id=\"Base_OE8\">")
    }

    fun e11(data: E11Data, assets: ImageAssets): String {
        var svg = "<defs><clipPath id=\"clippath-OE11-1\"><polygon points=\"410 80 220 190 220 410 410 520 600 410 600 190 410 80\" style=\"fill: none;\"/></clipPath></defs>" +
            "<g id=\"Base_OE11\"></g><g id=\"Background_OE11\" style=\"clip-path: url(#clippath-OE11-1);\"></g><g id=\"Overlay_OE11\"></g>"
        svg = Svg.setText(svg, assets.local("object-beatmap-hexagon.png")?.let { Svg.image(200.0, 75.0, 420.0, 450.0, it) }, "<g id=\"Overlay_OE11\">")
        svg = Svg.setText(svg, data.background?.let { Svg.image(220.0, 95.0, 380.0, 410.0, it) }, "<g style=\"clip-path: url(#clippath-OE11-1);\">")
        return Svg.setText(svg, assets.local("object-beatmap-mask.png")?.let { Svg.image(200.0, 75.0, 420.0, 450.0, it) }, "<g id=\"Base_OE11\">")
    }

    fun e12(data: E12Data): String {
        val textArray = listOf(
            TextPart(data.teamType, Fonts.poppinsBold, 84.0, "#fff"),
            TextPart(" ${data.vsType}", Fonts.poppinsBold, 48.0, "#fff"),
            TextPart(" / ${data.scoringType}", Fonts.poppinsBold, 24.0, "#fff"),
        )
        val texts = Typography.multipleTextPath(textArray, 20.0, 88.0, "left baseline")
        val title = Fonts.poppinsBold.textPath("Scoring Type", 475.0, 28.0, 18.0, "right baseline", "#fff")
        val width = maxOf((data.playerCount / 16.0) * 460.0, 30.0)
        val playerRect = PanelDraw.linearGradientRect(15.0, 105.0, width, 30.0, 15.0, listOf("#4FACFE", "#00F2FE"), listOf(0, 100), listOf(0, 0), 1.0)
        val playerText = Fonts.poppinsBold.textPath(data.playerCount.toString(), 15.0 + width - 10.0, 128.0, 24.0, "right baseline", "#fff")
        return PanelDraw.rect(0.0, 0.0, 490.0, 150.0, 20.0, "#382e32", 1.0) + texts + title + playerRect + playerText
    }

    fun cardE7(player: MicroUser, showName: Boolean, assets: ImageAssets): String {
        val avatar = assets.remote(player.avatarUrl) ?: assets.remote("https://a.ppy.sh/${player.userID}") ?: return ""
        var svg = "<defs><clipPath id=\"clippath-CE7-1\"><circle cx=\"50\" cy=\"40\" r=\"40\" style=\"fill: none;\"/></clipPath></defs>" +
            "<g id=\"Base_CE7\"></g><g id=\"Background_CE7\" style=\"clip-path: url(#clippath-CE7-1);\"></g><g id=\"Text_CE7\"></g>"
        svg = Svg.setText(svg, Svg.image(10.0, 0.0, 80.0, 80.0, avatar), "<g id=\"Background_CE7\" style=\"clip-path: url(#clippath-CE7-1);\">")
        val name = if (showName) Fonts.torus.textPath(Fonts.torus.cutStringTail(player.username, 18.0, 104.0), 50.0, 100.0, 18.0, "center baseline", "#fff") else ""
        return Svg.setText(svg, name, "<g id=\"Text_CE7\">")
    }

    fun e13(data: E13Data, assets: ImageAssets): String {
        var svg = "<g id=\"Base_OE13\"></g><g id=\"Avatar_OE13\"></g><g id=\"Text_OE13\"></g>"
        val title = Fonts.poppinsBold.textPath("Lobby", 15.0, 28.0, 18.0, "left baseline", "#fff")
        val length = data.players.size
        if (length <= 12) {
            for (i in 0..2) for (j in 0..3) {
                val k = i * 4 + j
                if (k >= length) return@e13 finish(svg, title)
                svg = Svg.setText(svg, Svg.group(j * 118.0 + 18.0, i * 135.0 + 40.0, cardE7(data.players[k], true, assets)), "<g id=\"Avatar_OE13\">")
            }
        } else {
            for (i in 0..3) for (j in 0..3) {
                val k = i * 4 + j
                if (k >= length) return@e13 finish(svg, title)
                svg = Svg.setText(svg, Svg.group(j * 118.0 + 18.0, i * 100.0 + 40.0, cardE7(data.players[k], false, assets)), "<g id=\"Avatar_OE13\">")
            }
        }
        return finish(svg, title)
    }

    private fun finish(svg: String, title: String): String {
        val withTitle = Svg.setText(svg, title, "<g id=\"Text_OE13\">")
        return Svg.setText(withTitle, PanelDraw.rect(0.0, 0.0, 490.0, 440.0, 20.0, "#382e32", 1.0), "<g id=\"Base_OE13\">")
    }

}
