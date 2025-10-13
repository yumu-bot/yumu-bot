package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallLite.ServiceCallResult
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.ServiceCallRepository
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_TIME
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.math.roundToInt

@Service("SERVICE_COUNT")
class ServiceCountService(
    private val legacyRepository: ServiceCallRepository,
    private val dao: ServiceCallStatisticsDao,
    private val imageService: ImageService
) : MessageService<ServiceCountService.ServiceCountParam> {

    data class ServiceCountParam(
        val from: LocalDateTime,
        val to: LocalDateTime,
        val status: ServiceCountStatus,
    )

    enum class ServiceCountStatus {
         DAILY, DURATION, ALL
    }

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<ServiceCountParam>
    ): Boolean {
        val matcher = Instruction.SERVICE_COUNT.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        val range = getTimeRange(matcher.group(FLAG_TIME))

        val between = java.time.Duration.between(range.first, range.second)

        val status = if (between.toMinutes().absoluteValue <= 1) {
            // 0 分之内，视作获取全部
            ServiceCountStatus.ALL
        } else if (between.toMinutes().absoluteValue <= 24 * 60 + 5) {
            // 1 天 5 分之内
            ServiceCountStatus.DAILY
        } else {
            // 常规
            ServiceCountStatus.DURATION
        }

        data.value = ServiceCountParam(
            from = range.first,
            to = range.second,
            status = status,
        )

        return true
    }

    @CheckPermission(isSuperAdmin = true)
    override fun handleMessage(
        event: MessageEvent, param: ServiceCountParam
    ): ServiceCallStatistic? {

        if (param.from.isBefore(boundary) || param.status == ServiceCountStatus.ALL) {
            val md = getLegacyServiceCountMarkdown(param)

            val image = imageService.getPanelA6(md, "service")

            event.reply(image)
        }

        if (param.to.isAfter(boundary) || param.status == ServiceCountStatus.ALL) {
            val md = if (param.status == ServiceCountStatus.DAILY) {
                getServiceCountMarkdownDaily(param)
            } else {
                getServiceCountMarkdown(param)
            }

            val image = imageService.getPanelA6(md, "service")

            event.reply(image)
        }


        return ServiceCallStatistic.building(event)
    }

    private fun getServiceCountMarkdown(param: ServiceCountParam): String {
        val sb = StringBuilder("## 时间段：${param.from.format(dateTimeFormatter)} - ${param.to.format(dateTimeFormatter)}\n")

        sb.append("""
            | # | 服务名 | 调用次数 | 平均 | 中位 | 最长 | 最短 | 中位用时占比 |
            | :-: | :-- | :-: | :-: | :-: | :-: | :-: | :-- |
            """.trimIndent()
        ).append('\n')

        val list = if (param.status == ServiceCountStatus.ALL) {
            dao.getBetween(minimum, param.to)
        } else {
            dao.getBetween(param.from, param.to)
        }

        val map = list
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.duration } }

        // val x = (map.values.maxOfOrNull { it.average() }?.roundToLong() ?: 0L).coerceIn(10000L, 15000L)

        val avs = map.mapValues { (_, v) -> v.average() }
        val mxs = map.mapValues { (_, v) -> v.takePercent(0.1) }
        val mis = map.mapValues { (_, v) -> v.takePercent(- 0.1) }
        val mds = map.mapValues { (_, v) -> v.takePercent(0.5) }

        fun getMarkdownLine(index: Int, name: String, times: List<Long>, maxAverage: Long = 10000L): String {
            val av = avs[name] ?: 0.0
            val mx = mxs[name] ?: 0L
            val mi = mis[name] ?: 0L
            val md = mds[name] ?: 0L

            val percent = (md * 1.0 / maxAverage)

            val block = when(percent) {
                in 0.0..1.0 / 3 -> "\uD83D\uDFE9" // 绿色方块
                in 1.0 / 3..2.0 / 3 -> "\uD83D\uDFE8" // 黄色方块
                in 2.0 / 3..1.0 -> "\uD83D\uDFE7" // 橙色方块
                else -> "\uD83D\uDFE5" // 红色方块
            }

            val blocks = block.repeat((percent.coerceIn(0.0, 1.5) * 10).roundToInt())

            return "| $index | $name | ${times.size} | ${av.roundToSec()}s | ${md.roundToSec()}s | ${mx.roundToSec()}s | ${mi.roundToSec()}s | $blocks |"
        }

        map.toList()
            .sortedByDescending {
                (_, v) -> v.size
            }
            .mapIndexed { i, (k, v) ->
                sb.append(getMarkdownLine(i + 1 , k, v))

                if (i != map.size - 1) {
                    sb.append('\n')
                }
        }

        return sb.toString()
    }

    private fun getServiceCountMarkdownDaily(param: ServiceCountParam): String {
        val sb = StringBuilder("## 时间段：${param.from.format(dateTimeFormatter)} - ${param.to.format(dateTimeFormatter)} (日结模式)\n")

        sb.append("""
            | # | 服务名 | 调用次数 | 平均 | 中位 | 最长 | 最短 | 中位用时占比 | DAU | DAU/MAU
            | :-: | :-- | :-: | :-: | :-: | :-: | :-: | :-- | :-: | :-: |
            """.trimIndent()
        ).append('\n')

        val list = dao.getBetween(param.from, param.to)
        val beforeList = dao.getBetween(
            param.from
                .minus(java.time.Duration.ofDays(
                    param.to.toLocalDate().lengthOfMonth().toLong()
                )),
            param.to
        )

        val map = list
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.duration } }

        // val x = (map.values.maxOfOrNull { it.average() }?.roundToLong() ?: 0L).coerceIn(10000L, 15000L)

        val avs = map.mapValues { (_, v) -> v.average() }
        val mxs = map.mapValues { (_, v) -> v.takePercent(0.1) }
        val mis = map.mapValues { (_, v) -> v.takePercent(- 0.1) }
        val mds = map.mapValues { (_, v) -> v.takePercent(0.5) }

        fun getMarkdownLineWithDAU(index: Int, name: String, times: List<Long>, dau: Int? = 0, mau: Int? = 0, maxAverage: Long = 10000L): String {
            val av = avs[name] ?: 0.0
            val mx = mxs[name] ?: 0L
            val mi = mis[name] ?: 0L
            val md = mds[name] ?: 0L

            val percent = (md * 1.0 / maxAverage)

            val block = when(percent) {
                in 0.0..1.0 / 3 -> "\uD83D\uDFE9" // 绿色方块
                in 1.0 / 3..2.0 / 3 -> "\uD83D\uDFE8" // 黄色方块
                in 2.0 / 3..1.0 -> "\uD83D\uDFE7" // 橙色方块
                else -> "\uD83D\uDFE5" // 红色方块
            }

            val blocks = block.repeat((percent.coerceIn(0.0, 1.5) * 10).roundToInt())

            val stickiness = String.format("%.1f", (dau ?: 0) * 100.0 / (mau ?: 1)) + "%"

            return "| $index | $name | ${times.size} | ${av.roundToSec()}s | ${md.roundToSec()}s | ${mx.roundToSec()}s | ${mi.roundToSec()}s | $blocks | ${dau ?: 0} | $stickiness |"
        }

        val mau = beforeList
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.userID }.toSet().size }

        val dau = list
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.userID }.toSet().size }

        list.groupBy { it.name }
            .toList()
            .sortedByDescending {
                    (_, v) -> v.size
            }
            .mapIndexed { i, (k, v) ->
                sb.append(getMarkdownLineWithDAU(i + 1 , k, v.map { it.duration }, dau[k], mau[k]))

                if (i != map.size - 1) {
                    sb.append('\n')
                }
            }

        return sb.toString()
    }


    private fun getLegacyServiceCountMarkdown(param: ServiceCountParam): String {
        val sb = StringBuilder()
        val result: List<ServiceCallResult>?

        val now = LocalDateTime.now()

        val from: LocalDateTime
        val to: LocalDateTime = param.to

        when (param.status) {
            ServiceCountStatus.ALL -> {
                from = minimum
                sb.append("## 时间段：迄今为止\n")
                result = legacyRepository.countAll()
            }

            ServiceCountStatus.DAILY -> {
                from = param.from
                result = legacyRepository.countBetween(from, to)
                sb.append("## 时间段：今天之内\n")
            }

            else -> {
                from = param.from
                sb.append(
                    "## 时间段：**${from.format(dateTimeFormatter)}** - **${now.format(dateTimeFormatter)}**\n"
                )
                result = legacyRepository.countBetween(from, now)
            }
        }

        val r1 = legacyRepository.countBetweenLimit(from, to, 0.01).associate { it.service to it.data }
        val r50 = legacyRepository.countBetweenLimit(from, to, 0.50).associate { it.service to it.data }
        val r80 = legacyRepository.countBetweenLimit(from, to, 0.80).associate { it.service to it.data }
        val r99 = legacyRepository.countBetweenLimit(from, to, 0.99).associate { it.service to it.data }

        sb.getCharts(result, r1, r50, r80, r99)

        return sb.toString()
    }

    // 构建表格
    private fun StringBuilder.getCharts(
        result: List<ServiceCallResult>?,
        r1: Map<String, Long>,
        r50: Map<String, Long>,
        r80: Map<String, Long>,
        r99: Map<String, Long>
    ) {
        if (result.isNullOrEmpty()) return

        this.append(
            """
                | 服务名 | 调用次数 | 最长用时 (99%) | 大部分人用时 (80%) | 平均用时 (50%) | 最短用时 (1%) |
                | :-- | :-: | :-: | :-: | :-: | :-: |
                """.trimIndent()
        ).append('\n')

        var count = 0
        val r99List = ArrayList<Long>()
        val r80List = ArrayList<Long>()
        val r50List = ArrayList<Long>()
        val r1List = ArrayList<Long>()

        for (r in result) {
            val service = r.service
            val size = r.size

            count += size

            r99List.add(r99.getOrDefault(service, 0L) * size)
            r80List.add(r80.getOrDefault(service, 0L) * size)
            r50List.add(r50.getOrDefault(service, 0L) * size)
            r1List.add(r1.getOrDefault(service, 0L) * size)

            this.append("| ").append(service).append(" | ").append(size).append(" | ")
                .append(r99.getOrDefault(service, 0L).roundToSec()).append('s').append(" | ")
                .append(r80.getOrDefault(service, 0L).roundToSec()).append('s').append(" | ")
                .append(r50.getOrDefault(service, 0L).roundToSec()).append('s').append(" | ")
                .append(r1.getOrDefault(service, 0L).roundToSec()).append('s').append(" |\n")
        }

        this.append("| ").append("总计和平均").append(" | ").append(count).append(" | ")
            .append(getListAverage(r99List, count).roundToSec()).append('s').append(" | ")
            .append(getListAverage(r80List, count).roundToSec()).append('s').append(" | ")
            .append(getListAverage(r50List, count).roundToSec()).append('s').append(" | ")
            .append(getListAverage(r1List, count).roundToSec()).append('s').append(" |\n")
    }

    //数组求平均值
    private fun getListAverage(list: List<Long>?, count: Int): Float {
        return if (list.isNullOrEmpty() || count == 0) {
            0f
        } else {
            list.sum() * 1f / count
        }
    }

    //1926ms -> 1.9s

    companion object {
        private val boundary = LocalDateTime.of(2025, 10, 9, 8, 0, 0)
        private val minimum = LocalDateTime.of(2021, 4, 26, 0, 0, 0)

        private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm")

        private fun <T : Number> T.roundToSec(): String {
            val str = String.format("%.1f", round(this.toFloat() / 100f) / 10f)

            return str.removeSuffix(".0")
        }


        /*

        /**
         * 筛选前 ..% 的数据，如果是负数，则是后 ..% 的数据
         * @param percent 0-1
         */
        fun <T : Comparable<T>> List<T>.takeTopPercentWithSet(percent: Double): List<T> {
            if (isEmpty()) return emptyList()

            val count = (size * percent.absoluteValue).toInt().coerceAtLeast(1)
            val topValues = sortedDescending().take(count).toSet()

            return if (percent > 0) {
                filter { it in topValues }
            } else {
                filter { it !in topValues }
            }
        }

         */

        /**
         * 找出前 ..% 的数据，如果是负数，则是后 ..% 的数据
         * @param percent 0-1
         */
        fun <T : Comparable<T>> List<T>.takePercent(percent: Double): T? {
            if (isEmpty()) return null

            val count = (size * percent.absoluteValue).toInt().coerceAtLeast(1)
            return if (percent >= 0.0) {
                sortedDescending().getOrNull(count - 1)
            } else {
                sorted().getOrNull(count - 1)
            }
        }


        private fun getTimeRange(input: String?): Pair<LocalDateTime, LocalDateTime> {
            val now = LocalDateTime.now()

            if (input.isNullOrEmpty()) {
                return now.minusDays(1) to now
            }

            val split = input.split(REG_HYPHEN.toRegex())
                .dropWhile { it.isEmpty() }

            return when (split.size) {
                1 -> {

                    val time = DataUtil.getTime(DataUtil.parseTime(split[0]))

                    if (time.isBefore(now)) {
                        time to now
                    } else {
                        now to time
                    }
                }

                2 -> {
                    val first = DataUtil.getTime(DataUtil.parseTime(split[0]))
                    val second = DataUtil.getTime(DataUtil.parseTime(split[1]))

                    if (first.isBefore(second)) {
                        first to second
                    } else {
                        second to first
                    }
                }

                else -> now.minusDays(1) to now
            }
        }
    }
}
