package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallLite.ServiceCallResult
import com.now.nowbot.mapper.ServiceCallRepository
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service("SERVICE_COUNT") class ServiceCountService(
    private val serviceCallRepository: ServiceCallRepository, private val imageService: ImageService
) : MessageService<Int> {
    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<Int>
    ): Boolean {
        val matcher = Instruction.SERVICE_COUNT.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
        }

        val d = matcher.group("days")
        val h = matcher.group("hours")

        val hours = if (d.isNullOrEmpty() && h.isNullOrEmpty()) {
            24
        } else if (d.isNullOrEmpty().not()) {
            24 * (d.toIntOrNull() ?: 0) + (h.toIntOrNull() ?: 0)
        } else {
            h.toIntOrNull() ?: 24
        }

        data.value = hours
        return true
    }

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun HandleMessage(
        event: MessageEvent, hours: Int
    ) {
        val sb = StringBuilder()
        val result: List<ServiceCallResult>?

        val now = LocalDateTime.now()
        val before: LocalDateTime

        when (hours) {
            24 -> {
                before = now.minusHours(24)
                result = serviceCallRepository.countBetween(before, now)
                sb.append("## 时间段：今天之内\n")
            }

            0 -> {
                before = LocalDateTime.of(2021, 4, 26, 0, 0, 0)
                result = serviceCallRepository.countAll()
                sb.append("## 时间段：迄今为止\n")
            }

            else -> {
                before = now.minusHours(hours.toLong())
                sb.append(
                    "## 时间段：**${before.format(dateTimeFormatter)}** - **${now.format(dateTimeFormatter)}**\n"
                )
                result = serviceCallRepository.countBetween(before, now)
            }
        }

        val r1 = serviceCallRepository.countBetweenLimit(before, now, 0.01).associate { it.service to it.data }
        val r50 = serviceCallRepository.countBetweenLimit(before, now, 0.50).associate { it.service to it.data }
        val r80 = serviceCallRepository.countBetweenLimit(before, now, 0.80).associate { it.service to it.data }
        val r99 = serviceCallRepository.countBetweenLimit(before, now, 0.99).associate { it.service to it.data }

        sb.getCharts(result, r1, r50, r80, r99)

        val image = imageService.getPanelA6(sb.toString(), "service")
        event.reply(image)
    }

    // 构建表格
    private fun StringBuilder.getCharts(
        result: List<ServiceCallResult>?,
        r1: Map<String, Long>,
        r50: Map<String, Long>,
        r80: Map<String, Long>,
        r99: Map<String, Long>
    ) {
        if (result == null) return

        this.append(
            """
                | 服务名 | 调用次数 | 最长用时 (99%) | 大部分人用时 (80%) | 平均用时 (50%) | 最短用时 (1%) |
                | :-- | :-: | :-: | :-: | :-: | :-: |
                
                """.trimIndent()
        )

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
                .append(roundToSec(r99.getOrDefault(service, 0L))).append('s').append(" | ")
                .append(roundToSec(r80.getOrDefault(service, 0L))).append('s').append(" | ")
                .append(roundToSec(r50.getOrDefault(service, 0L))).append('s').append(" | ")
                .append(roundToSec(r1.getOrDefault(service, 0L))).append('s').append(" |\n")
        }

        this.append("| ").append("总计和平均").append(" | ").append(count).append(" | ")
            .append(roundToSec(getListAverage(r99List, count))).append('s').append(" | ")
            .append(roundToSec(getListAverage(r80List, count))).append('s').append(" | ")
            .append(roundToSec(getListAverage(r50List, count))).append('s').append(" | ")
            .append(roundToSec(getListAverage(r1List, count))).append('s').append(" |\n")
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
    private fun <T : Number?> roundToSec(@Nullable millis: T?): String {
        if (millis == null) return "0"

        val str = String.format("%.1f", Math.round(millis.toFloat() / 100f) / 10f)

        if (str.endsWith(".0")) {
            return str.replace(".0", "")
        }

        return str
    }

    companion object {
        private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm")
    }
}
