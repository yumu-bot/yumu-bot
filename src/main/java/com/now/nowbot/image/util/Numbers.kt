package com.now.nowbot.image.util

import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.floor as mathFloor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.truncate

data class NumberParts(
    val integer: String,
    val decimal: String,
) {
    val text: String get() = integer + decimal
}

object Numbers {
    private val JS_NUMBER = Regex("""^\s*(-?[0-9]+[.]?[0-9]*)\s*$""")

    fun clamp(value: Any?, maxValue: Double = 1.0, minValue: Double = 0.0): Double {
        val number = toDouble(value)
        if (number.isNaN() || (number < 1e-4 && number > -1e-4)) return 0.0
        val low = min(maxValue, minValue)
        val high = max(maxValue, minValue)
        return min(high, max(low, number))
    }

    fun clampToInteger(value: Any?, maxValue: Double = 1.0, minValue: Double = 0.0): Double {
        return jsRound(clamp(value, maxValue, minValue))
    }

    fun normalize(value: Double, maxValue: Double, minValue: Double, range: Double = 1.0, bottom: Double = 0.0): Double {
        var low = minValue
        var high = maxValue
        if (high < low) {
            val swap = low
            low = high
            high = swap
        }
        if (value <= low || low == high) return 0.0
        val progress = ((min(value, high) - low) / (high - low)) * range
        return max(progress, bottom)
    }

    fun isNumber(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Number -> !value.toDouble().isNaN()
            is String -> JS_NUMBER.matches(value)
            else -> false
        }
    }

    fun isNotNumber(value: Any?): Boolean = !isNumber(value)

    fun toDouble(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: Double.NaN
            else -> Double.NaN
        }
    }

    fun floor(value: Any?, level: Int = 0, subLevel: Int = 0): String = floorParts(value, level, subLevel).text

    fun floorParts(value: Any?, level: Int = 0, subLevel: Int = 0): NumberParts {
        return floorOrRound(value, level, subLevel, false)
    }

    fun round(value: Any?, level: Int = 0, subLevel: Int = 0): String = roundParts(value, level, subLevel).text

    fun roundParts(value: Any?, level: Int = 0, subLevel: Int = 0): NumberParts {
        return floorOrRound(value, level, subLevel, true)
    }

    fun ratioString(ratio: Double): String {
        if (ratio.isNaN() || ratio == 0.0) return "0"
        if (!ratio.isFinite()) return "Infinity"

        val negative = ratio < 0
        val absRatio = abs(ratio)
        val text = when {
            absRatio < 0.9 -> "1 : ${floor(1 / absRatio, 1)}"
            absRatio <= 1.0 -> "1 : ${floor(1 / absRatio, 2)}"
            absRatio <= 1.1 -> "${floor(absRatio, 2)} : 1"
            else -> "${floor(absRatio, 1)} : 1"
        }
        return if (negative) "-$text" else text
    }

    private fun floorOrRound(input: Any?, level: Int, subLevel: Int, isRound: Boolean): NumberParts {
        if (isNotNumber(input)) {
            return NumberParts((input ?: "").toString(), "")
        }

        val numeric = toDouble(input)
        if (!numeric.isFinite()) return NumberParts(input?.toString().orEmpty(), "")
        val sign = if (numeric < 0) "-" else ""
        var number = abs(numeric)

        var intStr = ""
        var decStr = ""

        if (subLevel == -1) {
            val floorString = jsNumber(mathFloor(number))
            val s0 = "0."
            val s1 = floorString.take(1) + "."
            val s2 = floorString.take(2)

            intStr = when {
                number < 1e3 -> floorString
                number < 1e4 -> s1
                number < 1e5 -> s2
                number < 1e6 -> s0
                number < 1e7 -> s1
                number < 1e8 -> s2
                number < 1e9 -> s0
                number < 1e10 -> s1
                number < 1e11 -> s2
                number < 1e12 -> s0
                number < 1e13 -> s1
                number < 1e14 -> s2
                number < 1e15 -> s0
                number < 1e16 -> s1
                else -> floorString
            }

            val unit = unit(number, -1)
            val s0Digit = floorString.take(1)
            val s1Digit = floorString.drop(1).take(1)
            decStr = when {
                number < 1e3 -> unit
                number < 1e4 -> s1Digit + unit
                number < 1e5 -> unit
                number < 1e6 -> s0Digit + unit
                number < 1e7 -> s1Digit + unit
                number < 1e8 -> unit
                number < 1e9 -> s0Digit + unit
                number < 1e10 -> s1Digit + unit
                number < 1e11 -> unit
                number < 1e12 -> s0Digit + unit
                number < 1e13 -> s1Digit + unit
                number < 1e14 -> unit
                number < 1e15 -> s0Digit + unit
                number < 1e16 -> s1Digit + unit
                else -> ""
            }
        } else if (level >= 0) {
            val unit = unit(number, 0)
            while (number >= 1000.0) number /= 1000.0

            var intPart = truncate(number)
            val dec = if (isRound) {
                var rounded = jsRound(abs(number - intPart) * 10.0.pow(level)) / 10.0.pow(level)
                if (abs(rounded) >= 1 - 10.0.pow(-level)) {
                    if (intPart < 0) intPart -= 1 else intPart += 1
                    rounded = 0.0
                }
                rounded
            } else {
                mathFloor(abs(number - intPart) * 10.0.pow(level)) / 10.0.pow(level)
            }

            if (intPart != 0.0 || subLevel != -2) {
                intStr = jsNumber(intPart)
                decStr = decimalDigits(dec, level)
                decStr = decStr.trimEnd('0')
                if (decStr.isNotEmpty()) {
                    intStr += "."
                    decStr += unit
                } else {
                    decStr = unit
                }
            } else {
                var decValue = decimalDigits(dec, level).trimEnd('0')
                intStr = if (decValue.isNotEmpty()) "$sign.$decValue" else "${sign}0"
                decStr = unit
            }
        } else {
            val str = if (subLevel == 1) jsNumber(mathFloor(number)).padStart(7, '0') else jsNumber(mathFloor(number))
            val times = abs(level)
            val split = when {
                number < 10.0.pow(times) -> mathFloor(number).toLong().toString().length
                number < 10.0.pow(2 * times) -> -times
                number < 10.0.pow(3 * times) -> -2 * times
                number < 10.0.pow(4 * times) -> -3 * times
                number < 10.0.pow(5 * times) -> -4 * times
                else -> 0
            }

            val boundary = if (split >= 0) split.coerceIn(0, str.length) else (str.length + split).coerceIn(0, str.length)
            intStr = str.substring(0, boundary)
            decStr = str.substring(boundary)
            if (subLevel == 2) {
                decStr = decStr.mapIndexed { index, c -> if (index % 4 == 0) " $c" else c.toString() }.joinToString("")
            }
        }

        val finalSign = if (intStr == "0" && (decStr.isEmpty() || decStr.all { it == '0' })) "" else sign
        return NumberParts(finalSign + intStr, decStr)
    }

    private fun decimalDigits(value: Double, level: Int): String {
        if (!value.isFinite()) return ""
        val text = jsDecimal(value)
        val dot = text.indexOf('.')
        if (dot < 0) return ""
        return text.substring(dot + 1).take(level)
    }

    private fun jsNumber(value: Double): String {
        if (!value.isFinite()) return value.toString()
        if (value == truncate(value) && abs(value) < 1e16) {
            return value.toLong().toString()
        }
        return jsDecimal(value)
    }

    private fun jsDecimal(value: Double): String {
        if (!value.isFinite()) return value.toString()
        val plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private fun unit(number: Double, pattern: Int): String {
        val num = abs(number)
        return when (pattern) {
            0 -> when {
                num < 1e3 -> ""
                num < 1e6 -> "K"
                num < 1e9 -> "M"
                num < 1e12 -> "B"
                num < 1e15 -> "T"
                num < 1e18 -> "Q"
                num < 1e21 -> "U"
                num < 1e24 -> "S"
                else -> "Z"
            }
            -1 -> when {
                num < 1e3 -> ""
                num < 1e5 -> "K"
                num < 1e8 -> "M"
                num < 1e11 -> "B"
                num < 1e14 -> "T"
                num < 1e17 -> "Q"
                num < 1e20 -> "U"
                num < 1e23 -> "S"
                else -> "Z"
            }
            else -> ""
        }
    }

    fun roundTo(value: Double, scale: Int): Double {
        if (!value.isFinite()) return value
        val factor = 10.0.pow(scale)
        return jsRound(value * factor) / factor
    }

    fun jsRound(value: Double): Double {
        if (!value.isFinite()) return value
        return mathFloor(value + 0.5)
    }
}
