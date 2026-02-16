package com.now.nowbot.model.beatmapParse.parse

import java.io.BufferedReader

class ManiaBeatmapAttributes(reader: BufferedReader, general: BeatmapGeneral?) : OsuBeatmapAttributes(reader, general) {
    init {
        for (line in hitObjects) {
            val column: Int = getColumn(line.position.x, cs.toInt())
            line.column = column
        }
    }

    companion object {
        private fun getColumn(x: Int, key: Int): Int {
            return (x * key / 512.0).toInt().coerceIn(0 ..< key)
        }
    }
}
