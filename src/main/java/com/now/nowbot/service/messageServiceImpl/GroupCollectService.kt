package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("GROUP_COLLECT")
@CheckPermission(isSuperAdmin = true)
class GroupCollectService(
    private val serviceCallStatisticsDao: ServiceCallStatisticsDao,
): MessageService<Long> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Long>
    ): Boolean {
        val matcher = Instruction.GROUP_COLLECT.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        if (!Permission.isSuperAdmin(event.sender.contactID)) {
            return false
        }

        val group = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()
            ?: matcher.group(FLAG_QQ_ID)?.toLongOrNull()
            ?: throw IllegalArgumentException.WrongException.QQ()

        data.value = group

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: Long
    ): ServiceCallStatistic? {
        val records = serviceCallStatisticsDao.getBetweenInGroup(
            groupID = param,
            from = LocalDateTime.now().minusMonths(6L),
            to = LocalDateTime.now()
        )

        if (records.isEmpty()) {
            throw NoSuchElementException.Data()
        }

        event.replyAsync(records.toResult(param))

        return null
    }

    private fun List<ServiceCallStatistic>.toResult(group: Long): String {
        val sb = StringBuilder()

        sb.append("群聊 $group 的统计数据：\n")
            .append("过去 6 个月中，调用了 ${this.size} 次\n")
            .append("\n")
            .append("最常用的功能：\n")

        val most = this.groupingBy { it.name }.eachCount().toList().sortedByDescending { it.second }.take(5)

        if (most.isNotEmpty()) {
            most.forEach { (name, count) ->
                val percent = String.format("%.2f", 100.0 * count / this.size.coerceAtLeast(1))

                sb.append("$name: $count (${percent}%)\n")
            }
        } else {
            sb.append("无\n")
        }

        sb.append("\n")

        sb.append("最活跃的星期段：\n")

        if (this.isNotEmpty()) {
            // 1. 将所有调用时间提取为 LocalDate 并排序
            val dates = this.map { it.createTime.toLocalDate() }.sorted()

            var maxCount = 0
            var bestStartDate = dates.first()
            var bestEndDate = dates.first().plusDays(6)

            // 2. 双指针滑动窗口：找出包含数据最多的 7 天区间
            var right = 0
            for (left in dates.indices) {
                val start = dates[left]
                val end = start.plusDays(6) // 包含开始当天，一共 7 天

                // 右指针向右扩展，直到超出 7 天范围
                while (right < dates.size && !dates[right].isAfter(end)) {
                    right++
                }

                // 当前窗口内的调用次数
                val currentCount = right - left
                if (currentCount > maxCount) {
                    maxCount = currentCount
                    bestStartDate = start
                    bestEndDate = end
                }
            }

            // 3. 格式化输出
            sb.append("${bestStartDate.format(formatter)} 到 ${bestEndDate.format(formatter)}: $maxCount 次\n")
        } else {
            sb.append("无\n")
        }

        sb.append("\n")

        val latest = this.maxOfOrNull { it.createTime }

        if (latest != null) {
            sb.append("最近调用：${latest.format(formatter)}\n")
        } else {
            sb.append("无\n")
        }

        sb.append("\n")

        val distinct = this.map { it.userID }.toSet().size

        sb.append("用户数量：${distinct}")

        return sb.toString()
    }

    private val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日")
}
