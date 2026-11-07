package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.image.view.ViewChunk
import com.now.nowbot.image.view.ViewComponent
import com.now.nowbot.image.view.ViewNoteType
import com.now.nowbot.image.view.ViewSource
import com.now.nowbot.image.view.ViewSupport
import com.now.nowbot.image.view.ViewTimeline
import com.now.nowbot.service.messageServiceImpl.ViewService.ViewParam
import kotlin.math.ceil
import kotlin.math.max

object PanelV2 {
    private const val LANE_HEIGHT = 50.0
    private const val LANE_GAP = 30.0
    private const val LANE_PER_PAGE = 60
    private const val BAR_PER_LANE = 4
    private const val BEATS_PER_LANE = 16.0
    private const val MINUTE_INTERVAL = 60000
    private const val PADDING_WIDTH = 40.0
    private const val MAX_WIDTH = 1920.0 - 2 * PADDING_WIDTH
    private const val EPSILON = 0.005
    private const val MINIMUM_BEAT_LENGTH = 2.0

    fun toSvg(param: ViewParam, source: ViewSource, assets: ImageAssets): String {
        val red = source.timings.filter { it.isRed }
        val firstTime = source.notes.firstOrNull()?.time ?: 0
        val lastNote = source.notes.lastOrNull()
        val lastTime = lastNote?.endTime ?: lastNote?.time?.toDouble() ?: 0.0
        val lastTimelineTime = source.timings.lastOrNull()?.time ?: 0

        val bpm = ViewSupport.longestBpm(red, lastTime)
        val normalizedBpm = ViewSupport.normalizeBpm(bpm)
        val beatLength = MINUTE_INTERVAL / normalizedBpm

        val totalBeats = (lastTime - firstTime) / beatLength
        val beatsPerPage = BEATS_PER_LANE * LANE_PER_PAGE
        val totalPages = max(1, ceil(totalBeats / beatsPerPage).toInt())
        val page = if (param.page == 0) 1 else param.page
        val validPage = Numbers.clamp(page.toDouble(), totalPages.toDouble(), 1.0).toInt()
        val startBar = (validPage - 1) * LANE_PER_PAGE * BAR_PER_LANE
        val pageStartBeat = startBar * 4.0

        val chunks = List(LANE_PER_PAGE) { index ->
            ViewChunk(index = index, startBar = startBar + index * BAR_PER_LANE)
        }

        ViewTimeline.distributeNotes(
            source.notes, chunks, firstTime, beatLength, pageStartBeat, beatsPerPage, BEATS_PER_LANE, EPSILON,
            setOf(ViewNoteType.SLIDER, ViewNoteType.SPINNER),
        )

        val timeline = ViewTimeline.build(
            timings = source.timings,
            previewTime = source.previewTime,
            firstTime = firstTime,
            firstNoteTime = firstTime,
            lastTime = lastTime,
            lastTimelineTime = lastTimelineTime,
            beatLength = beatLength,
            minimumBeatLength = MINIMUM_BEAT_LENGTH,
        )

        ViewTimeline.pushLines(timeline.lines, chunks, pageStartBeat, beatsPerPage, BEATS_PER_LANE, EPSILON)
        ViewTimeline.assignInitialSv(chunks, timeline.lines, startBar, BEATS_PER_LANE, 1e-4, true)
        ViewTimeline.addVirtualRed(chunks, red, source.notes, firstTime, firstTime, beatLength)

        var maxUsedLaneIndex = -1
        for (index in 0 until LANE_PER_PAGE) {
            if (chunks[index].notes.isNotEmpty() || chunks[index].timings.isNotEmpty()) {
                maxUsedLaneIndex = index
            }
        }
        val actualLaneCount = max(1, maxUsedLaneIndex + 1)
        val totalHeight = 290.0 + 40 + actualLaneCount * LANE_HEIGHT + (actualLaneCount - 1) * LANE_GAP + 40

        val svMode = param.variation
        val svText = if (svMode) ViewSupport.svSummary(timeline.minSv, timeline.maxSv) else ""
        val requestTime = svText + "request time: " + TimeFormat.nowTimestamp()

        val cardA2 = CardA2.render(CardA2.fromBeatmap(param.beatmap, assets), assets)
        val banner = assets.mapBackground(param.beatmap, "cover")
        val hitOverlay = assets.local("Component/taikohitcircleoverlay.png")
        val bigOverlay = assets.local("Component/taikobigcircleoverlay.png")

        val svg = StringBuilder()
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"1920\" height=\"${Svg.number(totalHeight)}\" viewBox=\"0 0 1920 ${Svg.number(totalHeight)}\">")
        svg.append("<defs>")
        svg.append(taikoFilter("don", "#D32F2F", hitOverlay))
        svg.append(taikoFilter("ka", "#4FACFE", hitOverlay))
        svg.append(taikoFilter("don2", "#D32F2F", bigOverlay))
        svg.append(taikoFilter("ka2", "#4FACFE", bigOverlay))
        svg.append(symbol("note", 128.0, 128.0, assets.local("Component/taikohitcircle.png")))
        svg.append(symbol("big", 128.0, 128.0, assets.local("Component/taikobigcircle.png")))
        svg.append(symbol("roll-end", 68.0, 124.0, assets.local("Component/taiko-roll-end.png")))
        svg.append(symbol("roll-middle", 1.0, 124.0, assets.local("Component/taiko-roll-middle.png")))
        svg.append(symbol("spinner-approach", 390.0, 390.0, assets.local("Component/spinner-approachcircle.png")))
        svg.append(symbol("spinner-circle", 32.0, 32.0, assets.local("Component/spinner-circle.png")))
        svg.append("<clipPath id=\"banner\">")
        svg.append(PanelDraw.rect(0.0, 0.0, 1920.0, 290.0, 40.0, "none"))
        svg.append("</clipPath></defs><g>")
        svg.append(PanelDraw.rect(0.0, 0.0, 1920.0, totalHeight, 40.0, "#2A2226"))
        svg.append(PanelDraw.rect(0.0, 290.0, 1920.0, totalHeight - 290, 40.0, "#382E32"))
        svg.append("<g clip-path=\"url(#banner)\">")
        svg.append(banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }.orEmpty())
        svg.append("</g>")
        svg.append(PanelDraw.rect(510.0, 40.0, 195.0, 60.0, 15.0, "#382E32"))
        svg.append(Svg.group(40.0, 40.0, cardA2))
        svg.append(
            bPanelName("Beatmap View (!ymv) ~ Not even close to donscore", if (svMode) "VV" else "V", requestTime),
        )
        svg.append(
            Fonts.torusBold.textPath(
                "page: $validPage of $totalPages", 960.0, totalHeight - 15, 20.0, "center baseline", "#fff", 0.6,
            ),
        )
        svg.append("</g>")

        val backgrounds = mutableListOf<String>()
        val components = mutableListOf<String>()
        for (index in 0 until actualLaneCount) {
            val chunk = chunks.getOrNull(index) ?: continue
            val component = ViewComponent.taiko(
                chunk, LANE_HEIGHT, MAX_WIDTH, BEATS_PER_LANE, timeline.maxSv, timeline.minSv, svMode,
            )
            val x = PADDING_WIDTH
            val y = 290.0 + 40 + index * (LANE_HEIGHT + LANE_GAP) - 10
            if (index % 2 != 0) {
                backgrounds += PanelDraw.rect(x, y, MAX_WIDTH, LANE_HEIGHT, 0.0, "#46393F")
            }
            components += Svg.group(x, y, component)
        }

        svg.append(backgrounds.joinToString("\n"))
        svg.append(components.joinToString("\n"))
        svg.append("</svg>")
        return svg.toString()
    }

    private fun taikoFilter(id: String, color: String, overlay: String?): String {
        return "<filter id=\"$id\" x=\"0\" y=\"0\" width=\"100%\" height=\"100%\">" +
            "<feFlood flood-color=\"$color\" result=\"targetColor\" />" +
            "<feComposite in=\"SourceGraphic\" in2=\"targetColor\" operator=\"arithmetic\" k1=\"1\" k2=\"0\" k3=\"0\" k4=\"0\" result=\"multipliedBase\" />" +
            "<feImage href=\"${overlay.orEmpty()}\" result=\"overlayImage\" preserveAspectRatio=\"xMidYMid slice\" />" +
            "<feMerge><feMergeNode in=\"multipliedBase\" /><feMergeNode in=\"overlayImage\" /></feMerge></filter>"
    }

    private fun symbol(id: String, width: Double, height: Double, url: String?): String {
        return "<symbol id=\"$id\" viewBox=\"0 0 ${Svg.number(width)} ${Svg.number(height)}\" preserveAspectRatio=\"none\">" +
            Svg.image(0.0, 0.0, width, height, url.orEmpty(), 1.0, "none") +
            "</symbol>"
    }
}
