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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object PanelV {
    private const val LANE_WIDTH = 10.0
    private const val CHUNK_GAP = 30.0
    private const val MAX_WIDTH = 1920.0
    private const val BAR_PER_BUCKET = 4
    private const val BEATS_PER_CHUNK = 16.0
    private const val MINUTE_INTERVAL = 60000
    private const val ROW_HEIGHT = 710.0
    private const val ROW_GAP = 20.0
    private const val BACKGROUND_BLEED = 5.0
    private const val EPSILON = 0.005
    private const val MINIMUM_BEAT_LENGTH = 1.0

    fun toSvg(param: ViewParam, source: ViewSource, assets: ImageAssets): String {
        val rows = param.row
        val key = source.key
        val red = source.timings.filter { it.isRed }

        val firstTime = source.notes.firstOrNull()?.time ?: 0
        val lastNote = source.notes.lastOrNull()
        val lastTime = lastNote?.endTime ?: lastNote?.time?.toDouble() ?: 0.0
        val lastTimelineTime = source.timings.lastOrNull()?.time ?: 0

        val bpm = ViewSupport.longestBpm(red, lastTime)
        val normalizedBpm = ViewSupport.normalizeBpm(bpm)
        val beatLength = MINUTE_INTERVAL / normalizedBpm

        val chunkWidth = CHUNK_GAP + key * LANE_WIDTH
        val chunkPerRow = floor((MAX_WIDTH - CHUNK_GAP) / chunkWidth).toInt()
        val chunksPerPage = max(1, chunkPerRow * rows)
        val chunkX = (MAX_WIDTH - (chunkPerRow * chunkWidth - CHUNK_GAP)) / 2

        val totalBeats = (lastTime - firstTime) / beatLength
        val beatsPerPage = BEATS_PER_CHUNK * chunksPerPage
        val totalPages = max(1, ceil(totalBeats / beatsPerPage).toInt())
        val page = if (param.page == 0) 1 else param.page
        val validPage = Numbers.clamp(page.toDouble(), totalPages.toDouble(), 1.0).toInt()
        val startBar = (validPage - 1) * chunksPerPage * BAR_PER_BUCKET
        val pageStartBeat = startBar * 4.0

        val chunks = List(chunksPerPage) { index ->
            ViewChunk(index = index, startBar = startBar + index * BAR_PER_BUCKET)
        }

        val longTypes = setOf(ViewNoteType.LONGNOTE)
        ViewTimeline.distributeNotes(
            source.notes, chunks, firstTime, beatLength, pageStartBeat, beatsPerPage, BEATS_PER_CHUNK, EPSILON, longTypes,
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

        ViewTimeline.pushLines(timeline.lines, chunks, pageStartBeat, beatsPerPage, BEATS_PER_CHUNK, EPSILON)
        ViewTimeline.assignInitialSv(chunks, timeline.lines, startBar, BEATS_PER_CHUNK, 0.001, false)
        ViewTimeline.addVirtualRed(chunks, red, source.notes, firstTime, firstTime, beatLength)

        var maxUsedChunkIndex = -1
        for (index in 0 until chunksPerPage) {
            if (chunks[index].notes.isNotEmpty() || chunks[index].timings.isNotEmpty()) {
                maxUsedChunkIndex = index
            }
        }
        val actualRows = if (maxUsedChunkIndex == -1) 1 else maxUsedChunkIndex / chunkPerRow + 1
        val totalHeight = 290.0 + 40 + actualRows * ROW_HEIGHT + (actualRows - 1) * ROW_GAP + 40

        val svMode = param.variation
        val svText = if (svMode) ViewSupport.svSummary(timeline.minSv, timeline.maxSv) else ""
        val requestTime = svText + "request time: " + TimeFormat.nowTimestamp()

        val cardA2 = CardA2.render(CardA2.fromBeatmap(param.beatmap, assets), assets)
        val banner = assets.mapBackground(param.beatmap, "cover")

        val note1 = assets.local("Component/mania-note1.png")
        val note2 = assets.local("Component/mania-note2.png")
        val noteS = assets.local("Component/mania-noteS.png")
        val ln1 = assets.local("Component/mania-note1L.png")
        val ln2 = assets.local("Component/mania-note2L.png")
        val lnS = assets.local("Component/mania-noteSL.png")

        val svg = StringBuilder()
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"1920\" height=\"${Svg.number(totalHeight)}\" viewBox=\"0 0 1920 ${Svg.number(totalHeight)}\">")
        svg.append("<defs>")
        svg.append("<symbol id=\"note1\" viewBox=\"0 0 256 150\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 150.0, note1.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
        svg.append("<symbol id=\"note2\" viewBox=\"0 0 256 150\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 150.0, note2.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
        svg.append("<symbol id=\"notes\" viewBox=\"0 0 256 150\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 150.0, noteS.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
        svg.append("<symbol id=\"ln1\" viewBox=\"0 0 256 64\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 64.0, ln1.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
        svg.append("<symbol id=\"ln2\" viewBox=\"0 0 256 64\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 64.0, ln2.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
        svg.append("<symbol id=\"lns\" viewBox=\"0 0 256 64\" preserveAspectRatio=\"none\">")
        svg.append(Svg.image(0.0, 0.0, 256.0, 64.0, lnS.orEmpty(), 1.0, "none"))
        svg.append("</symbol>")
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
            bPanelName("Beatmap View (!ymv)", if (svMode) "VV" else "V", requestTime),
        )
        svg.append(
            Fonts.torusBold.textPath(
                "page: $validPage of $totalPages", 960.0, totalHeight - 15, 20.0, "center baseline", "#fff", 0.6,
            ),
        )
        svg.append("</g>")

        val renderChunksCount = actualRows * chunkPerRow
        val backgrounds = mutableListOf<String>()
        val components = mutableListOf<String>()
        for (index in 0 until renderChunksCount) {
            val chunk = chunks.getOrNull(index) ?: continue
            val component = ViewComponent.mania(
                chunk, key, ROW_HEIGHT, BEATS_PER_CHUNK, source.specialStyle,
                timeline.maxSv, timeline.minSv, svMode,
            )
            val row = index / chunkPerRow
            val column = index % chunkPerRow
            val x = column * chunkWidth + chunkX
            val y = 290.0 + 40 + row * (ROW_HEIGHT + ROW_GAP)
            if (row % 2 != 0) {
                backgrounds += PanelDraw.rect(0.0, y - BACKGROUND_BLEED, 1920.0, ROW_HEIGHT + BACKGROUND_BLEED * 2, 0.0, "#46393F")
            }
            components += Svg.group(x, y, component)
        }

        svg.append(backgrounds.joinToString("\n"))
        svg.append(components.joinToString("\n"))
        svg.append("</svg>")
        return svg.toString()
    }
}
