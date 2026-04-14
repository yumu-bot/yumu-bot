package com.now.nowbot.util

object MaimaiUtil {

    private val RATING_TABLE = listOf(
        100.5 to 22.4,
        100.0 to 21.6,
        99.5 to 21.1,
        99.0 to 20.8,
        98.0 to 20.3,
        97.0 to 20.0,
        94.0 to 16.8,
        90.0 to 15.2,
        80.0 to 13.6,
        75.0 to 12.0,
        70.0 to 11.2,
        60.0 to 9.6,
        50.0 to 8.0,
        40.0 to 6.4,
        30.0 to 4.8,
        20.0 to 3.2,
        10.0 to 1.6
    )

    fun getRating(star: Double, achievements: Double): Int {
        val accLevel = RATING_TABLE.firstOrNull { achievements >= it.first }?.second ?: 0.0

        return (star * achievements.coerceIn(0.0 .. 100.5) / 100.0 * accLevel).toInt()
    }
}