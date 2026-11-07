package com.now.nowbot.image.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.truncate

object TimeFormat {
    private const val OFFSET_HOURS = 8
    private val zone = ZoneOffset.ofHours(OFFSET_HOURS)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun formatted(time: OffsetDateTime?): String {
        if (time == null) return "-"
        return dateTimeFormatter.format(time.toLocalDateTime().plusHours(OFFSET_HOURS.toLong())) + " +$OFFSET_HOURS"
    }

    fun formatted(value: String?): String {
        val wall = parseWall(value) ?: return "-"
        return dateTimeFormatter.format(wall.plusHours(OFFSET_HOURS.toLong())) + " +$OFFSET_HOURS"
    }

    fun difference(value: String?, short: Int = 0, now: Instant = Instant.now()): String {
        val wall = parseWall(value) ?: return "-"
        return difference(wall, short, now)
    }

    fun nowTimestamp(): String {
        return dateTimeFormatter.format(Instant.now().atZone(zone)) + " +$OFFSET_HOURS"
    }

    fun difference(time: OffsetDateTime?, short: Int = 0, now: Instant = Instant.now()): String {
        if (time == null) return "-"
        return difference(time.toLocalDateTime(), short, now)
    }

    private fun difference(wall: LocalDateTime, short: Int, now: Instant): String {
        val compare = wall.atZone(ZoneOffset.UTC)
        val current = now.atZone(ZoneOffset.UTC)
        val years = truncate(monthDiff(compare, current) / 12)
        val months = truncate(monthDiff(compare, current))
        val millis = Duration.between(current.toInstant(), compare.toInstant()).toMillis().toDouble()
        val days = truncate(millis / 86400000.0)
        val hours = truncate(millis / 3600000.0)
        val minutes = truncate(millis / 60000.0)

        return when (short) {
            2 -> when {
                abs(years) > 0 -> "${years.toLong()}" + if (abs(years) > 1) "years" else "year"
                abs(months) > 0 -> "${months.toLong()}" + if (abs(months) > 1) "months" else "month"
                abs(days) > 0 -> "${days.toLong()}" + if (abs(days) > 1) "days" else "day"
                abs(hours) > 0 -> "${hours.toLong()}" + if (abs(hours) > 1) "hours" else "hour"
                abs(minutes) > 0 -> "${minutes.toLong()}" + if (abs(minutes) > 1) "minutes" else "minute"
                else -> "now"
            }
            1 -> when {
                abs(years) > 0 -> "${years.toLong()}" + if (abs(years) > 1) "yrs" else "yr"
                abs(months) > 0 -> "${months.toLong()}" + if (abs(months) > 1) "mos" else "mo"
                abs(days) > 0 -> "${days.toLong()}" + if (abs(days) > 1) "dys" else "dy"
                abs(hours) > 0 -> "${hours.toLong()}" + if (abs(hours) > 1) "hrs" else "hr"
                abs(minutes) > 0 -> "${minutes.toLong()}" + if (abs(minutes) > 1) "mins" else "min"
                else -> "now"
            }
            else -> when {
                abs(years) > 0 -> "${years.toLong()}y"
                abs(months) > 0 -> "${months.toLong()}mo"
                abs(days) > 0 -> "${days.toLong()}d"
                abs(hours) > 0 -> "${hours.toLong()}h"
                abs(minutes) > 0 -> "${minutes.toLong()}m"
                else -> "now"
            }
        }
    }

    private fun monthDiff(a: ZonedDateTime, b: ZonedDateTime): Double {
        if (a.dayOfMonth < b.dayOfMonth) return -monthDiff(b, a)
        val wholeMonthDiff = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
        val anchor = a.plusMonths(wholeMonthDiff.toLong())
        val anchor2: ZonedDateTime
        val adjust: Double
        if (b.isBefore(anchor)) {
            anchor2 = a.plusMonths((wholeMonthDiff - 1).toLong())
            adjust = Duration.between(anchor.toInstant(), b.toInstant()).toMillis().toDouble() /
                Duration.between(anchor2.toInstant(), anchor.toInstant()).toMillis().toDouble()
        } else {
            anchor2 = a.plusMonths((wholeMonthDiff + 1).toLong())
            adjust = Duration.between(anchor.toInstant(), b.toInstant()).toMillis().toDouble() /
                Duration.between(anchor.toInstant(), anchor2.toInstant()).toMillis().toDouble()
        }
        val value = -(wholeMonthDiff + adjust)
        return if (value == 0.0) 0.0 else value
    }

    fun shortDifference(time: OffsetDateTime?): String = difference(time, 1)

    private fun parseWall(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        runCatching { OffsetDateTime.parse(value) }.getOrNull()?.let { return it.toLocalDateTime() }
        runCatching { LocalDateTime.parse(value) }.getOrNull()?.let { return it }
        runCatching { LocalDate.parse(value) }.getOrNull()?.let { return it.atStartOfDay() }
        runCatching { Instant.parse(value) }.getOrNull()?.let { return LocalDateTime.ofInstant(it, ZoneOffset.UTC) }
        return null
    }
}
