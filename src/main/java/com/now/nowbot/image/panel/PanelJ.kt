package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.j.PanelJSupport
import com.now.nowbot.image.component.j2.PanelJ2Components
import com.now.nowbot.image.component.profile.MascotAssets
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService
import kotlin.math.max
import kotlin.math.min

data class JRenderResult(val svg: String, val report: RenderReport)

object PanelJ {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PJ-1);\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-PJ-BG);\">"
    private const val MARK_MAIN_CARD = "<g id=\"MainCard\">"
    private const val MARK_TOP_BP = "<g id=\"TopBP\">"
    private const val MARK_LAST_BP = "<g id=\"LastBP\">"
    private const val MARK_CARD_L = "<g id=\"Card_L\">"
    private const val MARK_LABEL_J1 = "<g id=\"Label_J1\">"
    private const val MARK_LABEL_J2 = "<g id=\"Label_J2\">"
    private const val MARK_LABEL_J3 = "<g id=\"Label_J3\">"
    private const val MARK_PP_GRAPH = "<g id=\"BPRankGraph\">"
    private const val MARK_RRECT = "<g id=\"BPRanksR\">"
    private const val MARK_MOD_PAN = "<g style=\"clip-path: url(#clippath-PJ-2);\">"
    private const val MARK_RANK_PAN = "<g style=\"clip-path: url(#clippath-PJ-3);\">"

    fun toSvg(body: Map<String, Any?>, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(body, lease, remote).svg

    fun toSvg(param: BPAnalysisService.BAParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(param.toMap(1), lease, remote).svg

    fun render(body: Map<String, Any?>, lease: RenderResourceLease, remote: RemoteImageSource? = null): JRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return JRenderResult(render(body, assets, report), report)
    }

    fun render(param: BPAnalysisService.BAParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): JRenderResult =
        render(param.toMap(1), lease, remote)

    fun render(body: Map<String, Any?>, assets: ImageAssets, report: RenderReport = RenderReport()): String {
        val user = body["card_A1"] as? OsuUser ?: throw IllegalArgumentException("J 需要 card_A1")
        val tops = readList<LazerScore>(body, "bpTop5")
        val lasts = readList<LazerScore>(body, "bpLast5")
        val length = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "bpLength")
        val combo = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "bpCombo")
        val star = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "bpSR")
        require(length.isNotEmpty() && combo.isNotEmpty() && star.isNotEmpty()) {
            "J 缺少 bpLength/bpCombo/bpSR 统计"
        }
        val mappers = readList<BPAnalysisService.Companion.Mapper>(body, "favorite_mappers")
        val mapperCount = (body["favorite_mappers_count"] as? Number)?.toInt() ?: mappers.size
        val ppRawArr = readList<Double>(body, "pp_raw_arr")
        val rankArr = readList<String>(body, "rank_arr")
        val rankElect = readList<String>(body, "rank_elect_arr")
        val bpLengthArr = readList<Int>(body, "bp_length_arr")
        val modsAttr = readList<BPAnalysisService.Companion.Attribute>(body, "mods_attr")
        val rankAttr = readList<BPAnalysisService.Companion.Attribute?>(body, "rank_attr")
        val bestPp = (body["pp_raw"] as? Number)?.toDouble() ?: 0.0
        val pp = (body["pp"] as? Number)?.toDouble() ?: 0.0
        val mode = when (val value = body["game_mode"]) {
            is OsuMode -> value
            is String -> value.toOsuMode()
            else -> OsuMode.OSU
        }

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_J.svg"))

        val ppFixed = Numbers.jsRound(pp).toLong()
        val ppRawFixed = Numbers.jsRound(bestPp).toLong()
        val ppBonus = max(ppFixed - ppRawFixed, 0L)
        val header = PanelBatchSupport.panelName("BP Analysis v2 (!ymba)", "BA") +
            Fonts.torus.get2SizeTextPath(ppFixed.toString(), " PP ($ppRawFixed+$ppBonus)", 36.0, 24.0, 1860.0, 374.754, "right baseline", "#fff") +
            Fonts.torus.textPath(GameModes.getGameMode(mode, 2), 1860.0, 401.836, 24.0, "right baseline", "#fff") +
            Fonts.torus.get2SizeTextPath(mapperCount.toString(), " mappers", 36.0, 24.0, 1500.0, 724.754, "right baseline", "#aaa") +
            fixedTitles()
        svg = Svg.setText(svg, header, MARK_INDEX)

        val cardA1 = CardA1.render(user, null, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_MAIN_CARD)

        svg = Svg.setText(
            svg,
            tops.take(5).mapIndexed { index, score -> Svg.group(55.0, 380.0 + index * 95.0, PanelJSupport.scoreRowCard(score, assets)) }.joinToString("\n"),
            MARK_TOP_BP,
        )
        svg = Svg.setText(
            svg,
            lasts.take(5).mapIndexed { index, score -> Svg.group(380.0, 380.0 + index * 95.0, PanelJSupport.scoreRowCard(score, assets)) }.joinToString("\n"),
            MARK_LAST_BP,
        )

        val cards = listOf(
            50.0 to PanelJSupport.componentK("Length", length, assets, ""),
            355.0 to PanelJSupport.componentK("Combo", combo, assets, ""),
            660.0 to PanelJSupport.componentK("Star Rating", star, assets, ""),
        )
        svg = Svg.setText(svg, cards.joinToString("\n") { (x, body) -> Svg.group(x, 880.0, body) }, MARK_CARD_L)

        val modLabels = modsAttr.take(4).mapIndexed { index, attr ->
            Svg.group(740.0, 570.0 + index * 70.0, PanelJSupport.labelJ1(attr.index, attr.index, attr.mapCount, attr.ppCount))
        }.joinToString("\n")
        svg = Svg.setText(svg, modLabels, MARK_LABEL_J1)

        val mapperLabels = mappers.take(6).mapIndexed { index, mapper ->
            val x = 992.0 + (index % 2) * 260.0
            val y = 742.0 + (index / 2) * 95.0
            Svg.group(x, y, PanelJSupport.labelJ2(index + 1, mapper.avatarUrl, mapper.username, mapper.mapCount, mapper.ppCount, assets))
        }.joinToString("\n")
        svg = Svg.setText(svg, mapperLabels, MARK_LABEL_J2)

        val rankValues = rankAttr.filterNotNull()
        val rankLabels = rankValues.take(6).mapIndexed { index, attr ->
            val x = if (index < 1) 1555.0 else 1715.0
            val y = if (index < 1) 902.0 else 702.0 + 66.0 * (index - 1)
            Svg.group(x, y, PanelJSupport.labelJ3(attr.index, attr.mapCount, attr.ppCount, attr.percent, assets))
        }.joinToString("\n")
        svg = Svg.setText(svg, rankLabels, MARK_LABEL_J3)

        val ppMax = ppRawArr.maxOrNull() ?: 1.0
        val ppMin = ppRawArr.minOrNull() ?: 0.0
        val ppMid = (ppMax + ppMin) / 2
        svg = Svg.setText(svg, PanelJSupport.lineChart(ppRawArr.take(100), ppMax, ppMin, 1040.0, 400.0, 780.0, 215.0, "#FFCC22"), MARK_PP_GRAPH)
        svg = Svg.setText(
            svg,
            Fonts.torus.textPath(Numbers.jsRound(ppMax).toLong().toString(), 1010.0, 402.836, 24.0, "center baseline", "#fc2") +
                Fonts.torus.textPath(Numbers.jsRound(ppMid).toLong().toString(), 1010.0, 509.836, 24.0, "center baseline", "#fc2") +
                Fonts.torus.textPath(Numbers.jsRound(ppMin).toLong().toString(), 1010.0, 616.836, 24.0, "center baseline", "#fc2"),
            MARK_PP_GRAPH,
        )

        val paddedLength = List(100) { bpLengthArr.getOrElse(it) { 0 } }
        val barLengths = List(39) { paddedLength[it * paddedLength.size / 39] }
        val lengthMax = barLengths.maxOrNull() ?: 0
        val lengthMin = barLengths.minOrNull() ?: 0
        val delta = (lengthMax - lengthMin).takeIf { it > 0 } ?: 1
        val barSvg = buildString {
            barLengths.forEachIndexed { index, value ->
                val height = Numbers.clampToInteger((value - lengthMin).toDouble() / delta * 90.0, 90.0, 16.0)
                val rank = rankElect.getOrElse(index * rankElect.size / 39) { rankArr.getOrElse(index) { "F" } }
                append(PanelDraw.rect(1042.0 + 20.0 * index, 620.0 - height, 16.0, height, 8.0, Colors.getRankColor(rank)))
            }
            val maxIndex = barLengths.indexOf(lengthMax).coerceAtLeast(0)
            append(Fonts.torus.textPath(
                "${lengthMax / 60}:${(lengthMax % 60).toString().padStart(2, '0')}",
                1050.0 + maxIndex * 20.0,
                515.0 + 90.0 - min(lengthMax, 5) / 5.0 * 90.0 + 10.0,
                16.0, "center baseline", "#aaa",
            ))
        }
        svg = Svg.setText(svg, barSvg, MARK_RRECT)

        svg = Svg.setText(svg, pie(modsAttr.map { it.index to it.percent }, 842.0, 470.0, 100.0, true, assets), MARK_MOD_PAN)
        svg = Svg.setText(svg, pie(rankValues.filter { it.index != "FC" }.map { it.index to it.percent }, 1630.0, 815.0, 100.0, false, assets), MARK_RANK_PAN)

        val banner = assets.resolveRef(user.profileBanners.firstOrNull()?.image) ?: assets.randomBanner()
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        val background = MascotAssets.rankBackground(user, assets)
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        return svg
    }

    private fun pie(values: List<Pair<String, Double>>, cx: Double, cy: Double, radius: Double, mod: Boolean, assets: ImageAssets): String {
        if (values.isEmpty()) return ""
        val overlay = assets.local("object-piechart-overlay2.png")?.let { Svg.image(cx - radius, cy - radius, radius * 2, radius * 2, it) }.orEmpty()
        var degrees = 0.0
        val slices = values.joinToString("") { (index, percent) ->
            val start = degrees
            val end = (degrees + percent).coerceAtMost(1.0)
            degrees = end
            val color = if (mod) Colors.getModColor(index) else Colors.getRankColor(index)
            PanelJ2Components.pie(cx, cy, radius, start, end, color)
        }
        return slices + overlay
    }

    private fun fixedTitles(): String {
        return Fonts.torus.textPath("BP 100", 1745.025, 646.836, 24.0, "left baseline", "#fc2") +
            Fonts.torus.textPath("BP 50", 1397.881, 646.836, 24.0, "left baseline", "#fc2") +
            Fonts.torus.textPath("BP 1", 1040.0, 646.836, 24.0, "left baseline", "#fc2") +
            Fonts.torus.textPath("BP Distribution", 1000.0, 370.795, 30.0, "left baseline", "#fff") +
            Fonts.torus.textPath("Favorite Mappers", 1000.0, 725.795, 30.0, "left baseline", "#fff") +
            Fonts.torus.textPath("Ranks", 1560.0, 725.795, 30.0, "left baseline", "#fff") +
            Fonts.torus.textPath("Mods", 740.0, 365.795, 30.0, "left baseline", "#fff") +
            Fonts.torus.textPath("Last 5 BPs", 380.0, 365.795, 30.0, "left baseline", "#fff") +
            Fonts.torus.textPath("Top 5 BPs", 55.0, 365.795, 30.0, "left baseline", "#fff")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> readList(body: Map<String, Any?>, key: String): List<T> {
        return when (val value = body[key]) {
            null -> emptyList()
            is List<*> -> value as List<T>
            is Array<*> -> value.toList() as List<T>
            is IntArray -> value.toList() as List<T>
            else -> throw IllegalArgumentException("J 字段 $key 类型不正确: ${value.javaClass.name}")
        }
    }
}
