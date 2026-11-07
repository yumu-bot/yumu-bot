package com.now.nowbot.image.view

import com.now.nowbot.image.util.Numbers
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ViewLineState(
    val lines: List<ViewLine>,
    val firstTimingPoint: ViewLine?,
    val maxSv: Double,
    val minSv: Double,
)

internal object ViewTimeline {
    fun build(
        timings: List<ViewTiming>,
        previewTime: Int,
        firstTime: Int,
        firstNoteTime: Int,
        lastTime: Double,
        lastTimelineTime: Int,
        beatLength: Double,
        minimumBeatLength: Double,
    ): ViewLineState {
        val lines = mutableListOf<ViewLine>()
        val red = timings.filter { it.isRed }

        for (index in red.indices) {
            val current = red[index]
            val next = red.getOrNull(index + 1)
            val nextTime = next?.time?.toDouble() ?: lastTime
            val timingBeatLength = current.beatLength
            if (timingBeatLength.isNaN() || timingBeatLength < minimumBeatLength) continue

            var time = current.time.toDouble()
            while (time < nextTime) {
                if (abs(time - current.time) < 1) {
                    time += timingBeatLength
                    continue
                }
                if (abs(time - firstNoteTime) <= 500) {
                    time += timingBeatLength
                    continue
                }

                val beatIndex = Numbers.jsRound((time - current.time) / timingBeatLength)
                val isBar = current.meter != 0 && beatIndex % current.meter == 0.0
                val roundedTime = Numbers.jsRound(time).toInt()
                val beat = (roundedTime - firstTime) / beatLength
                val untilNext = nextTime - time
                val isNearNext = next != null && untilNext < timingBeatLength * 0.1

                lines += ViewLine(
                    time = roundedTime,
                    type = if (isBar) "bar" else "beat",
                    beat = beat,
                    beatLength = current.beatLength,
                    sv = current.sv,
                    bpm = current.bpm,
                    measureIndex = if (isBar) beatIndex / current.meter + 1 else null,
                    isNearNext = isNearNext,
                    meter = current.meter,
                )
                time += timingBeatLength
            }
        }

        val latest = max(lastTimelineTime.toDouble(), lastTime)
        var minute = 0.0
        while (minute <= latest) {
            if (minute != 0.0) {
                lines += ViewLine(
                    time = minute.toInt(),
                    type = "minute",
                    beat = (minute - firstTime) / beatLength,
                )
            }
            minute += 60000
        }

        val offset = latest % 60000
        if (offset >= 5000) {
            val lastSecond = kotlin.math.floor(latest / 1000) * 1000
            lines += ViewLine(
                time = lastSecond.toInt(),
                type = "minute_latest",
                beat = (latest - firstTime) / beatLength,
            )
        }

        if (previewTime > 0) {
            lines += ViewLine(
                time = previewTime,
                type = "preview",
                beat = (previewTime - firstTime) / beatLength,
            )
        }

        for (timing in timings) {
            lines += ViewLine(
                time = timing.time,
                type = if (timing.isRed) "red" else "green",
                beat = (timing.time - firstTime) / beatLength,
                beatLength = timing.beatLength,
                sv = timing.sv,
                bpm = timing.bpm,
                meter = timing.meter,
            )
        }

        lines.sortWith(Comparator { first, second ->
            when {
                first.time != second.time -> first.time - second.time
                first.beatLength != null && second.beatLength == null -> -1
                first.beatLength == null && second.beatLength != null -> 1
                else -> 0
            }
        })

        val durationMap = LinkedHashMap<Double, Double>()
        for (index in lines.indices) {
            val current = lines[index]
            val currentBeatLength = current.beatLength ?: continue
            val sv = current.sv ?: 1.0
            val nextTime = if (index < lines.lastIndex) lines[index + 1].time.toDouble() else lastTime
            val duration = nextTime - current.time
            if (duration <= 0) continue

            val speedValue = Numbers.jsRound((currentBeatLength * sv) / 10) * 10
            if (speedValue >= 10) {
                durationMap[speedValue] = (durationMap[speedValue] ?: 0.0) + duration
            }
        }

        var significantSpeed = 1.0
        var maxStandardDuration = -1.0
        for ((speed, duration) in durationMap) {
            if (duration > maxStandardDuration) {
                maxStandardDuration = duration
                significantSpeed = speed
            }
        }

        var maxSv = Double.NEGATIVE_INFINITY
        var minSv = Double.POSITIVE_INFINITY
        for (line in lines) {
            val currentBeatLength = line.beatLength ?: continue
            val sv = line.sv
            if (sv == null) {
                line.standardSv = currentBeatLength / significantSpeed
            } else {
                val standard = currentBeatLength * sv / significantSpeed
                line.standardSv = standard
                if (standard > maxSv) maxSv = standard
                if (standard < minSv) minSv = standard
            }
        }

        val totalBeats = (lastTime - firstTime) / beatLength
        var afterSv: Double? = null
        var afterBeat = totalBeats
        for (index in lines.indices.reversed()) {
            val current = lines[index]
            val standard = current.standardSv ?: continue
            current.nextStandardSv = afterSv ?: standard
            current.nextBeat = afterBeat
            afterSv = standard
            afterBeat = current.beat
        }

        val firstTimingPoint = lines.firstOrNull { it.standardSv != null }
        var beforeSv = firstTimingPoint?.standardSv ?: 1.0
        var beforeBeat = firstTimingPoint?.beat ?: 0.0
        for (line in lines) {
            line.prevStandardSv = beforeSv
            line.prevBeat = beforeBeat
            val standard = line.standardSv
            if (standard != null) beforeSv = standard
            beforeBeat = line.beat
        }

        return ViewLineState(lines, firstTimingPoint, maxSv, minSv)
    }

    fun distributeNotes(
        notes: List<ViewNote>,
        chunks: List<ViewChunk>,
        firstTime: Int,
        beatLength: Double,
        pageStartBeat: Double,
        beatsPerPage: Double,
        beatsPerChunk: Double,
        epsilon: Double,
        longTypes: Set<ViewNoteType>,
    ) {
        for (note in notes) {
            val beat = (note.time - firstTime) / beatLength
            val extends = note.type in longTypes
            val endBeat = if (extends) {
                note.endTime?.let { (it - firstTime) / beatLength } ?: beat
            } else {
                beat
            }

            if (endBeat < pageStartBeat || beat >= pageStartBeat + beatsPerPage) continue

            val startChunk = max(0.0, kotlin.math.floor((beat - pageStartBeat + epsilon) / beatsPerChunk)).toInt()
            var endChunk = if (!extends) {
                startChunk
            } else {
                min(chunks.size - 1.0, kotlin.math.floor((endBeat - pageStartBeat - epsilon) / beatsPerChunk)).toInt()
            }
            endChunk = max(startChunk, endChunk)

            for (chunkIndex in startChunk..endChunk) {
                val chunkStart = pageStartBeat + chunkIndex * beatsPerChunk
                val chunkEnd = chunkStart + beatsPerChunk
                chunks.getOrNull(chunkIndex)?.notes?.add(
                    ViewChunkNote(
                        note = note,
                        beat = beat,
                        endBeat = endBeat,
                        renderStart = max(beat, chunkStart),
                        renderEnd = min(endBeat, chunkEnd),
                    ),
                )
            }
        }
    }

    fun pushLines(
        lines: List<ViewLine>,
        chunks: List<ViewChunk>,
        pageStartBeat: Double,
        beatsPerPage: Double,
        beatsPerChunk: Double,
        epsilon: Double,
    ) {
        for (line in lines) {
            if (line.beat < pageStartBeat || line.beat >= pageStartBeat + beatsPerPage) continue
            val chunkIndex = kotlin.math.floor((line.beat - pageStartBeat + epsilon) / beatsPerChunk).toInt()
            chunks.getOrNull(chunkIndex)?.timings?.add(line)
        }
    }

    fun assignInitialSv(
        chunks: List<ViewChunk>,
        lines: List<ViewLine>,
        startBar: Int,
        beatsPerChunk: Double,
        epsilon: Double,
        requireStandard: Boolean,
    ) {
        val firstTimingPoint = lines.firstOrNull { it.standardSv != null }
        chunks.forEachIndexed { index, chunk ->
            val chunkStartBeat = startBar * 4 + index * beatsPerChunk
            var lastPointBefore: ViewLine? = null
            for (lineIndex in lines.indices.reversed()) {
                val line = lines[lineIndex]
                if (requireStandard && line.standardSv == null) continue
                if (line.beat <= chunkStartBeat + epsilon) {
                    lastPointBefore = line
                    break
                }
            }
            chunk.initialSv = if (lastPointBefore != null) {
                lastPointBefore.standardSv
            } else {
                firstTimingPoint?.standardSv ?: 1.0
            }
        }
    }

    fun addVirtualRed(
        chunks: List<ViewChunk>,
        red: List<ViewTiming>,
        notes: List<ViewNote>,
        firstTime: Int,
        firstNoteTime: Int,
        beatLength: Double,
    ) {
        val firstChunk = chunks.firstOrNull() ?: return
        if (firstChunk.notes.isEmpty()) return
        val hasRedLine = red.any { abs(it.time - firstNoteTime) <= beatLength }
        if (hasRedLine) return

        val firstNote = notes.firstOrNull() ?: return
        val firstRed = red.firstOrNull()
        val virtual = ViewLine(
            time = firstNote.time,
            type = "virtual",
            beat = ((firstRed?.time ?: firstTime) - firstTime) / beatLength,
            bpm = firstRed?.bpm,
        )
        firstChunk.timings.add(0, virtual)
        firstChunk.timings.removeAll { line ->
            line !== virtual && (line.type == "beat" || line.type == "bar") &&
                abs(line.beat - virtual.beat) < 0.5
        }
    }
}
