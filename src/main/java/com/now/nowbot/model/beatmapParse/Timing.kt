package com.now.nowbot.model.beatmapParse

import com.now.nowbot.model.beatmapParse.timing.TimingEffect
import com.now.nowbot.model.beatmapParse.timing.TimingSampleSet

data class Timing(
    var startTime: Int,
    var beatLength: Double,
    var meter: Int, // 节拍
    var sampleSet: TimingSampleSet,
    var sampleParameter: Int,
    var volume: Int,
    var isRedLine: Boolean,
    var effect: TimingEffect,
) {
    // BPM 计算属性
    var bpm: Double = 0.0
        get() = if (field > 0.0) field else 60000.0 / beatLength

    val sliderVelocity: Double
        get() = if (isRedLine) {
            1.0
        } else {
            100.0 / (-1.0 * beatLength)
        }

    val safeBeatLength: Double
        get() = if (isRedLine) beatLength else 0.0
}