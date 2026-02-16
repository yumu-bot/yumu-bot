package com.now.nowbot.model.beatmapParse.hitObject

class SliderAttr {
    // 像素长度
    var length: Float = 0f

    // 重复次数 (折返点)
    var repeats: Int = 0

    // 控制点
    var controlPoints: List<SliderPoint> = listOf()

    var sounds: List<Int> = listOf()
}

class SliderPoint(x: Int, y: Int) : Point(x, y) {
    var type: PointType? = null
}

enum class PointType(val type: Int) {
    Catmull(0),
    Bezier(1),
    Linear(2),
    PerfectCircle(3),
    ;

    companion object {
        fun fromStr(s: String): PointType {
            if (s.isEmpty()) return fromChar('*')
            return fromChar(s.get(0))
        }

        fun fromChar(s: Char): PointType {
            return when (s) {
                'L' -> Linear
                'B' -> Bezier
                'P' -> PerfectCircle
                else -> Catmull
            }
        }
    }
}