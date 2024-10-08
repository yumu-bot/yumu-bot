package com.now.nowbot.model.beatmapParse.newHitObject

class HitCircle(val line: String): HitObject() {

    fun getInstance(): HitObject {
        val tokens = line.split(',')

        this.type = TYPE_CIRCLE


        return this
    }
}
