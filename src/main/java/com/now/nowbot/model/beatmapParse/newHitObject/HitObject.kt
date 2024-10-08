package com.now.nowbot.model.beatmapParse.newHitObject

abstract class HitObject {
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

    /** Current index in combo color array.  */
    var comboIndex = 0

    /** Number to display in hit object.  */
    var comboNumber = 0

    /** Hit object index in the current stack.  */
    var stack = 0

    fun getInstance(line: String?) : HitObject {
        if (line.isNullOrEmpty()) return HitCircle("")

        val tokens = line.split(',')

        if (tokens.size <= 3) {
            return HitCircle("")
        }

        /*
        物件格式：
        x,y,时间,物件类型,打击音效,物件参数,打击音效组
        x（整型） 和 y（整型）： 物件的位置，原点在左上角，单位是 osu! 像素。
        时间（整型）： 物件精确击打的时间。以谱面音频开始为原点，单位是毫秒。
        物件类型（整型）： 一位标记物件类型的数据。参见：类型部分。
        打击音效（整型）： 一位标记物件打击音效的数据。参见：音效部分。
        物件参数（逗号分隔的数组）： 根据物件类型不同附加的一些参数。
        打击音效组（冒号分隔的数组）： 击打物件时，决定具体该播放哪些音效的一些参数。与打击音效参数关系密切。参见：音效部分。如果没有设置特殊参数，则默认为 0:0:0:0:。
        */

        this.point = Point(tokens[0].toInt(), tokens[1].toInt())
        this.time = tokens[2].toInt()
        this.type = tokens[3].toInt()

        when(tokens[3].toInt()) {
            TYPE_CIRCLE -> return HitCircle(line).getInstance()
            TYPE_SLIDER -> return HitCircle(line)
            TYPE_SPINNER -> return HitCircle(line)
        }



        return HitCircle("")
    }
}
