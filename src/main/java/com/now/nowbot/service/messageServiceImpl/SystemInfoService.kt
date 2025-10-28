package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory

@Service("SYS_INFO")
class SystemInfoService : MessageService<Boolean> {
    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Boolean>
    ): Boolean {
        val matcher = Instruction.SYSTEM_INFO.matcher(messageText)

        if (matcher.find()) {
            data.value = true
            return true
        }

        return false
    }

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun handleMessage(
        event: MessageEvent,
        param: Boolean
    ): ServiceCallStatistic? {
        fun Long.toMega(): Long {
            return this / 1024L / 1024L
        }

        fun Double.digit2(): String {
            if (this <= 0.0) return "未知"
            return String.format("%.2f", this * 100.0)
        }

        val sb = StringBuilder()
        INFO_MAP.forEach { (key: String?, value: String?) -> sb.append(key).append(": ").append(value).append("\n") }

        val m = ManagementFactory.getMemoryMXBean()
        val nm = m.nonHeapMemoryUsage
        val hm = m.heapMemoryUsage
        val t = ManagementFactory.getThreadMXBean()
        val o = ManagementFactory.getOperatingSystemMXBean()

        val message = """
            非堆内存：已使用 ${nm.used.toMega()} MB，已分配 ${nm.committed.toMega()} MB
            堆内存：已使用 ${hm.used.toMega()} MB，已分配 ${hm.committed.toMega()} MB，最大可用 ${hm.max.toMega()} MB
            线程：当前 ${t.threadCount} 个 (守护 ${t.daemonThreadCount}，最大 ${t.peakThreadCount})
            当前负载: ${o.systemLoadAverage.digit2()}% (每核 ${(o.systemLoadAverage / o.availableProcessors).digit2()}%)
        """.trimIndent()

        sb.append(message)

        event.reply(sb.toString())
        return ServiceCallStatistic.building(event)
    }

    companion object {
        @JvmField
        val INFO_MAP: MutableMap<String, String> = HashMap(2)
    }
}
