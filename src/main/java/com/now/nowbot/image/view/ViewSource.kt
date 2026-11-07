package com.now.nowbot.image.view

import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectSound
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType
import com.now.nowbot.model.beatmapParse.parse.OsuBeatmapAttributes
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.image.util.Numbers
import kotlin.math.max

enum class ViewNoteType {
    CIRCLE, SLIDER, SPINNER, LONGNOTE
}

data class ViewNote(
    val x: Int,
    val time: Int,
    val endTime: Double?,
    val type: ViewNoteType,
    val soundMask: Int,
)

data class ViewTiming(
    val time: Int,
    val beatLength: Double,
    val sv: Double?,
    val bpm: Double?,
    val meter: Int,
    val isRed: Boolean,
)

data class ViewLine(
    var time: Int,
    var type: String,
    var beat: Double = 0.0,
    var beatLength: Double? = null,
    var sv: Double? = null,
    var bpm: Double? = null,
    var measureIndex: Double? = null,
    var isNearNext: Boolean = false,
    var meter: Int = 0,
    var standardSv: Double? = null,
    var nextStandardSv: Double? = null,
    var nextBeat: Double = 0.0,
    var prevStandardSv: Double? = null,
    var prevBeat: Double = 0.0,
)

data class ViewChunk(
    val index: Int,
    val startBar: Int,
    val notes: MutableList<ViewChunkNote> = mutableListOf(),
    val timings: MutableList<ViewLine> = mutableListOf(),
    var initialSv: Double? = 1.0,
)

data class ViewChunkNote(
    val note: ViewNote,
    val beat: Double,
    val endBeat: Double,
    val renderStart: Double,
    val renderEnd: Double,
)

data class ViewSource(
    val key: Int,
    val notes: List<ViewNote>,
    val timings: List<ViewTiming>,
    val previewTime: Int,
    val specialStyle: Boolean,
) {
    companion object {
        fun fromText(text: String, mode: OsuMode): ViewSource {
            val file = OsuFile(text)
            val fileMode = file.mode
            val attributes: OsuBeatmapAttributes = if (fileMode == mode || fileMode == OsuMode.OSU) {
                when (mode) {
                    OsuMode.TAIKO -> file.getTaiko()
                    OsuMode.MANIA -> file.getMania()
                    else -> file.getOsu()
                }
            } else {
                file.getOsu()
            }
            val extras = scanExtras(text)
            val timings = normalizeTimings(attributes)
            val notes = attributes.hitObjects.mapIndexedNotNull { index, hit ->
                val type = when (hit.type) {
                    HitObjectType.CIRCLE -> ViewNoteType.CIRCLE
                    HitObjectType.SLIDER -> ViewNoteType.SLIDER
                    HitObjectType.SPINNER -> ViewNoteType.SPINNER
                    HitObjectType.LONGNOTE -> ViewNoteType.LONGNOTE
                    else -> return@mapIndexedNotNull null
                }
                val raw = extras.hitObjects.getOrNull(index)
                val endTime = when (type) {
                    ViewNoteType.CIRCLE -> null
                    ViewNoteType.SLIDER -> sliderEnd(hit.startTime, raw, attributes, timings) ?: hit.endTime.toDouble()
                    else -> hit.endTime.toDouble()
                }
                ViewNote(
                    x = hit.position.x,
                    time = hit.startTime,
                    endTime = endTime,
                    type = type,
                    soundMask = raw?.soundMask ?: soundMask(hit.sound),
                )
            }.sortedWith(NOTE_ORDER)
            return ViewSource(
                key = Numbers.jsRound(attributes.cs).toInt(),
                notes = notes,
                timings = timings,
                previewTime = extras.previewTime,
                specialStyle = extras.specialStyle,
            )
        }

        private fun normalizeTimings(attributes: OsuBeatmapAttributes): List<ViewTiming> {
            val sorted = attributes.timings.sortedWith(
                compareBy<com.now.nowbot.model.beatmapParse.Timing> { it.startTime }
                    .thenBy { if (it.isRedLine) 0 else 1 },
            )
            var currentBeatLength = 60000.0 / 120
            return sorted.map { timing ->
                if (timing.isRedLine) {
                    currentBeatLength = timing.beatLength
                    val length = max(timing.beatLength, 1e-10)
                    ViewTiming(
                        time = timing.startTime,
                        beatLength = timing.beatLength,
                        sv = null,
                        bpm = Numbers.jsRound(60000 * 1000 / length) / 1000,
                        meter = timing.meter,
                        isRed = true,
                    )
                } else {
                    ViewTiming(
                        time = timing.startTime,
                        beatLength = currentBeatLength,
                        sv = timing.sliderVelocity,
                        bpm = null,
                        meter = timing.meter,
                        isRed = false,
                    )
                }
            }
        }

        private fun sliderEnd(
            startTime: Int,
            raw: RawHitObject?,
            attributes: OsuBeatmapAttributes,
            timings: List<ViewTiming>,
        ): Double? {
            val visualLength = raw?.sliderLength ?: return null
            val slideCount = raw.slideCount ?: return null
            if (timings.isEmpty()) return null

            var timing = timings[0]
            for (candidate in timings) {
                if (candidate.time <= startTime) {
                    timing = candidate
                } else {
                    break
                }
            }
            val sv = if (timing.isRed) 1.0 else timing.sv ?: 1.0
            val slideTime = (visualLength * timing.beatLength) / (attributes.sliderMultiplier * 100 * sv)
            return startTime + slideTime * slideCount
        }

        private fun soundMask(sound: HitObjectSound): Int {
            return when (sound) {
                HitObjectSound.WHISTLE -> 2
                HitObjectSound.FINISH -> 4
                HitObjectSound.CLAP -> 8
                HitObjectSound.NORMAL -> 1
                HitObjectSound.DEFAULT -> 0
            }
        }

        private fun scanExtras(text: String): Extras {
            var previewTime = 0
            var specialStyle = false
            val hitObjects = ArrayList<RawHitObject>()
            var section = ""
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("//")) continue
                if (line.startsWith("[")) {
                    section = line
                    continue
                }
                when (section) {
                    "[General]" -> {
                        val split = line.split(":", limit = 2)
                        if (split.size == 2) {
                            when (split[0].trim()) {
                                "PreviewTime" -> previewTime = split[1].trim().toIntOrNull() ?: 0
                                "SpecialStyle" -> specialStyle = (split[1].trim().toIntOrNull() ?: 0) == 1
                            }
                        }
                    }

                    "[HitObjects]" -> {
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val typeMask = parts[3].trim().toIntOrNull() ?: 0
                            val sliderLength = if ((typeMask and 2) != 0) {
                                parts.getOrNull(7)?.trim()?.toDoubleOrNull()?.toInt()
                            } else {
                                null
                            }
                            val slideCount = if ((typeMask and 2) != 0) {
                                parts.getOrNull(6)?.trim()?.toIntOrNull()
                            } else {
                                null
                            }
                            hitObjects += RawHitObject(
                                soundMask = parts.getOrNull(4)?.trim()?.toIntOrNull() ?: 0,
                                sliderLength = sliderLength,
                                slideCount = slideCount,
                            )
                        }
                    }
                }
            }
            return Extras(previewTime, specialStyle, hitObjects)
        }

        private val NOTE_ORDER = Comparator<ViewNote> { first, second ->
            when {
                first.time != second.time -> first.time - second.time
                first.endTime == null && second.endTime != null -> -1
                first.endTime != null && second.endTime == null -> 1
                first.endTime != null && second.endTime != null && first.endTime != second.endTime ->
                    first.endTime!!.compareTo(second.endTime!!)

                else -> first.x - second.x
            }
        }
    }
}

private data class RawHitObject(
    val soundMask: Int,
    val sliderLength: Int?,
    val slideCount: Int?,
)

private data class Extras(
    val previewTime: Int,
    val specialStyle: Boolean,
    val hitObjects: List<RawHitObject>,
)
