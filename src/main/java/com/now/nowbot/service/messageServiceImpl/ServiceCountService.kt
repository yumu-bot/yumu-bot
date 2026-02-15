package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallLite.Companion.toStatistic
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.ServiceCallRepository
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
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
import kotlin.time.DurationUnit

@Service("SERVICE_COUNT")
class ServiceCountService(
    private val legacyRepository: ServiceCallRepository,
    private val serviceCallStatisticsDao: ServiceCallStatisticsDao,
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

        if (!Permission.isSuperAdmin(event.sender.contactID)) {
            return false
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
        val md = if (param.status == ServiceCountStatus.DAILY) {
            val pair = getServiceCountListDaily(param)
            getServiceCountMarkdownDaily(param, pair.first, pair.second)
        } else {
            val services = getServiceCountList(param)
            getServiceCountMarkdown(param, services)
        }

        val image = imageService.getPanelA6(md, "service")

        event.reply(image)

        return ServiceCallStatistic.building(event)
    }

    /**
     * 整合新老两版的数据
     */
    private fun getServiceCountList(
        param: ServiceCountParam
    ): List<ServiceCallStatistic> {
        val services = mutableListOf<ServiceCallStatistic>()

        if (param.status == ServiceCountStatus.ALL) {
            return legacyRepository.getBetween(minimum, param.to).map {
                it.toStatistic()
            } + serviceCallStatisticsDao.getBetween(minimum, param.to)
        }

        if (param.from.isBefore(boundary)) {
            services.addAll(
                legacyRepository.getBetween(param.from, param.to).map {
                    it.toStatistic()
                }
            )

        }

        if (param.to.isAfter(boundary)) {
            services.addAll(serviceCallStatisticsDao.getBetween(param.from, param.to))
        }

        return services.toList()
    }

    /**
     * 获取今天和前一天的数据
     */
    private fun getServiceCountListDaily(param: ServiceCountParam): Pair<List<ServiceCallStatistic>, List<ServiceCallStatistic>> {
        val monthAgo = param.to.minus(
            java.time.Duration.ofDays(
                param.to.toLocalDate().lengthOfMonth().toLong()
            ))

        val today = ServiceCountParam(param.to.minusDays(1), param.to, param.status)
        val monthly = ServiceCountParam(monthAgo, param.to, param.status)

        return getServiceCountList(today) to getServiceCountList(monthly)
    }

    private fun getServiceCountMarkdown(param: ServiceCountParam, list: List<ServiceCallStatistic>): String {
        val sb = StringBuilder("## 时间段：${param.from.format(dateTimeFormatter)} - ${param.to.format(dateTimeFormatter)}\n")

        sb.append("""
            | # | 服务名 | 调用次数 | 平均 | 中位 | 最长 | 最短 | 中位用时占比 |
            | :-: | :-- | :-: | :-: | :-: | :-: | :-: | :-- |
            """.trimIndent()
        ).append('\n')

        val map = list
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.duration } }

        val avs = map.mapValues { (_, v) -> v.average() }
        val mxs = map.mapValues { (_, v) -> v.takePercent(0.1) ?: 0L }
        val mis = map.mapValues { (_, v) -> v.takePercent(- 0.1) ?: 0L }
        val mds = map.mapValues { (_, v) -> v.takePercent(0.5) ?: 0L }

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

                sb.append('\n')
        }

        sb.append("| 0 | 总计和平均 | ${list.size} | ${avs.map { it.value }.average().roundToSec()}s | ${mds.map { it.value }.average().roundToSec()}s | ${mxs.map { it.value }.average().roundToSec()}s | ${mis.map { it.value }.average().roundToSec()}s |")

        return sb.toString()
    }

    private fun getServiceCountMarkdownDaily(
        param: ServiceCountParam,
        list: List<ServiceCallStatistic>,
        monthlyList: List<ServiceCallStatistic>
    ): String {
        val sb = StringBuilder("## 时间段：${param.from.format(dateTimeFormatter)} - ${param.to.format(dateTimeFormatter)} (日结模式)\n")

        sb.append("""
            | # | 服务名 | 调用次数 | 平均 | 中位 | 最长 | 最短 | 中位用时占比 | DAU | DAU/MAU
            | :-: | :-- | :-: | :-: | :-: | :-: | :-: | :-- | :-: | :-: |
            """.trimIndent()
        ).append('\n')

        val map = list
            .groupBy { it.name }
            .mapValues { (_, v) -> v.map { it.duration } }

        // val x = (map.values.maxOfOrNull { it.average() }?.roundToLong() ?: 0L).coerceIn(10000L, 15000L)

        val avs = map.mapValues { (_, v) -> v.average() }
        val mxs = map.mapValues { (_, v) -> v.takePercent(0.1) ?: 0L }
        val mis = map.mapValues { (_, v) -> v.takePercent(- 0.1) ?: 0L }
        val mds = map.mapValues { (_, v) -> v.takePercent(0.5) ?: 0L }

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

        val mau = monthlyList
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

                sb.append('\n')
            }

        val dauTotal = list.map { it.userID }.toSet().size
        val mauTotal = monthlyList.map { it.userID }.toSet().size

        val stickiness = String.format("%.1f", dauTotal * 100.0 / mauTotal.coerceAtLeast(1)) + "%"

        sb.append("| 0 | 总计和平均 | ${list.size} | ${avs.map { it.value }.average().roundToSec()}s | ${mds.map { it.value }.average().roundToSec()}s | ${mxs.map { it.value }.average().roundToSec()}s | ${mis.map { it.value }.average().roundToSec()}s |  | $mauTotal | $stickiness |")

        return sb.toString()
    }

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
                sortedDescending()
            } else {
                sorted()
            }.getOrNull(count - 1)
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

                    val time = DataUtil.getTime(split[0], mode = false, unit = DurationUnit.DAYS)

                    if (time.isBefore(now)) {
                        time to now
                    } else {
                        now to time
                    }
                }

                2 -> {
                    val first = DataUtil.getTime(split[0], mode = false, unit = DurationUnit.DAYS)
                    val second = DataUtil.getTime(split[1], mode = false, unit = DurationUnit.DAYS)

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
