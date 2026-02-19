package com.now.nowbot.model.beatmapParse.hitObject

enum class HitObjectType(var value: Int) {
    DEFAULT(0),
    CIRCLE(1),
    SLIDER(2),
    SPINNER(8),
    LONGNOTE(128);

    override fun toString(): String {
        return name.lowercase()
    }

    companion object {
        fun getType(value: Int): HitObjectType = when {
            (value and 1) != 0 -> CIRCLE
            (value and 2) != 0 -> SLIDER
            (value and 8) != 0 -> SPINNER
            (value and 128) != 0 -> LONGNOTE
            else -> DEFAULT
        }
    }
}