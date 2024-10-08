package com.now.nowbot.model.beatmapParse.newHitObject

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// 也可以看成一个向量
class Point(var x: Int = 0, var y: Int = 0) {
    fun add(point: Point): Point {
        return Point(this.x + point.x, this.y + point.y)
    }

    fun subtract(point: Point): Point {
        return Point(this.x - point.x, this.y - point.y)
    }

    fun distance(point: Point): Double {
        return sqrt(((this.x - point.x).toDouble().pow(2) + (this.y - point.y).toDouble().pow(2)))
    }

    // 中点
    fun middle(point: Point): Point {
        return Point((this.x - point.x) / 2, (this.y - point.y) / 2)
    }

    // 向量的模，点到原点的距离
    fun norm(): Double {
        return distance(Point(0, 0))
    }

    // 内积，点乘，innerProduct
    fun dotMultiply(point: Point): Double {
        return 1.0 * this.x * point.x + 1.0 * this.y * point.y
    }

    // 外积，叉乘，outerProduct
    fun crossMultiply(point: Point): Double {
        return 1.0 * this.x * point.y - 1.0 * point.x * this.y
    }

    fun scale(scale: Double): Point {
        return Point((this.x * scale).roundToInt(), (this.y * scale).roundToInt())
    }

    fun copy(): Point {
        return Point(this.x, this.y)
    }

    override fun toString(): String {
        return String.format("(%d, %d)", x, y)
    }
}
