package com.now.nowbot.image.util

import com.now.nowbot.image.svg.Svg
import kotlin.math.abs
import kotlin.math.floor

fun TimeFormat.duration(seconds: Double, whitespace: Boolean = false): String {
    if (!(seconds > 0)) return "-"

    val value = abs(seconds)
    val days = floor(value / 86400).toLong()
    val hours = floor((value % 86400) / 3600).toLong()
    val minutes = floor((value % 3600) / 60).toLong()
    val sec = value % 60

    val large: String
    val small: String
    if (whitespace) {
        when {
            days > 0 -> {
                large = days.toString()
                small = "d $hours" + "h " + minutes + "m"
            }

            hours > 0 -> {
                large = hours.toString()
                small = "h " + minutes + "m"
            }

            minutes > 0 -> {
                large = minutes.toString()
                small = "m " + Svg.number(sec) + "s"
            }

            sec > 0 -> {
                large = Svg.number(sec)
                small = "s"
            }

            else -> {
                large = "0"
                small = "s"
            }
        }
    } else {
        when {
            days > 0 -> {
                large = days.toString()
                small = "d" + hours + "h" + minutes + "m"
            }

            hours > 0 -> {
                large = hours.toString()
                small = "h" + minutes + "m"
            }

            minutes > 0 -> {
                large = minutes.toString()
                small = "m" + Svg.number(sec) + "s"
            }

            sec > 0 -> {
                large = Svg.number(sec)
                small = "s"
            }

            else -> {
                large = "0"
                small = "s"
            }
        }
    }
    return large + small
}
