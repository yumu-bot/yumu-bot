package com.now.nowbot.model.beatmapParse

import com.now.nowbot.model.beatmapParse.hitObject.HitObjectPosition
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectSound
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType
import com.now.nowbot.model.beatmapParse.hitObject.SliderAttr

class HitObject {
    var position: HitObjectPosition = HitObjectPosition()
    var type: HitObjectType = HitObjectType.DEFAULT
    var sound: HitObjectSound = HitObjectSound.DEFAULT
    var startTime: Int = 0

    var endTime: Int = 0
        get() = if (field > 0) field else startTime

    var column: Int = 0

    /**
     * 仅 type == Slider 时存在
     */
    var sliderAttr: SliderAttr? = null

    constructor()

    constructor(x: Int, y: Int, type: HitObjectType, time: Int, end: Int) {
        this.position = HitObjectPosition(x, y)
        this.startTime = time
        this.endTime = end
        this.type = type
    }
}
