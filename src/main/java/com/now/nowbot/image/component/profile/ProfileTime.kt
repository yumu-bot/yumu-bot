package com.now.nowbot.image.component.profile

import com.now.nowbot.image.svg.Svg
import kotlin.math.floor

object ProfileTime {
    data class Parts(val large: String, val small: String)

    fun parts(seconds: Double, whitespace: Boolean): Parts {
        if (seconds <= 0) return Parts("-", "")
        val value = kotlin.math.abs(seconds)
        val days = floor(value / 86400).toLong()
        val hours = floor((value % 86400) / 3600).toLong()
        val minutes = floor((value % 3600) / 60).toLong()
        val sec = value % 60
        return when {
            days > 0 -> Parts(days.toString(), if (whitespace) "d $hours" + "h " + minutes + "m" else "d$hours" + "h" + minutes + "m")
            hours > 0 -> Parts(hours.toString(), if (whitespace) "h $minutes" + "m" else "h$minutes" + "m")
            minutes > 0 -> Parts(minutes.toString(), if (whitespace) "m " + Svg.number(sec) + "s" else "m" + Svg.number(sec) + "s")
            sec > 0 -> Parts(Svg.number(sec), "s")
            else -> Parts("0", "s")
        }
    }

    fun large(seconds: Double, whitespace: Boolean = false): String = parts(seconds, whitespace).large
    fun small(seconds: Double, whitespace: Boolean = false): String = parts(seconds, whitespace).small
    fun full(seconds: Double, whitespace: Boolean = false): String = parts(seconds, whitespace).let { it.large + it.small }
}
