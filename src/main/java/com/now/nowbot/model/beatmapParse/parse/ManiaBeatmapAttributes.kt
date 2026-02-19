package com.now.nowbot.model.beatmapParse.parse

import java.io.BufferedReader

class ManiaBeatmapAttributes(reader: BufferedReader, general: BeatmapGeneral?) : OsuBeatmapAttributes(reader, general) {
    init {
        val cs = cs.toInt()

        for (line in hitObjects) {
            line.column = getColumn(
                line.position.x, cs
            )
        }
    }

    companion object {
        private fun getColumn(x: Int, key: Int): Int {
            return (x * key / 512.0).toInt().coerceIn(0 ..< key)
        }
    }
}
