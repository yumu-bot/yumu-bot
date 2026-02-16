package com.now.nowbot.model.beatmapParse.hitObject

import kotlin.math.roundToInt
import kotlin.math.sqrt

open class Point {
    var x: Int
    var y: Int

    constructor() {
        x = 0
        y = 0
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun dotMultiply(other: Point): Int {
        return x * other.x + y * other.y

    }

    fun distance(): Double {
        return sqrt(dotMultiply(this).toDouble())
    }

    fun normalize(): Point {
        return this.divide(this.distance())
    }

    fun plus(x: Int, y: Int): Point {
        return Point(this.x + x, this.y + y)
    }

    fun minus(x: Int, y: Int): Point {
        return Point(this.x - x, this.y - y)
    }

    fun multiply(multiplier: Double): Point {
        return Point((x * multiplier).roundToInt(), (y * multiplier).roundToInt())
    }

    fun divide(divisor: Double): Point {
        if (divisor != 0.0) {
            return Point((x / divisor).roundToInt(), (y / divisor).roundToInt())
        } else {
            throw RuntimeException("除数为0")
        }
    }
}
