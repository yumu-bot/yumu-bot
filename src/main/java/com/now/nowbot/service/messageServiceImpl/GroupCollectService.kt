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
import java.time.format.TextStyle
import java.util.Locale

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

        val most = this.groupingBy { it.name }.eachCount().toList().sortedByDescending { it.first }.take(5)

        if (most.isNotEmpty()) {
            most.forEach { (name, count) ->
                val percent = String.format("%.2f", 100.0 * count / this.size.coerceAtLeast(1))

                sb.append("$name: $count (${percent}%)\n")
            }
        } else {
            sb.append("无\n")
        }

        sb.append("\n")

        val dayOfWeekCount = this.groupingBy { it.createTime.dayOfWeek }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }.take(5)

        sb.append("最活跃的星期：\n")

        if (dayOfWeekCount.isNotEmpty()) {
            dayOfWeekCount.forEach { (day, count) ->
                val dayName = day.getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE)
                sb.append("$dayName: $count 次\n")
            }
        } else {
            sb.append("无\n")
        }

        sb.append("\n")

        val distinct = this.map { it.userID }.toSet().size

        sb.append("用户数量：${distinct}")

        return sb.toString()
    }

}