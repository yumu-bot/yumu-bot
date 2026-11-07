package com.now.nowbot.image.view

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ViewComponent {
    fun mania(
        chunk: ViewChunk,
        totalKey: Int,
        maxHeight: Double = 710.0,
        beatsPerBucket: Double = 16.0,
        isSpecial: Boolean = false,
        maxSv: Double,
        minSv: Double,
        svMode: Boolean,
    ): String {
        val overlay = ViewSupport.keyOverlay(totalKey, isSpecial)
        val svg = StringBuilder()

        val lineWidth = 10.0
        val noteHeight = 5.0
        val totalWidth = totalKey * lineWidth

        svg.append("<g class=\"timing-${chunk.index}\">")
        svg.append("<line x1=\"0\" y1=\"0\" x2=\"0\" y2=\"${Svg.number(maxHeight)}\" stroke=\"#555\" stroke-width=\"1\" />")
        svg.append("<line x1=\"${Svg.number(totalWidth)}\" y1=\"0\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(maxHeight)}\" stroke=\"#555\" stroke-width=\"1\" />")

        val reds = StringBuilder()
        val greens = StringBuilder()
        val yellows = StringBuilder()
        val others = StringBuilder()
        val svPoints = mutableListOf<Pair<Double, Double>>()

        var greenCount = 0
        var beforeBpm: Double? = -1.0
        var beforeSv: Double = -1.0

        for (timing in chunk.timings) {
            val relative = timing.beat - chunk.startBar * 4
            val y = maxHeight - ((relative / beatsPerBucket) * maxHeight)

            when (timing.type) {
                "red", "virtual" -> {
                    val bpm = timing.bpm
                    val previousBpm = beforeBpm
                    if (bpm != null && previousBpm != null && abs(bpm - previousBpm) < 1e-4) {
                        reds.append(Fonts.torusBold.textPath("=", -2.0, y + 3, 14.0, "right baseline", "#F990AB"))
                    } else if (bpm != null && bpm < 1000) {
                        val parts = Numbers.roundParts(bpm, 2)
                        val hasDot = parts.integer.contains(".")
                        reds.append(
                            Fonts.torusBold.textPath(
                                parts.integer.replace(".", ""), -2.0, y + 3, 14.0, "right baseline", "#F990AB",
                            ),
                        )
                        reds.append(
                            Fonts.torusBold.textPath(
                                (if (hasDot) "." else "") + parts.decimal, -2.0, y + 3 + 10, 12.0, "right baseline", "#F990AB",
                            ),
                        )
                    } else {
                        reds.append(Fonts.torusBold.textPath("+", -2.0, y + 3, 14.0, "right baseline", "#F990AB"))
                    }
                    reds.append(
                        "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#D32F2F\" stroke-width=\"2\" />",
                    )
                    beforeBpm = bpm
                }

                "bar" -> {
                    if (!timing.isNearNext) {
                        timing.measureIndex?.let {
                            others.append(Fonts.torusBold.textPath(Svg.number(it), -2.0, y + 3, 14.0, "right baseline", "#fff", 0.6))
                        }
                    }
                    others.append(
                        "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#ccc\" stroke-width=\"1.5\" />",
                    )
                }

                "beat" -> others.append(
                    "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#666\" stroke-width=\"1\" />",
                )

                "preview" -> {
                    yellows.append(
                        Fonts.torusBold.textPath("PV", totalWidth + 2, y + 3, 14.0, "left baseline", "#F6F05C"),
                    )
                    yellows.append(
                        "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#F6F05C\" stroke-width=\"2\"/>",
                    )
                }

                "minute" -> {
                    val minute = timing.time / 60000
                    others.append(
                        Fonts.torusBold.get2SizeTextPath(
                            "$minute:", "00", 14.0, 10.0, totalWidth + 2, y + 3, "left baseline", "#00F2FE",
                        ),
                    )
                    others.append(
                        "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#00F2FE\" stroke-width=\"1.5\" stroke-dasharray=\"2,2\" />",
                    )
                }

                "minute_latest" -> {
                    val total = timing.time / 1000
                    val minute = total / 60
                    val second = total % 60
                    others.append(
                        Fonts.torusBold.get2SizeTextPath(
                            "$minute:", second.toString().padStart(2, '0'), 14.0, 10.0,
                            totalWidth + 2, y + 3, "left baseline", "#00F2FE",
                        ),
                    )
                    others.append(
                        "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#00F2FE\" stroke-width=\"1.5\" stroke-dasharray=\"2,2\" />",
                    )
                }

                "green" -> {
                    if (svMode) {
                        val currentSv = timing.standardSv ?: 1.0
                        val center = totalWidth / 2
                        val svX = ViewSupport.svX(currentSv, center, maxSv, minSv)
                        val relativeStart = timing.beat - chunk.startBar * 4
                        val relativeEnd = timing.nextBeat - chunk.startBar * 4
                        val yStart = maxHeight - ((relativeStart / beatsPerBucket) * maxHeight)
                        val yEnd = maxHeight - ((relativeEnd / beatsPerBucket) * maxHeight)
                        val drawBottom = min(maxHeight, max(0.0, yStart))
                        val drawTop = min(maxHeight, max(0.0, yEnd))

                        if (greenCount == 0 && relativeStart > 0 && timing.prevStandardSv != null) {
                            val prevSv = timing.prevStandardSv ?: 1.0
                            val prevX = ViewSupport.svX(prevSv, center, maxSv, minSv)
                            svPoints += prevX to maxHeight
                            svPoints += prevX to drawBottom
                        }
                        if (drawBottom != drawTop) {
                            svPoints += svX to drawBottom
                            svPoints += svX to drawTop
                        }
                    } else {
                        val sv = timing.sv ?: 0.0
                        if (abs(sv - beforeSv) < 1e-4) {
                            greens.append(Fonts.torusBold.textPath("=", totalWidth + 2, y + 3, 14.0, "left baseline", "#CAF881"))
                        } else {
                            greens.append(
                                Fonts.torusBold.textPath(
                                    ViewSupport.formatSv(sv), totalWidth + 2, y + 3, 14.0, "left baseline", "#CAF881",
                                ),
                            )
                        }
                        greens.append(
                            "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(totalWidth)}\" y2=\"${Svg.number(y)}\" opacity=\"0.8\" stroke=\"#CAF881\" stroke-width=\"1\" stroke-dasharray=\"2,2\" />",
                        )
                        beforeSv = sv
                    }
                    greenCount++
                }
            }
        }

        if (svPoints.isEmpty() && chunk.notes.isNotEmpty() && svMode) {
            val sv = chunk.initialSv ?: 1.0
            val center = totalWidth / 2
            val x = ViewSupport.svX(sv, center, maxSv, minSv)
            val opacity = if (abs(sv - 1.0) < 1e-4) 0.2 else 0.5
            greens.append(
                "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(maxHeight)}\" stroke=\"#CAF881\" stroke-width=\"3\" opacity=\"${Svg.number(opacity)}\" />",
            )
        } else {
            val points = svPoints.joinToString(" ") { "${Svg.number(it.first)},${Svg.number(it.second)}" }
            others.append(
                "<polyline points=\"$points\" fill=\"none\" stroke=\"#CAF881\" stroke-width=\"3\" opacity=\"0.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
            )
        }

        svg.append(others).append(greens).append(reds).append(yellows)
        svg.append("</g><g class=\"note-${chunk.index}\">")

        for (chunkNote in chunk.notes) {
            val note = chunkNote.note
            if (note.type == ViewNoteType.CIRCLE) {
                val relative = chunkNote.renderStart - chunk.startBar * 4
                val y = maxHeight - ((relative / beatsPerBucket) * maxHeight)
                val key = ViewSupport.key(note.x, totalKey)
                val x = key * lineWidth
                svg.append(
                    "<use href=\"#note${overlayValue(overlay, key)}\" x=\"${Svg.number(x)}\" y=\"${Svg.number(y - noteHeight / 2)}\" width=\"${Svg.number(lineWidth)}\" height=\"${Svg.number(noteHeight)}\" />",
                )
            } else if (note.type == ViewNoteType.LONGNOTE) {
                val relativeStart = chunkNote.renderStart - chunk.startBar * 4
                val relativeEnd = chunkNote.renderEnd - chunk.startBar * 4
                val startY = maxHeight - ((relativeStart / beatsPerBucket) * maxHeight)
                val endY = maxHeight - ((relativeEnd / beatsPerBucket) * maxHeight)
                val key = ViewSupport.key(note.x, totalKey)
                val x = key * lineWidth
                val height = min(maxHeight, abs(startY - endY))

                svg.append(
                    "<use href=\"#ln${overlayValue(overlay, key)}\" x=\"${Svg.number(x)}\" y=\"${Svg.number(endY)}\" width=\"${Svg.number(lineWidth)}\" height=\"${Svg.number(height)}\" opacity=\"0.6\"/>",
                )
                if (abs(chunkNote.renderStart - chunkNote.beat) < 1e-4 && startY >= 0 && startY <= maxHeight) {
                    svg.append(
                        "<use href=\"#note${overlayValue(overlay, key)}\" x=\"${Svg.number(x)}\" y=\"${Svg.number(startY - noteHeight / 2)}\" width=\"${Svg.number(lineWidth)}\" height=\"${Svg.number(noteHeight)}\" />",
                    )
                }
            }
        }

        svg.append("</g>")
        return svg.toString()
    }

    fun taiko(
        chunk: ViewChunk,
        laneHeight: Double = 50.0,
        laneWidth: Double = 1840.0,
        beatsPerLane: Double = 16.0,
        maxSv: Double,
        minSv: Double,
        svMode: Boolean,
    ): String {
        val svg = StringBuilder()

        val small = Numbers.jsRound(laneHeight * 2 / 3)
        val medium = Numbers.jsRound(laneHeight * 3 / 4)
        val big = Numbers.jsRound(laneHeight * 7 / 8)

        val smallHeight = small
        val smallWidth = small

        val bigHeight = big
        val bigWidth = big

        val drumrollSmallHeight = small
        val drumrollSmallWidth = small / 2

        val drumrollBigHeight = medium
        val drumrollBigWidth = medium / 2

        val spinnerApproachSize = Numbers.jsRound(laneHeight * 8 / 10)
        val spinnerCircleSize = Numbers.jsRound(laneHeight / 5)

        val textUnderDelta = 13.0

        svg.append("<g class=\"timing-${chunk.index}\">")
        svg.append("<line x1=\"0\" y1=\"0.5\" x2=\"${Svg.number(laneWidth)}\" y2=\"${Svg.number(laneHeight - 0.5)}\" fill=\"#46393F\" fill-opacity=\"1\" />")
        svg.append("<line x1=\"0\" y1=\"0\" x2=\"${Svg.number(laneWidth)}\" y2=\"0\" stroke=\"#555\" stroke-width=\"1\" />")
        svg.append("<line x1=\"0\" y1=\"${Svg.number(laneHeight)}\" x2=\"${Svg.number(laneWidth)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#555\" stroke-width=\"1\" />")

        val reds = StringBuilder()
        val greens = StringBuilder()
        val yellows = StringBuilder()
        val others = StringBuilder()
        val svPoints = mutableListOf<Pair<Double, Double>>()

        var greenCount = 0
        var beforeBpm: Double? = -1.0
        var beforeSv: Double = -1.0

        for (timing in chunk.timings) {
            val relative = timing.beat - chunk.startBar * 4
            val x = (relative / beatsPerLane) * laneWidth

            when (timing.type) {
                "red", "virtual" -> {
                    val bpm = timing.bpm
                    val previousBpm = beforeBpm
                    if (bpm != null && previousBpm != null && abs(bpm - previousBpm) < 1e-4) {
                        reds.append(Fonts.torusBold.textPath("=", x, -4.0, 16.0, "center baseline", "#F990AB"))
                    } else if (bpm != null && bpm < 1000) {
                        val parts = Numbers.roundParts(bpm, 2)
                        reds.append(
                            Fonts.torusBold.get2SizeTextPath(
                                parts.integer, parts.decimal, 16.0, 12.0, x, -4.0, "center baseline", "#F990AB",
                            ),
                        )
                    } else {
                        reds.append(Fonts.torusBold.textPath("+", x, -4.0, 16.0, "center baseline", "#F990AB"))
                    }
                    reds.append(
                        "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#D32F2F\" stroke-width=\"2\" />",
                    )
                    beforeBpm = bpm
                }

                "bar" -> {
                    if (!timing.isNearNext) {
                        timing.measureIndex?.let {
                            others.append(Fonts.torusBold.textPath(Svg.number(it), x, -4.0, 14.0, "center baseline", "#fff", 0.6))
                        }
                    }
                    others.append(
                        "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#ccc\" stroke-width=\"1.5\" />",
                    )
                }

                "beat" -> others.append(
                    "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#666\" stroke-width=\"1\" />",
                )

                "preview" -> {
                    yellows.append(
                        Fonts.torusBold.textPath("PV", x, laneHeight + textUnderDelta, 14.0, "center baseline", "#F6F05C"),
                    )
                    yellows.append(
                        "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#F6F05C\" stroke-width=\"2\"/>",
                    )
                }

                "minute" -> {
                    val minute = timing.time / 60000
                    others.append(
                        Fonts.torusBold.get2SizeTextPath(
                            "$minute:", "00", 14.0, 10.0, x, laneHeight + textUnderDelta,
                            "center baseline", "#00F2FE",
                        ),
                    )
                    others.append(
                        "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#00F2FE\" stroke-width=\"1.5\" stroke-dasharray=\"2,2\" />",
                    )
                }

                "minute_latest" -> {
                    val total = timing.time / 1000
                    val minute = total / 60
                    val second = total % 60
                    others.append(
                        Fonts.torusBold.get2SizeTextPath(
                            "$minute:", second.toString().padStart(2, '0'), 14.0, 10.0, x,
                            laneHeight + textUnderDelta, "center baseline", "#00F2FE",
                        ),
                    )
                    others.append(
                        "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" stroke=\"#00F2FE\" stroke-width=\"1.5\" stroke-dasharray=\"2,2\" />",
                    )
                }

                "green" -> {
                    if (svMode) {
                        val currentSv = timing.standardSv ?: 1.0
                        val center = laneHeight / 2
                        val svY = ViewSupport.svY(currentSv, center, maxSv, minSv)
                        val relativeStart = timing.beat - chunk.startBar * 4
                        val relativeEnd = timing.nextBeat - chunk.startBar * 4
                        val xStart = (relativeStart / beatsPerLane) * laneWidth
                        val xEnd = (relativeEnd / beatsPerLane) * laneWidth
                        val drawLeft = min(laneWidth, max(0.0, xStart))
                        val drawRight = min(laneWidth, max(0.0, xEnd))

                        if (greenCount == 0 && relativeStart > 0 && timing.prevStandardSv != null) {
                            val prevSv = timing.prevStandardSv ?: 1.0
                            val prevY = ViewSupport.svY(prevSv, center, maxSv, minSv)
                            svPoints += 0.0 to prevY
                            svPoints += drawLeft to prevY
                        }
                        if (drawLeft != drawRight) {
                            svPoints += drawLeft to svY
                            svPoints += drawRight to svY
                        }
                    } else {
                        val sv = timing.sv ?: 0.0
                        if (abs(sv - beforeSv) < 1e-4) {
                            greens.append(Fonts.torusBold.textPath("=", x, laneHeight + textUnderDelta, 14.0, "center baseline", "#CAF881"))
                        } else {
                            greens.append(
                                Fonts.torusBold.textPath(
                                    ViewSupport.formatSv(sv), x, laneHeight + textUnderDelta, 14.0, "center baseline", "#CAF881",
                                ),
                            )
                        }
                        greens.append(
                            "<line x1=\"${Svg.number(x)}\" y1=\"0\" x2=\"${Svg.number(x)}\" y2=\"${Svg.number(laneHeight)}\" opacity=\"0.8\" stroke=\"#CAF881\" stroke-width=\"1\" stroke-dasharray=\"2,2\" />",
                        )
                        beforeSv = sv
                    }
                    greenCount++
                }
            }
        }

        if (svPoints.isEmpty() && chunk.notes.isNotEmpty() && svMode) {
            val sv = chunk.initialSv ?: 1.0
            val center = laneHeight / 2
            val y = ViewSupport.svY(sv, center, maxSv, minSv)
            val opacity = if (abs(sv - 1.0) < 1e-4) 0.2 else 0.5
            greens.append(
                "<line x1=\"0\" y1=\"${Svg.number(y)}\" x2=\"${Svg.number(laneWidth)}\" y2=\"${Svg.number(y)}\" stroke=\"#CAF881\" stroke-width=\"3\" opacity=\"${Svg.number(opacity)}\" />",
            )
        } else {
            val points = svPoints.joinToString(" ") { "${Svg.number(it.first)},${Svg.number(it.second)}" }
            others.append(
                "<polyline points=\"$points\" fill=\"none\" stroke=\"#CAF881\" stroke-width=\"3\" opacity=\"0.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
            )
        }

        svg.append(others).append(greens).append(reds).append(yellows)
        svg.append("</g><g class=\"note-${chunk.index}\">")

        val noteArray = mutableListOf<NoteBody>()
        for (chunkNote in chunk.notes) {
            val note = chunkNote.note
            var noteBody: String? = null

            when (note.type) {
                ViewNoteType.CIRCLE -> {
                    val relative = chunkNote.renderStart - chunk.startBar * 4
                    val x = (relative / beatsPerLane) * laneWidth
                    val mask = note.soundMask
                    val ka = (mask and 2) != 0 || (mask and 8) != 0
                    val bigNote = (mask and 4) != 0
                    val width = if (bigNote) bigWidth else smallWidth
                    val height = if (bigNote) bigHeight else smallHeight
                    val y = (laneHeight - height) / 2
                    val href = if (bigNote) "big" else "note"
                    val filter = if (ka) {
                        if (bigNote) "ka2" else "ka"
                    } else {
                        if (bigNote) "don2" else "don"
                    }
                    noteBody = "<use href=\"#$href\" filter=\"url(#$filter)\" x=\"${Svg.number(x - width / 2)}\" y=\"${Svg.number(y)}\" width=\"${Svg.number(width)}\" height=\"${Svg.number(height)}\" />"
                }

                ViewNoteType.SLIDER -> {
                    val relativeStart = chunkNote.renderStart - chunk.startBar * 4
                    val relativeEnd = chunkNote.renderEnd - chunk.startBar * 4
                    val startX = Numbers.jsRound((relativeStart / beatsPerLane) * laneWidth)
                    val endX = Numbers.jsRound((relativeEnd / beatsPerLane) * laneWidth)
                    val bigNote = (note.soundMask and 4) != 0
                    val drumrollHeight = if (bigNote) drumrollBigHeight else drumrollSmallHeight
                    val drumrollWidth = if (bigNote) drumrollBigWidth else drumrollSmallWidth
                    val deltaX = abs(startX - endX)
                    val rollWidth = max(min(laneWidth, deltaX), 0.0)
                    if (rollWidth < 3) continue

                    val y = (laneHeight - drumrollHeight) / 2
                    val body = StringBuilder()
                    body.append(
                        "<use href=\"#roll-middle\" x=\"${Svg.number(startX)}\" y=\"${Svg.number(y)}\" width=\"${Svg.number(rollWidth)}\" height=\"${Svg.number(drumrollHeight)}\" opacity=\"0.6\"/>",
                    )
                    if (abs(chunkNote.renderEnd - chunkNote.endBeat) < 1 && endX >= 0 && endX <= laneWidth) {
                        body.append(
                            "<use href=\"#roll-end\" shape-rendering=\"crispEdges\" x=\"0\" y=\"0\" width=\"${Svg.number(drumrollWidth)}\" height=\"${Svg.number(drumrollHeight)}\" transform=\"translate(${Svg.number(endX)}, ${Svg.number(y)}) scale(-1, 1)\" />",
                        )
                        body.append(
                            "<use href=\"#roll-end\" shape-rendering=\"crispEdges\" x=\"${Svg.number(endX)}\" y=\"${Svg.number(y)}\" width=\"${Svg.number(drumrollWidth)}\" height=\"${Svg.number(drumrollHeight)}\"/>",
                        )
                    }
                    if (abs(chunkNote.renderStart - chunkNote.beat) < 1e-4 && startX >= 0 && startX <= laneWidth) {
                        body.append(
                            "<use href=\"#roll-end\" shape-rendering=\"crispEdges\" x=\"0\" y=\"0\" width=\"${Svg.number(drumrollWidth)}\" height=\"${Svg.number(drumrollHeight)}\" transform=\"translate(${Svg.number(startX)}, ${Svg.number(y)}) scale(-1, 1)\" />",
                        )
                        body.append(
                            "<use href=\"#roll-end\" shape-rendering=\"crispEdges\" x=\"${Svg.number(startX)}\" y=\"${Svg.number(y)}\" width=\"${Svg.number(drumrollWidth)}\" height=\"${Svg.number(drumrollHeight)}\"/>",
                        )
                    }
                    noteBody = body.toString()
                }

                ViewNoteType.SPINNER -> {
                    val relative = chunkNote.renderStart - chunk.startBar * 4
                    val x = (relative / beatsPerLane) * laneWidth
                    noteBody = "<use href=\"#spinner-approach\" x=\"${Svg.number(x - spinnerApproachSize / 2)}\" y=\"${Svg.number((laneHeight - spinnerApproachSize) / 2)}\" width=\"${Svg.number(spinnerApproachSize)}\" height=\"${Svg.number(spinnerApproachSize)}\"/>" +
                        "<use href=\"#spinner-circle\" x=\"${Svg.number(x - spinnerCircleSize / 2)}\" y=\"${Svg.number((laneHeight - spinnerCircleSize) / 2)}\" width=\"${Svg.number(spinnerCircleSize)}\" height=\"${Svg.number(spinnerCircleSize)}\"/>"
                }

                ViewNoteType.LONGNOTE -> Unit
            }

            val finalBody = noteBody
            if (finalBody != null) {
                noteArray += NoteBody(finalBody, note.type.name.lowercase(), note.time)
            }
        }

        noteArray.sortWith(Comparator { first, second ->
            val firstPriority = NOTE_PRIORITY[first.type] ?: 0
            val secondPriority = NOTE_PRIORITY[second.type] ?: 0
            if (firstPriority != secondPriority) {
                firstPriority - secondPriority
            } else {
                second.time - first.time
            }
        })
        noteArray.forEach { svg.append(it.body) }

        svg.append("</g>")
        return svg.toString()
    }

    private fun overlayValue(overlay: List<String>, key: Int): String = overlay.getOrNull(key).orEmpty()

    private data class NoteBody(val body: String, val type: String, val time: Int)

    private val NOTE_PRIORITY = mapOf("slider" to 1, "spinner" to 2, "circle" to 3)
}
