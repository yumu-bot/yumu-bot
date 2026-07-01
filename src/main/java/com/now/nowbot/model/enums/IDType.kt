package com.now.nowbot.model.enums

enum class IDType(val abbr: String, val full: String) {
    BeatmapID("b", "bid"),
    BeatmapsetID("s", "sid");

    companion object {
        private val negatives = "-\u2010\u2013\u2014\u2212\uFF0D".toSet()
        private val assumeSet = "sS".toSet()

        // 1. 解析类型的方法
        fun fromString(typeStr: String?): IDType {
            val firstChar = typeStr?.trim()?.firstOrNull()

            return if (firstChar in negatives || firstChar in assumeSet) {
                BeatmapsetID
            } else {
                BeatmapID
            }
        }

        private fun clear(str: String?): Long? {
            if (str == null) return null
            val digits = str.filter { it.isDigit() }
            if (digits.isEmpty()) return null
            val hasMinus = str.any { it in negatives }
            return (if (hasMinus) "-$digits" else digits).toLongOrNull()
        }

        fun parse(typeStr: String?, idStr: String?): Pair<IDType, Long?> {
            val type = fromString(typeStr)
            val id = clear(idStr)
            return type to id
        }
    }
}