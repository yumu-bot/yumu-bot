package com.now.nowbot.util

import com.now.nowbot.throwable.TipsRuntimeException
import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

object TimeParser {
    private val BASE_DATE = LocalDate.of(2007, 9, 1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant()

    fun isRelativeTime(input: String): Boolean {
        val relativeRegex = ".*\\d+\\s*([a-zA-Z\\u4e00-\\u9fa5]+).*".toRegex()

        return relativeRegex.matches(input) && !input.contains("-") && !input.contains("/")
    }

    fun process(input: String): ZonedDateTime {
        val now = ZonedDateTime.now()
        val thresholdMs = now.toInstant().toEpochMilli() - BASE_DATE.toEpochMilli()

        return try {
            when {
                // 1. 包含日期分隔符，走绝对时间逻辑
                input.contains("-") || input.contains("/") -> {
                    parseAbsoluteDateTime(input)
                }

                // 2. 包含时间单位，走相对时间逻辑
                isRelativeTime(input) -> {
                    val (period, duration) = parseRelative(input)
                    // 注意：period.days 仅仅获取天数部分，不含月和年。
                    // 建议使用更准确的粗略估算方式：
                    val totalMonths = period.toTotalMonths()
                    val totalDays = (totalMonths * 30) + period.days
                    val approxMillis = totalDays * 24L * 3600L * 1000L + duration.toMillis()

                    if (approxMillis < thresholdMs) {
                        now.minus(period).minus(duration)
                    } else {
                        // 跳转点逻辑：低位补0 (按天截断)
                        Instant.ofEpochMilli(approxMillis)
                            .atZone(ZoneId.systemDefault())
                            .truncatedTo(java.time.temporal.ChronoUnit.DAYS)
                    }
                }

                else -> throw IllegalArgumentException("未知格式")
            }
        } catch (e: Exception) {
            throw TipsRuntimeException("解析错误: ${e.message}")
        }
    }

    // 同时解析 Period (YMD) 和 Duration (HMS)
    private fun parseRelative(input: String): Pair<Period, Duration> {
        var period = Period.ZERO
        var duration = Duration.ZERO

        val regex = "(\\d+)\\s*([a-zA-Z\\u4e00-\\u9fa5]+)".toRegex()

        regex.findAll(input).forEach {
            val (value, unit) = it.destructured
            val num = value.toLongOrNull() ?: 0L

            when (unit.lowercase()) {
                // 年
                in listOf("y", "year", "years", "年") ->
                    period = period.plusYears(num)

                // 月 (注意：这里用 mo 或 month 区分分钟 m)
                in listOf("mo", "month", "months", "月") ->
                    period = period.plusMonths(num)

                // 周
                in listOf("w", "week", "weeks", "周", "星期") ->
                    period = period.plusDays(num * 7)

                // 天
                in listOf("d", "day", "days", "天", "日", "天数") ->
                    period = period.plusDays(num)

                // 时
                in listOf("h", "hour", "hours", "时", "小时") ->
                    duration = duration.plusHours(num)

                // 分
                in listOf("m", "min", "minute", "minutes", "分", "分钟") ->
                    duration = duration.plusMinutes(num)

                // 秒
                in listOf("s", "sec", "second", "seconds", "秒") ->
                    duration = duration.plusSeconds(num)

                else -> duration = duration.plusSeconds(num)
            }
        }

        return period to duration
    }

    private fun parseAbsoluteDateTime(input: String): ZonedDateTime {
        val cleaned = input.replace("[/\\\\_.]".toRegex(), "-")

        val localDateTime = LocalDateTime.parse(cleaned, dateDataFormatter)

        return localDateTime.atZone(ZoneId.systemDefault())
    }

    private val dateDataFormatter = DateTimeFormatterBuilder()
        // 1. 年份 (必填)
        .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)

        // 2. 月份 (必填)
        .optionalStart().appendPattern("[-/. \\\\]").optionalEnd() // 简写分隔符匹配
        .appendValue(ChronoField.MONTH_OF_YEAR)

        // 3. 日期 (可选) -> 如果省略，默认为该月 1 号
        .optionalStart()
        .optionalStart().appendPattern("[-/. \\\\]").optionalEnd()
        .appendValue(ChronoField.DAY_OF_MONTH)
        .optionalEnd()

        // 4. 时间部分 (整体可选)
        .optionalStart()
        .appendLiteral(" ")
        .appendValue(ChronoField.HOUR_OF_DAY)

        // 分钟 (可选)
        .optionalStart()
        .appendLiteral(":")
        .appendValue(ChronoField.MINUTE_OF_HOUR)

        // 秒 (可选)
        .optionalStart()
        .appendLiteral(":")
        .appendValue(ChronoField.SECOND_OF_MINUTE)
        .optionalEnd() // 结束秒
        .optionalEnd() // 结束分
        .optionalEnd() // 结束时间整体

        // 5. 默认值：关键在于 DAY_OF_MONTH 也需要默认值
        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1) // 省略日期时默认为 1 号
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter()
}