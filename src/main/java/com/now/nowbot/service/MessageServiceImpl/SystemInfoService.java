package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instruction;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@Service("SYS_INFO")
public class SystemInfoService implements MessageService<Boolean> {
    public static final Map<String, String> INFO_MAP = new HashMap<>(2);

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Boolean> data) throws Throwable {
        var matcher = Instruction.SYSTEM_INFO.matcher(messageText);

        return matcher.find();
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Boolean data) throws Throwable {
        StringBuilder sb = new StringBuilder();
        INFO_MAP.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));

        var m = ManagementFactory.getMemoryMXBean();
        var t = ManagementFactory.getThreadMXBean();

        sb.append("已使用堆内存: ").append(m.getHeapMemoryUsage().getUsed() / 1024 / 1024).append(" MB\n");
        sb.append("当前线程数: ").append(t.getThreadCount());

        event.getSubject().sendMessage(sb.toString());
    }
}
