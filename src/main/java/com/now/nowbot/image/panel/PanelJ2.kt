package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.j.PanelJSupport
import com.now.nowbot.image.component.j2.J2LabelData
import com.now.nowbot.image.component.j2.J2Mapper
import com.now.nowbot.image.component.j2.PanelJ2Components
import com.now.nowbot.image.component.profile.MascotAssets
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService
import kotlin.math.max

data class J2RenderResult(val svg: String, val report: RenderReport)

object PanelJ2 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PJ2-1);\">"
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PJ2-BG)\" style=\"clip-path: url(#clippath-PJ2-BG);\">"

    fun toSvg(
        param: BPAnalysisService.BAParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: BPAnalysisService.BAParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): J2RenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return J2RenderResult(render(param.toMap(2), assets, report), report)
    }

    fun toSvg(body: Map<String, Any?>, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(body, lease, remote).svg

    fun render(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): J2RenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return J2RenderResult(render(body, assets, report), report)
    }

    fun render(body: Map<String, Any?>, assets: ImageAssets, report: RenderReport = RenderReport()): String {
        val user = body["user"] as? OsuUser ?: throw IllegalArgumentException("J2 需要 user")
        val bests = readList<LazerScore>(body, "bests")
        val bpmAttr = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "bpm_attr")
        val lengthAttr = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "length_attr")
        val comboAttr = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "combo_attr")
        val starAttr = readList<BPAnalysisService.Companion.BeatmapAnalysis>(body, "star_attr")
        require(bpmAttr.isNotEmpty() && lengthAttr.isNotEmpty() && comboAttr.isNotEmpty() && starAttr.isNotEmpty()) {
            "J2 缺少 bpm/length/combo/star 统计"
        }
        val modsAttr = readList<BPAnalysisService.Companion.Attribute>(body, "mods_attr")
        val rankAttr = readList<BPAnalysisService.Companion.Attribute?>(body, "rank_attr")
        val mappers = readList<BPAnalysisService.Companion.Mapper>(body, "favorite_mappers")
        val ppRaw = readList<Double>(body, "pp_raw_arr")
        val rankArr = readList<String>(body, "rank_arr")
        val lengthArr = readList<Int>(body, "length_arr")
        val modsArr = readList<List<String>>(body, "mods_arr")
        val starArr = readList<Double>(body, "star_arr")
        val timeArr = readList<Double>(body, "time_arr")
        val timeDist = readList<Int>(body, "time_dist_arr")
        val ppSum = (body["pp_sum"] as? Number)?.toDouble() ?: 0.0
        val hue = user.profileHue.takeIf { it != 0 } ?: 342

        var svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\" viewBox=\"0 0 1920 1080\">")
            append("<defs>")
            append("<clipPath id=\"clippath-PJ2-1\"><rect width=\"1920\" height=\"320\" rx=\"20\" ry=\"20\"/></clipPath>")
            append("<clipPath id=\"clippath-PJ2-BG\"><rect y=\"290\" width=\"1920\" height=\"790\" rx=\"30\" ry=\"30\"/></clipPath>")
            append("<filter id=\"blur-PJ2-BG\" height=\"110%\" width=\"110%\" x=\"-5%\" y=\"-5%\" filterUnits=\"userSpaceOnUse\">")
            append("<feGaussianBlur stdDeviation=\"15\"/></filter></defs>")
            append("<g><rect width=\"1920\" height=\"320\" rx=\"20\" ry=\"20\" style=\"fill:${PanelColor.base(hue)}\"/></g>")
            append("<g id=\"Banner\"><g style=\"clip-path: url(#clippath-PJ2-1);\"></g></g>")
            append("<g><rect y=\"290\" width=\"1920\" height=\"790\" rx=\"30\" ry=\"30\" style=\"fill:${PanelColor.bottom(hue)}\"/>")
            append("<g filter=\"url(#blur-PJ2-BG)\" style=\"clip-path: url(#clippath-PJ2-BG);\"></g></g>")
            append("<g id=\"Component\"></g><g id=\"Card_A1\"></g>")
            append("<g><rect x=\"510\" y=\"40\" width=\"195\" height=\"60\" rx=\"15\" ry=\"15\" style=\"fill:${PanelColor.middle(hue)}\"/></g>")
            append("<g id=\"Index\"></g></svg>")
        }

        val labels = mutableListOf<String>()
        labels += PanelJSupport.labelJ8("object-score-beatsperminute.png", "BPM", "BPM", remark(bpmAttr), minB(bpmAttr, "bpm"), "", midB(bpmAttr, "bpm"), "", maxB(bpmAttr, "bpm"), "", 60.0, 300.0, bpmAttr.getOrNull(2)?.bpm?.toDouble() ?: 0.0, bpmAttr.getOrNull(0)?.bpm?.toDouble() ?: 0.0, "#00B7EE", assets)
        labels += PanelJSupport.labelJ8("object-score-length.png", "Length", "LN", remark(lengthAttr), minLength(lengthAttr).first, minLength(lengthAttr).second, midLength(lengthAttr).first, midLength(lengthAttr).second, maxLength(lengthAttr).first, maxLength(lengthAttr).second, 30.0, 530.0, (lengthAttr.getOrNull(2)?.length ?: 0).toDouble(), (lengthAttr.getOrNull(0)?.length ?: 0).toDouble(), "#7CC576", assets)
        labels += PanelJSupport.labelJ8("object-score-combo.png", "Combo", "CB", remark(comboAttr), (comboAttr.getOrNull(2)?.combo ?: 0).toString(), "", (comboAttr.getOrNull(1)?.combo ?: 0).toString(), "", (comboAttr.getOrNull(0)?.combo ?: 0).toString(), "", 0.0, 3000.0, (comboAttr.getOrNull(2)?.combo ?: 0).toDouble(), (comboAttr.getOrNull(0)?.combo ?: 0).toDouble(), "#7CC576", assets)
        labels += PanelJSupport.labelJ8("object-score-star.png", "Star Rating", "SR", remark(starAttr), Numbers.floor(starAttr.getOrNull(2)?.star ?: 0.0, 2), "", Numbers.floor(starAttr.getOrNull(1)?.star ?: 0.0, 2), "", Numbers.floor(starAttr.getOrNull(0)?.star ?: 0.0, 2), "", 0.0, 9.0, starAttr.getOrNull(2)?.star ?: 0.0, starAttr.getOrNull(0)?.star ?: 0.0, "#FFCC22", assets)

        val j1 = buildString {
            append(PanelDrawRect(0.0, 0.0, 490.0, 390.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Main Statistics", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            labels.forEachIndexed { index, label -> append(Svg.group(20.0, 45.0 + index * 85.0, label)) }
        }

        val j2 = buildString {
            append(PanelDrawRect(0.0, 0.0, 490.0, 300.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Bests", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            bests.take(6).forEachIndexed { index, score ->
                val x = 10.0 + (index % 3) * 160.0
                val y = 40.0 + (index / 3) * 130.0
                append(Svg.group(x, y, PanelJSupport.scoreCardD2(score, assets)))
            }
        }

        val rankValues = rankAttr.filterNotNull().filterNot { it.index == "FC" && it.mapCount <= 0 }
        val rankMaxWidth = when {
            rankValues.size <= 2 -> 380.0
            rankValues.size <= 4 -> 185.0
            else -> 120.0
        }
        val rankCols = when {
            rankValues.size <= 2 -> 1
            rankValues.size <= 4 -> 2
            else -> 3
        }
        val j3 = buildString {
            append(PanelDrawRect(0.0, 0.0, 400.0, 390.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Ranks", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            var degrees = 0.0
            rankValues.forEach { attr ->
                val end = degrees + attr.percent
                append(PanelJ2Components.pie(200.0, 136.0, 95.0, degrees, end, Colors.getRankColor(attr.index)))
                degrees = end
            }
            rankValues.take(6).forEachIndexed { index, attr ->
                val x = 10.0 + (index % rankCols) * (rankMaxWidth + 10.0)
                val y = 250.0 + (index / rankCols) * 70.0
                append(
                    PanelJ2Components.labelJ4(
                        J2LabelData(attr.index.replace("X", "SS"), attr.index, "${Numbers.jsRound(attr.ppCount).toLong()} PP", attr.mapCount.toString().padStart(3, '0'), attr.percent, Colors.getRankColor(attr.index), rankMaxWidth),
                        x, y,
                    ),
                )
            }
        }

        val modMaxWidth = when {
            modsAttr.size <= 2 -> 380.0
            modsAttr.size <= 4 -> 185.0
            else -> 120.0
        }
        val modCols = when {
            modsAttr.size <= 2 -> 1
            modsAttr.size <= 4 -> 2
            else -> 3
        }
        val j4 = buildString {
            append(PanelDrawRect(0.0, 0.0, 400.0, 390.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Mods", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            var degrees = 0.0
            modsAttr.forEach { attr ->
                val end = degrees + attr.percent
                append(PanelJ2Components.pie(200.0, 136.0, 95.0, degrees, end, Colors.getModColor(attr.index)))
                degrees = end
            }
            if (modsAttr.isEmpty()) {
                append(
                    PanelJ2Components.labelJ4(
                        J2LabelData("No Mod", "NM", "${Numbers.jsRound(ppSum).toLong()} PP", bests.size.toString().padStart(3, '0'), (bests.size / 200.0).coerceIn(0.0, 1.0), "#EEE", 380.0),
                        10.0, 250.0,
                    ),
                )
            }
            modsAttr.take(6).forEachIndexed { index, attr ->
                val x = 10.0 + (index % modCols) * (modMaxWidth + 10.0)
                val y = 250.0 + (index / modCols) * 70.0
                append(
                    PanelJ2Components.labelJ4(
                        J2LabelData(attr.index, attr.index, "${Numbers.jsRound(attr.ppCount).toLong()} PP", attr.mapCount.toString().padStart(3, '0'), attr.percent, Colors.getModColor(attr.index), modMaxWidth),
                        x, y,
                    ),
                )
            }
        }

        val j5 = buildString {
            append(PanelDrawRect(0.0, 0.0, 820.0, 300.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Bests Distribution", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Ranks / Star Rating", 805.0, 22.0, 12.0, "right baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Mods / Length", 804.0, 288.0, 12.0, "right baseline", "#fff"))
            append(PanelJSupport.barChart(starArr.map { it.toInt() }, max(starArr.maxOrNull() ?: 1.0, 1.0), 150.0, 0.0, 798.0, 110.0, rankArr.map { Colors.getRankColor(it) }, 2.0))
            append(PanelJSupport.barChart(lengthArr, max(lengthArr.maxOrNull() ?: 1, 1).toDouble(), 180.0, 0.0, 798.0, 110.0, modsArr.map { Colors.getModColor(it.firstOrNull()) }, 2.0))
            append(Fonts.poppinsBold.textPath("#1", 10.0, 170.0, 14.0, "left baseline"))
            append(Fonts.poppinsBold.textPath("#50", 410.0, 170.0, 14.0, "center baseline"))
            append(Fonts.poppinsBold.textPath("#100", 810.0, 170.0, 14.0, "right baseline"))
        }

        val large = if (ppRaw.size > 200) ppRaw.take(200) else ppRaw
        val ppMax = large.maxOrNull() ?: 1.0
        val ppMin = large.minOrNull() ?: 0.0
        val average = if (large.isEmpty()) 0 else (large.sum() / large.size).toInt()
        val j6 = buildString {
            append(PanelDrawRect(0.0, 0.0, 490.0, 180.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("PP", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Average: $average PP", 475.0, 27.0, 18.0, "right baseline", "#fff"))
            append(PanelJSupport.lineChart(large, ppMax, ppMin, 50.0, 155.0, 415.0, 115.0, "#fc2"))
            append(Fonts.poppinsBold.textPath(Numbers.floor(ppMax, 0), 15.0, 48.0, 14.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath(Numbers.floor((ppMax + ppMin) / 2, 0), 15.0, 101.0, 14.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath(Numbers.floor(ppMin, 0), 15.0, 154.0, 14.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("#1", 35.0, 170.0, 14.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("#100", 257.5, 170.0, 14.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("#200", 480.0, 170.0, 14.0, "right baseline", "#fff"))
        }

        val j7 = buildString {
            append(PanelDrawRect(0.0, 0.0, 490.0, 190.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Time Distribution", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(PanelJSupport.barChart(timeDist, max(timeDist.maxOrNull() ?: 1, 1).toDouble(), 40.0, 0.0, 460.0, 120.0, listOf("#0075A9", "#00736D", "#007130", "#0D7D25", "#648C0C", "#B7AB00", "#AD6B00", "#A84200"), 4.0))
            append(PanelJ2Components.scatter(15.0, 40.0, 460.0, 120.0, 4.0, timeArr, ppRaw, rankArr.map { Colors.getRankColor(it) }, 0.0, 24.0, ppRaw.minOrNull() ?: 0.0, ppRaw.maxOrNull() ?: 1.0))
            for (i in 0..8) {
                append(Fonts.poppinsBold.textPath((i * 3).toString(), 13.0 + i * (460.0 / 8), 180.0, 14.0, "center baseline"))
            }
            val maxIndex = timeDist.indices.maxByOrNull { timeDist.getOrElse(it) { 0 } } ?: 0
            append(Fonts.poppinsBold.textPath("Max: ${timeDist.getOrElse(maxIndex) { 0 }}x [${maxIndex * 3}-${(maxIndex + 1) * 3}]", 475.0, 27.0, 18.0, "right baseline"))
        }

        val j8 = buildString {
            append(PanelDrawRect(0.0, 0.0, 490.0, 300.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Favorite Mappers", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            val j2Mappers = mappers.map { J2Mapper(it.username, it.avatarUrl, it.mapCount, it.ppCount) }
            j2Mappers.firstOrNull()?.let { append(PanelJ2Components.labelJ7(it, hue, assets)) }
            j2Mappers.drop(1).take(6).forEachIndexed { index, mapper ->
                val x = 10.0 + (index % 2) * 235.0
                val y = 105.0 + (index / 2) * 65.0
                append(Svg.group(x, y, PanelJ2Components.labelJ6(mapper, index + 2, hue, assets)))
            }
        }

        svg = Svg.setText(svg, PanelBatchSupport.panelName("BP Analysis v3 (!ymba)", "BA"), MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA1.render(user, null, assets)), MARK_CARD_A1)
        svg = Svg.setText(
            svg,
            Svg.group(40.0, 330.0, j1) + Svg.group(40.0, 740.0, j2) + Svg.group(550.0, 330.0, j3) +
                Svg.group(970.0, 330.0, j4) + Svg.group(550.0, 740.0, j5) + Svg.group(1390.0, 330.0, j6) +
                Svg.group(1390.0, 530.0, j7) + Svg.group(1390.0, 740.0, j8),
            MARK_COMPONENT,
        )
        val banner = assets.resolveRef(user.profileBanners.firstOrNull()?.image) ?: assets.randomBanner()
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7, "xMidYMid slice") }, MARK_BANNER)
        val background = MascotAssets.rankBackground(user, assets) ?: assets.localBanner()
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        return svg
    }

    private fun PanelDrawRect(x: Double, y: Double, w: Double, h: Double, color: String): String {
        return com.now.nowbot.image.draw.PanelDraw.rect(x, y, w, h, 20.0, color)
    }

    private fun remark(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>): String {
        return "#${attr.getOrNull(2)?.ranking ?: "?"} - #${attr.getOrNull(0)?.ranking ?: "?"} [#${attr.getOrNull(1)?.ranking ?: "?"}]"
    }

    private fun minB(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>, field: String): String {
        val item = attr.getOrNull(2) ?: return "0"
        return if (field == "bpm") Numbers.floor(item.bpm, 0) else "0"
    }

    private fun midB(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>, field: String): String {
        val item = attr.getOrNull(1) ?: return "0"
        return if (field == "bpm") Numbers.floor(item.bpm, 0) else "0"
    }

    private fun maxB(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>, field: String): String {
        val item = attr.getOrNull(0) ?: return "0"
        return if (field == "bpm") Numbers.floor(item.bpm, 0) else "0"
    }

    private fun minLength(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>): Pair<String, String> {
        val value = attr.getOrNull(2)?.length ?: 0
        return (value / 60).toString() + ":" to (value % 60).toString().padStart(2, '0')
    }

    private fun midLength(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>): Pair<String, String> {
        val value = attr.getOrNull(1)?.length ?: 0
        return (value / 60).toString() + ":" to (value % 60).toString().padStart(2, '0')
    }

    private fun maxLength(attr: List<BPAnalysisService.Companion.BeatmapAnalysis>): Pair<String, String> {
        val value = attr.getOrNull(0)?.length ?: 0
        return (value / 60).toString() + ":" to (value % 60).toString().padStart(2, '0')
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> readList(body: Map<String, Any?>, key: String): List<T> {
        return when (val value = body[key]) {
            null -> emptyList()
            is List<*> -> value as List<T>
            is Array<*> -> value.toList() as List<T>
            is IntArray -> value.toList() as List<T>
            else -> throw IllegalArgumentException("J2 字段 $key 类型不正确: ${value.javaClass.name}")
        }
    }
}
