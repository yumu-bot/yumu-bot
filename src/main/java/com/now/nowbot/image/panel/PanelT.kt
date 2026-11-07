package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA2Data
import com.now.nowbot.image.card.CardC
import com.now.nowbot.image.card.CardCData
import com.now.nowbot.image.card.CardCSideColor
import com.now.nowbot.image.component.t.PanelTComponents
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.service.messageServiceImpl.PopularService.Attr
import com.now.nowbot.service.messageServiceImpl.PopularService.MaxRetry
import com.now.nowbot.service.messageServiceImpl.PopularService.PanelTData
import com.now.nowbot.service.messageServiceImpl.PopularService.PopularBeatmap
import com.now.nowbot.model.osu.Beatmap
import kotlin.math.max

object PanelT {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BODY = "<g id=\"Body_Card\">"
    private const val MARK_MAIN = "<g id=\"Main_Card\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PT-1);\">"

    fun render(data: PanelTData, assets: ImageAssets): String {
        var svg = TEMPLATE
        val start = TimeFormat.formatted(data.info.startTime)
        val end = TimeFormat.formatted(data.info.endTime)
        val request = "duration: $start - $end // request time: ${TimeFormat.nowTimestamp()}"
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Popular Beatmap (!ympu)", "PU", request), MARK_INDEX)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)
        val infoCard = CardA2Data(
            background = data.bestPerformance.beatmap?.let { assets.mapBackground(it, "list@2x") },
            mapStatus = "",
            title1 = "Popular Beatmap",
            title2 = "Group: ${data.info.groupID}",
            title3 = "",
            left1 = "member count: ${data.info.memberCount}",
            left2 = "beatmap count: ${data.info.beatmapCount}",
            left3 = "score count: ${data.info.scoreCount}",
            right1 = "mode: ${GameModes.getGameMode(data.info.mode, 2)}",
            right2 = "player count:",
            right3b = data.info.playerCount.toString(),
            right3m = "",
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA2.render(infoCard, assets)), MARK_MAIN)
        val popular = data.popular.take(5).mapIndexed { index, item -> popularCard(item, index + 1, assets) }
        val t1 = componentT1(popular)
        val t2 = componentT2(data.bestPerformance, assets)
        val t3 = componentT3(data.modAttr, data.modMaxPercent)
        val t4 = componentT4(data.ppAttr, data.ppMaxPercent)
        svg = Svg.setTexts(svg, listOf(t1, t2, t3, t4), MARK_BODY)
        return svg
    }

    private fun popularCard(item: PopularBeatmap, identifier: Int, assets: ImageAssets): String {
        val beatmap = item.beatmap ?: throw IllegalArgumentException("Popular beatmap 缺少 beatmap")
        val set = beatmap.beatmapset ?: throw IllegalArgumentException("Popular beatmap 缺少 beatmapset")
        val acc = Numbers.floor(item.accuracy * 100.0, 2) + "%"
        val combo = "${item.combo}x"
        val difficulty = Fonts.torus.cutStringTail(
            GameModes.getKeyDifficulty(beatmap), 24.0,
            500.0 - 10.0 - Fonts.torus.width("[] -   $acc$combo", 24.0), true,
        )
        val artist = Fonts.torus.cutStringTail(
            set.artist, 24.0, 500.0 - 10.0 - Fonts.torus.width(" // ${set.creator}", 24.0), true,
        )
        val title2 = if (set.title == set.titleUnicode) "" else set.titleUnicode
        val star = beatmap.starRating
        val starColor = Colors.getStarRatingColor(star)
        val color12 = if (star < 4) "#1c1719" else "#fff"
        return CardC.render(
            CardCData(
                background = assets.mapBackground(beatmap, "list@2x"),
                cover = assets.mapBackground(beatmap, "list@2x"),
                title = set.title,
                title2 = title2,
                left1 = "$artist // ${set.creator}",
                left2 = "[$difficulty] - $acc $combo",
                indexB = item.count.toString(),
                indexM = "PC",
                indexL = "${item.playerCount}x",
                indexBSize = 48.0,
                indexMSize = 36.0,
                indexLSize = 24.0,
                label1 = Numbers.floor(star, 1),
                label2 = beatmap.beatmapID.toString(),
                label5 = "#$identifier",
                colorTitle2 = "#bbb",
                colorRight = CardCSideColor.Solid(listOf("#B7AA00", "#A0A0A0", "#AC6A00", "#46393f", "#46393f").getOrElse(identifier - 1) { "#46393f" }),
                colorLeft = CardCSideColor.Solid(starColor),
                colorLabel1 = starColor,
                colorLabel2 = starColor,
                colorLabel5 = starColor,
                colorLabel12 = color12,
                colorLeft12 = "#bbb",
                fontTitle2 = "PuHuiTi",
            ),
            assets,
        )
    }

    private fun componentT1(popular: List<String>): String {
        val body = popular.mapIndexed { index, card -> Svg.group(20.0, 50.0 + index * 135.0, card) }.joinToString("\n")
        val title = Fonts.poppinsBold.textPath("Most Popular Beatmaps", 20.0, 32.0, 18.0, "left baseline", "#fff")
        val base = PanelDraw.rect(0.0, 0.0, 940.0, 710.0, 20.0, "#382E32")
        return "<g id=\"Component_T1\">$body$title$base</g>"
    }

    private fun componentT2(best: MaxRetry, assets: ImageAssets): String {
        val beatmap = best.beatmap
        val card = beatmap?.let {
            CardA2.render(CardA2Data(
                background = assets.mapBackground(it, "list@2x"),
                mapStatus = it.status,
                title1 = it.beatmapset?.title ?: "",
                title2 = it.beatmapset?.artist ?: "",
                title3 = it.beatmapset?.creator ?: "",
                left1 = "",
                left2 = it.difficultyName,
                left3 = "b${it.beatmapID}",
                right1 = "",
                right2 = "",
                right3b = Numbers.floor(it.starRating, 2).toString(),
                right3m = "*",
            ), assets)
        }.orEmpty()
        var svg = "<defs><clipPath id=\"clippath-OT2-1\"><circle cx=\"665\" cy=\"120\" r=\"70\" style=\"fill: none;\"/></clipPath></defs>" +
            "<g id=\"Component_T2\"></g><g id=\"Avatar_OT2\"><g clip-path=\"url(#clippath-OT2-1)\"></g></g>"
        svg = Svg.setText(svg, Svg.group(20.0, 50.0, card), "<g id=\"Component_T2\">")
        val title = Fonts.poppinsBold.textPath("Best Performance", 20.0, 32.0, 18.0, "left baseline", "#fff")
        val name = Fonts.poppinsBold.textPath(best.user?.username ?: "", 665.0, 222.0, 30.0, "center baseline", "#fff")
        val pp = Fonts.poppinsBold.get2SizeTextPath(best.count.toString(), " PP", 30.0, 24.0, 665.0, 258.0, "center baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 880.0, 280.0, 20.0, "#382E32")
        svg = Svg.setTexts(svg, listOf(title, name, pp, rrect), "<g id=\"Component_T2\">")
        val avatar = best.user?.avatarUrl?.let { assets.remote(it) } ?: best.user?.userID?.let { assets.remote("https://a.ppy.sh/$it") }
        return Svg.setText(svg, avatar?.let { Svg.image(595.0, 50.0, 140.0, 140.0, it) }, "<g clip-path=\"url(#clippath-OT2-1)\">")
    }

    private fun componentT3(attr: List<Attr>, maxPercent: Double): String {
        val maxWidth = when {
            attr.size <= 4 -> 400.0
            attr.size <= 8 -> 195.0
            else -> 126.66
        }
        val cols = when {
            attr.size <= 4 -> 1
            attr.size <= 8 -> 2
            else -> 3
        }
        var pies = ""
        var prev = 0.0
        attr.forEach { pies += PanelTComponents.pie(215.0, 126.0, 100.0, prev, prev + it.percent, Colors.getModColor(it.index)); prev += it.percent }
        val labels = attr.take(12).mapIndexed { index, item ->
            val x = index % cols
            val y = index / cols
            Svg.group(15.0 + x * (maxWidth + 10.0), 245.0 + y * 40.0,
                PanelTComponents.labelT(item.index, item.index, item.count.toString().padStart(3, '0'),
                    item.percent / maxPercent.coerceAtLeast(1e-9), Colors.getModColor(item.index), maxWidth))
        }.joinToString("")
        val title = Fonts.poppinsBold.textPath("Mods", 20.0, 32.0, 18.0, "left baseline", "#fff")
        val base = PanelDraw.rect(0.0, 0.0, 430.0, 410.0, 20.0, PanelColor.middle())
        val circle = PanelDraw.circle(215.0, 126.0, 100.0, PanelColor.top())
        return "<g id=\"Component_T3\">$title$pies$labels$circle$base</g>"
    }

    private fun componentT4(attr: List<Attr>, maxPercent: Double): String {
        val maxWidth = when {
            attr.size <= 4 -> 400.0
            attr.size <= 8 -> 195.0
            else -> 126.66
        }
        val cols = when {
            attr.size <= 4 -> 1
            attr.size <= 8 -> 2
            else -> 3
        }
        var pies = ""
        var prev = 0.0
        attr.forEach { pies += PanelTComponents.pie(215.0, 126.0, 100.0, prev, prev + it.percent, ppColor(it.index)); prev += it.percent }
        val labels = attr.take(12).mapIndexed { index, item ->
            val x = index % cols
            val y = index / cols
            val title = ppIndex(item.index)
            Svg.group(15.0 + x * (maxWidth + 10.0), 245.0 + y * 40.0,
                PanelTComponents.labelT(title, title, item.count.toString().padStart(3, '0'),
                    item.percent / maxPercent.coerceAtLeast(1e-9), ppColor(item.index), maxWidth))
        }.joinToString("")
        val title = Fonts.poppinsBold.textPath("PP", 20.0, 32.0, 18.0, "left baseline", "#fff")
        val base = PanelDraw.rect(0.0, 0.0, 430.0, 410.0, 20.0, PanelColor.middle())
        val circle = PanelDraw.circle(215.0, 126.0, 100.0, PanelColor.top())
        return "<g id=\"Component_T4\">$title$pies$labels$circle$base</g>"
    }

    private fun ppIndex(index: String): String = when (index) {
        "0" -> "0-50"; "50" -> "50-100"; "100" -> "100-200"; "200" -> "200-300"
        "300" -> "300-400"; "400" -> "400-500"; "500" -> "500-600"; "600" -> "600+"; else -> "?"
    }

    private fun ppColor(index: String): String = when (index) {
        "0" -> "#00B7EE"; "50" -> "#31B16C"; "100" -> "#FFF45C"; "200" -> "#F19149"
        "300" -> "#EC6841"; "400" -> "#EA68A2"; "500" -> "#AD5DA1"; "600" -> "#5F52A0"; else -> "#aaa"
    }

    private val TEMPLATE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
        <defs><clipPath id="clippath-PT-1"><rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/></clipPath></defs>
        <g id="Banner"><rect width="1920" height="320" rx="20" ry="20" style="fill: #1c1719;"/><g style="clip-path: url(#clippath-PT-1);"></g></g>
        <g id="Background"><rect y="290" width="1920" height="790" rx="20" ry="20" style="fill: #2a2226;"/></g>
        <g id="Main_Card"></g><g id="Body_Card"></g>
        <g id="IndexBase"><rect x="510" y="40" width="195" height="60" rx="15" ry="15" style="fill: #382e32;"/></g>
        <g id="Index"></g></svg>
    """.trimIndent()
}
