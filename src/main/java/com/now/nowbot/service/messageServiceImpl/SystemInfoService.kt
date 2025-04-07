package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
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

        return matcher.find()
    }

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun HandleMessage(
        event: MessageEvent,
        data: Boolean
    ) {
        val sb = StringBuilder()
        INFO_MAP.forEach { (key: String?, value: String?) -> sb.append(key).append(": ").append(value).append("\n") }

        val m = ManagementFactory.getMemoryMXBean()
        val t = ManagementFactory.getThreadMXBean()

        sb.append("已使用堆内存: ").append(m.heapMemoryUsage.used / 1024 / 1024).append(" MB\n")
        sb.append("当前线程数: ").append(t.threadCount)

        event.subject.sendMessage(sb.toString())
    }

    companion object {
        val INFO_MAP: Map<String, String> = HashMap(2)
    }
}
