package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory

// 不小心写内存泄露了，又new了一块大大的、硬硬的连续内存，一直握着引用没有释放
// 断断续续露出来的内存碎片撒的到处都是...Jvm酱GC了，世界都好像停了下来...
// 但是，没有用...内存的引用，好像在别的对象手里呢...
// Jvm酱的新生代就要被填满了，Jfr里面的内存图也变得红扑扑的...真是可爱呢...
// Jvm酱又GC了...每次GC都比上一次时间更长，但GC的时间间隔却越来越短...
// 不过没有用呢...Jvm酱最终也没有逃脱坏掉的命运...看，Jvm酱OOM了呢..

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
