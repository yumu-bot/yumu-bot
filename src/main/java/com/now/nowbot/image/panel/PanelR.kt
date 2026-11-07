package com.now.nowbot.image.panel

import com.now.nowbot.image.component.j.PanelJSupport
import com.now.nowbot.image.component.j2.PanelJ2Components
import com.now.nowbot.image.component.j2.J2LabelData
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.calculate.RosuPerformance
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService.PanelRParam
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

data class RRenderResult(val svg: String, val report: RenderReport)

object PanelR {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-PR-1);\" filter=\"url(#blur-PR-1)\">"
    private const val MARK_BODY = "<g id=\"BodyCard\">"

    fun toSvg(param: PanelRParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(param, lease, remote).svg

    fun render(param: PanelRParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): RRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return RRenderResult(render(param, assets, report), report)
    }

    fun render(param: PanelRParam, assets: ImageAssets, report: RenderReport = RenderReport()): String {
        val set = param.beatmapset
        val beatmap = param.beatmap
        val expected = param.expected
        val pp = param.ppDistribution as? RosuPerformance
        val background = assets.mapBackground(set, "fullsize")

        var svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\" viewBox=\"0 0 1920 1080\">")
            append("<defs><clipPath id=\"clippath-PR-1\"><rect width=\"1920\" height=\"1080\"/></clipPath>")
            append("<filter id=\"blur-PR-1\" height=\"130%\" width=\"130%\" x=\"-15%\" y=\"-15%\" filterUnits=\"userSpaceOnUse\">")
            append("<feGaussianBlur stdDeviation=\"15\"/></filter></defs>")
            append("<g><rect width=\"1920\" height=\"1080\" style=\"fill:#1c1719\"/>")
            append("<g style=\"clip-path: url(#clippath-PR-1);\" filter=\"url(#blur-PR-1)\"></g></g>")
            append("<g id=\"BodyCard\"></g><g id=\"Index\"></g></svg>")
        }

        val header = componentHeader(set, beatmap, expected.mods, expected.isLazer)
        val meta = componentMeta(set)
        val fail = componentFail(beatmap)
        val settings = componentSettings(beatmap, param.original)
        val performance = componentPerformance(pp, param.ppDistribution.pp, param.ppList, beatmap)
        val related = componentRelated(set, beatmap)

        svg = Svg.setText(
            svg,
            Svg.group(40.0, 40.0, header) +
                Svg.group(40.0, 410.0, meta) +
                Svg.group(40.0, 730.0, fail) +
                Svg.group(880.0, 40.0, settings) +
                Svg.group(880.0, 360.0, performance) +
                Svg.group(880.0, 680.0, related),
            MARK_BODY,
        )
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Map Statistics v2.1 (!ymm)", ""), MARK_INDEX)
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.4) }, MARK_BACKGROUND)
        return svg
    }

    private fun componentHeader(set: com.now.nowbot.model.osu.Beatmapset, beatmap: Beatmap, mods: List<LazerMod>, lazer: Boolean): String {
        val titleFont = Fonts.puHuiTi
        val title = set.title.ifBlank { beatmap.difficultyName }
        val artist = set.artist
        val rank = beatmap.starRating
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 810.0, 350.0, 20.0, "#382e32"))
            append(titleFont.textPath(titleFont.cutStringTail(title, 44.0, 720.0), 20.0, 70.0, 44.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath(artist, 20.0, 108.0, 24.0, "left baseline", "#ddd"))
            append(Fonts.poppinsBold.textPath("mapped by ${set.creator}", 20.0, 140.0, 20.0, "left baseline", "#aaa"))
            append(Fonts.torus.get2SizeTextPath(Numbers.floor(rank, 2), "*", 48.0, 26.0, 20.0, 210.0, "left baseline", Colors.getStarRatingColor(rank)))
            append(Fonts.poppinsBold.textPath("[${beatmap.difficultyName}]", 160.0, 200.0, 22.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("${beatmap.totalLength / 60}:${(beatmap.totalLength % 60).toString().padStart(2, '0')}  ${Numbers.floor(beatmap.bpm, 1)} BPM", 160.0, 228.0, 20.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath(set.status.ifBlank { beatmap.status }, 780.0, 45.0, 20.0, "right baseline", Colors.getRankColor(if (beatmap.ranked > 0) "S" else "F")))
            append(Fonts.poppinsBold.textPath(if (lazer) "LAZER" else "STABLE", 780.0, 72.0, 18.0, "right baseline", "#fff"))
            var x = 20.0
            mods.forEach { mod ->
                val label = mod.acronym
                val width = max(Fonts.poppinsBold.width(label, 18.0) + 20.0, 42.0)
                append(PanelDraw.rect(x, 275.0, width, 34.0, 17.0, Colors.getModColor(label)))
                append(Fonts.poppinsBold.textPath(label, x + width / 2, 298.0, 18.0, "center baseline", "#fff"))
                x += width + 8.0
            }
            if (mods.isEmpty()) append(Fonts.poppinsBold.textPath("NM", 20.0, 298.0, 20.0, "left baseline", "#aaa"))
        }
    }

    private fun componentMeta(set: com.now.nowbot.model.osu.Beatmapset): String {
        val tags = set.tags.split(" ").filter { it.isNotBlank() }.take(8).joinToString(" ")
        val related = set.relatedTags.take(5).joinToString(" ") { it.type }
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 810.0, 300.0, 20.0, "#382e32"))
            append(Fonts.poppinsBold.textPath("Mapper", 15.0, 27.0, 18.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath(set.creator, 15.0, 55.0, 26.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Genre / Language", 15.0, 95.0, 18.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath("${set.genre?.name ?: "unknown"} / ${set.language?.name ?: "unknown"}", 15.0, 122.0, 20.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Source", 15.0, 162.0, 18.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(set.source.ifBlank { "-" }, 20.0, 760.0), 15.0, 189.0, 20.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Tags", 15.0, 229.0, 18.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(tags.ifBlank { "-" }, 16.0, 760.0), 15.0, 256.0, 16.0, "left baseline", "#B8D3E0"))
            if (related.isNotBlank()) append(Fonts.poppinsBold.textPath(related, 795.0, 27.0, 16.0, "right baseline", "#aaa"))
        }
    }

    private fun componentFail(beatmap: Beatmap): String {
        val play = beatmap.playCount.toDouble()
        val passRate = if (play > 0) ((beatmap.passCount ?: 0) / play).coerceIn(0.0, 1.0) else 0.0
        val retries = beatmap.retries
        val fails = beatmap.fails
        val retrySum = retries.sum().toDouble()
        val failSum = fails.sum().toDouble()
        val total = (retrySum + failSum).takeIf { it > 0 } ?: 1.0
        val retryRate = retrySum / total
        val quitRate = failSum / total
        val retryBars = downsample(retries, 39)
        val failBars = downsample(fails, 39)
        val maxBar = max((retryBars + failBars).maxOrNull() ?: 1, 1)
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 810.0, 310.0, 20.0, "#382e32"))
            append(Fonts.poppinsBold.textPath("Pass / Retry / Quit", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Pass", 15.0, 55.0, 16.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath("Retry", 290.0, 55.0, 16.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath("Quit", 565.0, 55.0, 16.0, "left baseline", "#aaa"))
            append(PanelDraw.rect(15.0, 70.0, 240.0, 16.0, 8.0, "#46393f"))
            append(PanelDraw.rect(15.0, 70.0, 240.0 * passRate, 16.0, 8.0, "#7CC576"))
            append(PanelDraw.rect(290.0, 70.0, 240.0, 16.0, 8.0, "#46393f"))
            append(PanelDraw.rect(290.0, 70.0, 240.0 * retryRate, 16.0, 8.0, "#F6D659"))
            append(PanelDraw.rect(565.0, 70.0, 240.0, 16.0, 8.0, "#46393f"))
            append(PanelDraw.rect(565.0, 70.0, 240.0 * quitRate, 16.0, 8.0, "#ED6C9E"))
            append(Fonts.poppinsBold.textPath("${Math.round(100 * passRate)}%", 255.0, 114.0, 16.0, "right baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("${Math.round(100 * retryRate)}%", 530.0, 114.0, 16.0, "right baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("${Math.round(100 * quitRate)}%", 805.0, 114.0, 16.0, "right baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Retry", 15.0, 145.0, 16.0, "left baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath("Fail", 410.0, 145.0, 16.0, "left baseline", "#aaa"))
            append(retryBars.mapIndexed { index, value -> PanelDraw.rect(15.0 + index * 10.0, 290.0 - value.toDouble() / maxBar * 100.0, 8.0, value.toDouble() / maxBar * 100.0, 2.0, "#F6D659") }.joinToString(""))
            append(failBars.mapIndexed { index, value -> PanelDraw.rect(410.0 + index * 10.0, 290.0 - value.toDouble() / maxBar * 100.0, 8.0, value.toDouble() / maxBar * 100.0, 2.0, "#ED6C9E") }.joinToString(""))
        }
    }

    private fun componentSettings(beatmap: Beatmap, original: Map<String, Any>): String {
        fun originalValue(key: String): String = (original[key] as? Number)?.let { Numbers.floor(it.toDouble(), 2) } ?: "-"
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 1000.0, 300.0, 20.0, "#382e32"))
            append(Fonts.poppinsBold.textPath("Difficulty / Objects", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            val rows = listOf(
                "AR" to "${Numbers.floor(beatmap.ar ?: 0.0, 2)} (${originalValue("ar")})",
                "OD" to "${Numbers.floor(beatmap.od ?: 0.0, 2)} (${originalValue("od")})",
                "CS" to "${Numbers.floor(beatmap.cs ?: 0.0, 2)} (${originalValue("cs")})",
                "HP" to "${Numbers.floor(beatmap.hp ?: 0.0, 2)} (${originalValue("hp")})",
                "BPM" to Numbers.floor(beatmap.bpm, 2),
            )
            rows.forEachIndexed { index, (name, value) ->
                append(Fonts.poppinsBold.textPath(name, 15.0, 65.0 + index * 38.0, 20.0, "left baseline", "#aaa"))
                append(Fonts.poppinsBold.textPath(value, 390.0, 65.0 + index * 38.0, 20.0, "right baseline", "#fff"))
            }
            val objects = listOf(
                "Circles" to (beatmap.circles ?: 0),
                "Sliders" to (beatmap.sliders ?: 0),
                "Spinners" to (beatmap.spinners ?: 0),
                "Max Combo" to (beatmap.maxCombo ?: 0),
                "Length" to "${beatmap.totalLength / 60}:${(beatmap.totalLength % 60).toString().padStart(2, '0')}",
            )
            objects.forEachIndexed { index, (name, value) ->
                append(Fonts.poppinsBold.textPath(name, 500.0, 65.0 + index * 38.0, 20.0, "left baseline", "#aaa"))
                append(Fonts.poppinsBold.textPath(value.toString(), 970.0, 65.0 + index * 38.0, 20.0, "right baseline", "#fff"))
            }
            append(Fonts.poppinsBold.textPath("Mode: ${GameModes.getGameMode(beatmap.mode, 1)}", 500.0, 260.0, 20.0, "left baseline", "#fff"))
        }
    }

    private fun componentPerformance(pp: RosuPerformance?, totalPp: Double, ppList: List<Double>, beatmap: Beatmap): String {
        val values = if (pp == null) emptyList() else listOfNotNull(
            pp.pp.takeIf { it > 0 }?.let { "Total" to it },
            pp.ppAim?.takeIf { it > 0 }?.let { "Aim" to it },
            pp.ppSpeed?.takeIf { it > 0 }?.let { "Speed" to it },
            pp.ppAccuracy?.takeIf { it > 0 }?.let { "Accuracy" to it },
            pp.ppReading?.takeIf { it > 0 }?.let { "Reading" to it },
            pp.ppFlashlight?.takeIf { it > 0 }?.let { "Flashlight" to it },
            pp.ppDifficulty?.takeIf { it > 0 }?.let { "Difficulty" to it },
        )
        val maxPp = values.maxOfOrNull { it.second } ?: 1.0
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 1000.0, 300.0, 20.0, "#382e32"))
            append(Fonts.poppinsBold.textPath("Performance", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(Fonts.torus.get2SizeTextPath(Numbers.floor(totalPp, 1), "PP", 48.0, 24.0, 970.0, 70.0, "right baseline", "#fff"))
            values.forEachIndexed { index, (name, value) ->
                val y = 90.0 + index * 28.0
                append(Fonts.poppinsBold.textPath(name, 15.0, y + 14.0, 16.0, "left baseline", "#aaa"))
                append(PanelDraw.rect(140.0, y, 730.0, 12.0, 6.0, "#46393f"))
                append(PanelDraw.rect(140.0, y, 730.0 * value / maxPp, 12.0, 6.0, Colors.getStarRatingColor(index.toDouble())))
                append(Fonts.poppinsBold.textPath(Numbers.floor(value, 1), 900.0, y + 14.0, 16.0, "right baseline", "#fff"))
            }
            val ppMax = ppList.maxOrNull() ?: 1.0
            val ppBars = ppList.take(13)
            ppBars.forEachIndexed { index, value ->
                val height = value / ppMax * 34.0
                append(PanelDraw.rect(15.0 + index * 24.0, 288.0 - height, 18.0, height, 3.0, "#FFCC22"))
            }
            append(Fonts.poppinsBold.textPath("PP list", 970.0, 260.0, 16.0, "right baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath("Stars: ${Numbers.floor(beatmap.starRating, 2)}", 500.0, 260.0, 16.0, "left baseline", "#aaa"))
        }
    }

    private fun componentRelated(set: com.now.nowbot.model.osu.Beatmapset, beatmap: Beatmap): String {
        val diffs = set.beatmaps.orEmpty().sortedBy { it.starRating }.take(9)
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 1000.0, 360.0, 20.0, "#382e32"))
            append(Fonts.poppinsBold.textPath("Difficulties", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            diffs.forEachIndexed { index, diff ->
                val x = 15.0 + (index % 3) * 325.0
                val y = 45.0 + (index / 3) * 100.0
                val color = Colors.getStarRatingColor(diff.starRating)
                append(PanelDraw.rect(x, y, 310.0, 88.0, 12.0, if (diff.beatmapID == beatmap.beatmapID) "#46393f" else "#2a2226"))
                append(PanelDraw.rect(x, y, 6.0, 88.0, 3.0, color))
                append(Fonts.poppinsBold.textPath("${Numbers.floor(diff.starRating, 2)}*", x + 18.0, y + 32.0, 24.0, "left baseline", color))
                append(Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(diff.difficultyName, 16.0, 230.0), x + 18.0, y + 58.0, 16.0, "left baseline", "#fff"))
                append(Fonts.poppinsBold.textPath(GameModes.getGameMode(diff.mode, -1), x + 292.0, y + 32.0, 24.0, "right baseline", "#aaa"))
                append(Fonts.poppinsBold.textPath("M${diff.mode.safeModeValue}", x + 292.0, y + 58.0, 14.0, "right baseline", "#aaa"))
            }
        }
    }

    private fun downsample(values: IntArray, size: Int): List<Int> {
        if (values.isEmpty()) return List(size) { 0 }
        return List(size) { index -> values.getOrElse(index * values.size / size) { 0 } }
    }
}
