package com.now.nowbot.model.beatmapParse.parse

import com.now.nowbot.model.enums.OsuMode

class BeatmapGeneral(var version: Int) {
    @JvmField
    var sampleSet: String? = null
    @JvmField
    var stackLeniency: Double? = null
    @JvmField
    var mode: OsuMode? = null
}
