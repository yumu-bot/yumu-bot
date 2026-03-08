package com.now.nowbot.util

import com.now.nowbot.util.command.REG_COLON
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object TimeParser {
    private val REGEX = Regex("""(\d+)\\s*([a-z]*)""")

    private val YEAR_RANGE_REGEX = Regex("""^(\d{4})(?:-(\d{4}))?$""")

    enum class ColonMode { HOUR_MIN, MIN_SEC }

    // 定义返回包装类
    data class ParsedTime(
        val time: LocalDateTime,
        val unit: ChronoUnit
    )

    data class ParsedDuration(
        val duration: Duration,
        val unit: ChronoUnit
    )

    /**
     * 最小单位后都是 0 的情况
     */
    fun getDeltaFromUnit(time: LocalDateTime, unit: ChronoUnit): Duration {
        return when (unit) {
            YEARS -> (time.dayOfYear - 1).days + time.hour.hours + time.minute.minutes + time.second.seconds
            MONTHS -> (time.dayOfMonth - 1).days + time.hour.hours + time.minute.minutes + time.second.seconds
            WEEKS -> (time.dayOfWeek.value - 1).days + time.hour.hours + time.minute.minutes + time.second.seconds
            else -> 1.days
        }
    }

    /**
     * 解析为“一段时间”并返回最小单位
     * 返回 Pair(时长, 最小物理单位)
     */
    fun parseDuration(
        input: String,
        defaultUnit: String = "s",
        colonMode: ColonMode = ColonMode.MIN_SEC
    ): ParsedDuration {
        val raw = input.trim().lowercase()
        if (raw.isEmpty()) return ParsedDuration(Duration.ZERO, convertToChronoUnit(defaultUnit))

        return when {
            // 1. 处理冒号格式
            raw.contains(REG_COLON.toRegex()) -> {
                val parts = raw.split(REG_COLON.toRegex()).map { it.toLong() }
                val d: Duration
                val unit: ChronoUnit

                if (colonMode == ColonMode.HOUR_MIN) {
                    d = when (parts.size) {
                        3 -> {
                            unit = SECONDS; parts[0].hours + parts[1].minutes + parts[2].seconds
                        }

                        2 -> {
                            unit = MINUTES; parts[0].hours + parts[1].minutes
                        }

                        else -> {
                            unit = MINUTES; Duration.ZERO
                        }
                    }
                } else {
                    d = when (parts.size) {
                        3 -> {
                            unit = SECONDS; parts[0].hours + parts[1].minutes + parts[2].seconds
                        }

                        2 -> {
                            unit = SECONDS; parts[0].minutes + parts[1].seconds
                        }

                        else -> {
                            unit = SECONDS; Duration.ZERO
                        }
                    }
                }
                ParsedDuration(d, unit)
            }

            // 2. 处理复合或单单位格式 (如 10m30s)
            else -> {
                val regex = REGEX
                val matches = regex.findAll(raw).toList()

                if (matches.isEmpty()) return ParsedDuration(Duration.ZERO, convertToChronoUnit(defaultUnit))

                var total = Duration.ZERO
                var lastUnit = convertToChronoUnit(defaultUnit)

                for (match in matches) {
                    val value = match.groupValues[1].toLong()
                    val unitStr = match.groupValues[2].ifEmpty { defaultUnit }

                    // 更新当前循环的单位
                    val currentUnit = convertToChronoUnit(unitStr)
                    lastUnit = currentUnit // 最后一个匹配到的单位即为最小单位

                    total += when (unitStr) {
                        "y", "year" -> (value * 365).days
                        "mo", "month" -> (value * 30).days
                        "d", "day" -> value.days
                        "h", "hour" -> value.hours
                        "m", "min" -> value.minutes
                        "s", "sec" -> value.seconds
                        else -> Duration.ZERO
                    }
                }
                ParsedDuration(total, lastUnit)
            }
        }
    }

    fun parseBackwards(
        input: String,
        defaultUnit: String = "s",
        colonMode: ColonMode = ColonMode.HOUR_MIN
    ): ParsedTime {
        val now = LocalDateTime.now()
        val trimmed = input.trim()

        // 情况 A: 年份/区间 (同前)
        val yearMatch = YEAR_RANGE_REGEX.find(trimmed)
        if (yearMatch != null) {
            return ParsedTime(now.withYear(yearMatch.groupValues[1].toInt()), YEARS)
        }

        // 情况 B: 日期格式 (同前)
        if (trimmed.contains("-") || trimmed.contains("/")) {
            val normalized = trimmed.replace("/", "-")
            val parts = normalized.split("-")
            if (!(parts.size == 2 && parts[0].length == 4 && parts[1].length == 4)) {
                val pattern = if (parts[0].length == 2) "yy-MM-dd" else "yyyy-MM-dd"
                val date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern))
                // 日期格式的最小单位通常是 DAYS
                return ParsedTime(date.atTime(now.toLocalTime()), DAYS)
            }
        }

        // 情况 C: 所有的时长/冒号逻辑统一走 parseDuration
        val (duration, minUnit) = parseDuration(trimmed, defaultUnit, colonMode)
        val finalTime = now.minusNanos(duration.inWholeNanoseconds)

        return ParsedTime(finalTime, minUnit)
    }

    private fun convertToChronoUnit(unit: String): ChronoUnit = when (unit) {
        "y", "year" -> YEARS
        "mo", "month" -> MONTHS
        "d", "day" -> DAYS
        "h", "hour" -> HOURS
        "m", "min" -> MINUTES
        "s", "sec" -> SECONDS
        else -> SECONDS
    }
}