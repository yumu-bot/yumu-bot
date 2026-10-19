package com.now.nowbot.util

import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.util.command.PATTERN_CHINESE_NUMBER
import com.now.nowbot.util.command.PATTERN_DURATION_NUMBER
import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

object TimeParser {
    val BASE_DATE: LocalDate = LocalDate.of(2007, 9, 1)

    private val COLON_DURATION_REGEX = Regex("""(?:$PATTERN_DURATION_NUMBER)(?::$PATTERN_DURATION_NUMBER){0,2}""")

    private val DURATION_TOKEN_REGEX = Regex(
        """($PATTERN_DURATION_NUMBER)(hours|hour|hrs|hr|h|minutes|minute|mins|min|m|seconds|second|secs|sec|s|days|day|d|小时|时|分钟|分|秒|天|日)"""
    )

    private val CHINESE_NUMBER_REGEX = Regex("""$PATTERN_CHINESE_NUMBER""")

    private val CHINESE_DIGITS = mapOf(
        '零' to 0, '〇' to 0,
        '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )

    private val CHINESE_SMALL_UNITS = mapOf(
        '十' to 10L,
        '百' to 100L,
        '千' to 1000L,
    )

    private val CHINESE_BIG_UNITS = mapOf(
        '万' to 10_000L,
        '亿' to 100_000_000L,
    )

    fun isRelativeTime(input: String): Boolean {
        val relativeRegex = ".*\\d+\\s*([a-zA-Z\\u4e00-\\u9fa5]+).*".toRegex()

        return relativeRegex.matches(input) && !input.contains("-") && !input.contains("/")
    }

    fun process(input: String): ZonedDateTime {
        val now = ZonedDateTime.now()
        val thresholdMs = now.toInstant().toEpochMilli() - BASE_DATE
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                            .truncatedTo(ChronoUnit.DAYS)
                    }
                }

                else -> throw IllegalArgumentException("未知格式")
            }
        } catch (e: Exception) {
            throw TipsRuntimeException("解析错误: ${e.message}")
        }
    }

    /**
     * 将“时长”解析为秒数。
     *
     * 注意这和 [process] 是两种语义：[process] 解析的是“时间点”（过去某天、某时），
     * 而谱面长度、成绩时长等需要的是“持续了多久”，所以不能复用 [process] 的返回值。
     *
     * 支持的写法：
     * - 纯数字或中文数词：按秒解释，如 `length>90`、`length>九十`；
     * - 冒号：`mm:ss` 或 `hh:mm:ss`，如 `3:30` = 210 秒、`1:02:03` = 3723 秒；
     * - 单位：`1h2m3s`、`3分30秒`、`1小时30分钟`、`三分三十秒`，单位可混用、顺序不限。
     *
     * 中文数词支持常见的整数写法：零/〇、一~九、十/百/千/万/亿、两，以及廿/卅。
     * 例如 `三` = 3 秒、`十` = 10 秒、`一百零五` = 105 秒、`两千` = 2000 秒。
     * 暂不支持 `半`（如 `半小时`），因为它的结果不一定是整数秒。
     *
     * 与原来的 `str.filter { it.isDigit() }` 不同，冒号和时间单位会按结构换算，
     * 不会再出现 `3:30` 被拼成 `330` 秒这种“强行读取秒数”的问题。
     *
     * @throws TipsRuntimeException 无法识别时抛出带示例的提示
     */
    fun parseDurationSeconds(input: String): Long {
        // 兼容全角冒号、单位之间带空格（如 "1 h 30 m"）以及首尾空白
        val text = input.trim()
            .replace('：', ':')
            .replace("\\s+".toRegex(), "")
            .lowercase()

        if (text.isEmpty()) throw invalidDuration(input)

        // 1. 纯数字：沿用旧语义，按秒处理
        if (text.all { it.isDigit() }) {
            return text.toLongOrNull() ?: throw invalidDuration(input)
        }

        // 2. 冒号格式：从右往左依次是秒、分、时
        if (COLON_DURATION_REGEX.matches(text)) {
            val parts = text.split(':').map { parseDurationNumber(it, input) }

            return when (parts.size) {
                1 -> parts[0]
                2 -> parts[0] * 60 + parts[1]
                else -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            }
        }

        // 3. 单位格式：必须把整个字符串完整消费掉，避免 "1h2x" 之类的半截解析
        var cursor = 0
        var total = 0L

        for (match in DURATION_TOKEN_REGEX.findAll(text)) {
            if (match.range.first != cursor) throw invalidDuration(input)

            val value = parseDurationNumber(match.groupValues[1], input)
            val unit = match.groupValues[2]

            total += value * unitSeconds(unit)
            cursor = match.range.last + 1
        }

        if (cursor == 0 || cursor != text.length) throw invalidDuration(input)

        return total
    }

    private fun parseDurationNumber(token: String, original: String): Long {
        token.toLongOrNull()?.let { return it }

        if (!CHINESE_NUMBER_REGEX.matches(token)) throw invalidDuration(original)

        return parseChineseNumber(token, original)
    }

    /**
     * 把常见中文整数转成 Long。
     *
     * 支持：零/〇、一~九、两、十/百/千、万/亿，以及廿/卅（先归一化成二十/三十）。
     * 采用“节”的方式处理万/亿，因此 `一万二千`、`一亿二千万` 都能正确换算。
     */
    private fun parseChineseNumber(source: String, original: String): Long {
        val text = source.replace("廿", "二十").replace("卅", "三十")

        var total = 0L
        var section = 0L
        var number = 0L
        var hasNumber = false
        var lastSmallUnit = Long.MAX_VALUE
        var lastWasZero = false

        for (ch in text) {
            val digit = CHINESE_DIGITS[ch]

            if (digit != null) {
                if (digit == 0) {
                    // 非零数字后面不能再直接跟零，例如“一零”不是合法数词
                    if (hasNumber && number != 0L) throw invalidDuration(original)
                    if (lastWasZero) throw invalidDuration(original)

                    number = 0L
                    hasNumber = true
                    lastWasZero = true
                } else {
                    // 两个非零数字不能连续出现，例如“一二”
                    if (hasNumber && number != 0L) throw invalidDuration(original)

                    number = digit.toLong()
                    hasNumber = true
                    lastWasZero = false
                }
                continue
            }

            lastWasZero = false

            val smallUnit = CHINESE_SMALL_UNITS[ch]
            if (smallUnit != null) {
                // 小单位必须从大到小，例如“一百二十”可以，“十百”不行
                if (smallUnit >= lastSmallUnit) throw invalidDuration(original)

                if (!hasNumber) {
                    // “十”“百”“千”单独出现时按 10、100、1000 处理
                    if (total == 0L && section == 0L) {
                        section += smallUnit
                    } else {
                        throw invalidDuration(original)
                    }
                } else {
                    if (number == 0L) throw invalidDuration(original)
                    section += number * smallUnit
                }

                number = 0L
                hasNumber = false
                lastSmallUnit = smallUnit
                continue
            }

            val bigUnit = CHINESE_BIG_UNITS[ch] ?: throw invalidDuration(original)

            if (bigUnit == 100_000_000L) {
                // 亿要把前面累计的“万级”一起乘上去，才能支持“一万亿”
                val base = total + section + (if (hasNumber) number else 0L)

                total = when {
                    base > 0L -> base * bigUnit
                    !hasNumber && total == 0L && section == 0L -> bigUnit
                    else -> throw invalidDuration(original)
                }
            } else {
                section += (if (hasNumber) number else 0L)

                if (section == 0L) {
                    if (total == 0L && !hasNumber) {
                        // “万”单独出现按一万处理
                        section = 1L
                    } else {
                        throw invalidDuration(original)
                    }
                }

                total += section * bigUnit
            }

            section = 0L
            number = 0L
            hasNumber = false
            lastSmallUnit = Long.MAX_VALUE
        }

        return total + section + (if (hasNumber) number else 0L)
    }

    private fun unitSeconds(unit: String): Long {
        return when (unit) {
            "h", "hr", "hrs", "hour", "hours", "小时", "时" -> 3600L
            "m", "min", "mins", "minute", "minutes", "分", "分钟" -> 60L
            "s", "sec", "secs", "second", "seconds", "秒" -> 1L
            "d", "day", "days", "天", "日" -> 86400L
            else -> throw IllegalArgumentException("未知的时间单位: $unit")
        }
    }

    private fun invalidDuration(input: String): TipsRuntimeException {
        return TipsRuntimeException("无法解析时长：$input（示例：90、3:30、3分30秒、三分三十秒）")
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
        val cleaned = input.trim().replace("[-_/.。\\s\\\\:]+".toRegex(), "-").trim()

        val localDateTime = LocalDateTime.parse(cleaned, dateDataFormatter)

        return localDateTime.atZone(ZoneId.systemDefault())
    }

    private val dateDataFormatter = DateTimeFormatterBuilder()
        // 1. 年份 (必填)
        .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)

        // 2. 月份 (必填)
        .optionalStart().appendLiteral('-').optionalEnd() // 简写分隔符匹配
        .appendValue(ChronoField.MONTH_OF_YEAR)

        // 3. 日期 (可选) -> 如果省略，默认为该月 1 号
        .optionalStart()
        .optionalStart().appendLiteral('-').optionalEnd()
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