package com.now.nowbot.model.beatmapParse.newHitObject

import com.yumu.core.extensions.isNotNull

open class HitObject(line: String?) {
    protected val TYPE_CIRCLE = 1
    protected val TYPE_SLIDER = 2
    protected val TYPE_NEW_COMBO = 4
    protected val TYPE_SPINNER = 8

    protected val SOUND_NORMAL = 0
    protected val SOUND_WHISTLE = 2
    protected val SOUND_FINISH = 4
    protected val SOUND_CLAP = 8

    protected val SLIDER_CATMULL = 'C'
    protected val SLIDER_BEZIER = 'B'
    protected val SLIDER_LINEAR = 'L'
    protected val SLIDER_PERFECT_CURVE = 'P'

    /** Max hit object coordinates.  */
    protected val MAX_X: Int = 512
    protected val MAX_Y: Int = 384

    /** Starting coordinates.  */
    var point: Point = Point(0, 0)

    /** Start time (in ms).  */
    var time = 0

    /** Hit object type (TYPE_* bitmask).  */
    var type = 0

    /** Hit sound type (SOUND_* bitmask).  */
    var hitSound: Short = 0

    /** Hit sound addition (sampleSet, AdditionSampleSet).  */
    var addition: ByteArray = byteArrayOf()

    /** Addition custom sample index.  */
    var additionCustomSampleIndex: Byte = 0

    /** Addition hit sound volume.  */
    var additionHitSoundVolume = 0

    /** Addition hit sound file.  */
    var additionHitSound: String? = null

    /** Slider curve type (SLIDER_* constant).  */
    var sliderType = 0.toChar()

    /** Slider coordinate lists.  */
    var sliderX: DoubleArray = doubleArrayOf()

    /** Slider coordinate lists.  */
    var sliderY: DoubleArray = doubleArrayOf()

    /** Slider repeat count.  */
    var repeat = 0

    /** Slider pixel length.  */
    var pixelLength = 0f

    /** Spinner end time (in ms).  */
    var endTime = 0

    /** Slider edge hit sound type (SOUND_* bitmask).  */
    var edgeHitSound: ShortArray = shortArrayOf()

    /** Slider edge hit sound addition (sampleSet, AdditionSampleSet).  */
    var edgeAddition: ByteArray = byteArrayOf()

    /** Hit object index in the current stack.  */
    var stack = 0

    init {
        if (line.isNotNull() && line?.isNotBlank() == true) {
            val tokens = line.split(',')

            this.point = Point(tokens[0].toInt(), tokens[1].toInt())
            this.time = tokens[2].toInt()
            this.type = tokens[3].toInt()

        }
    }
}

class HitCircle(point: Point, time: Int, ) {

}
