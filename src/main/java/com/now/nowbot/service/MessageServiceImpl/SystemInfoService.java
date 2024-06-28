package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@Service("SYS_INFO")
public class SystemInfoService implements MessageService<Boolean> {
    public static final Map<String, String> INFO_MAP = new HashMap<>(2);

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Boolean> data) throws Throwable {
        return messageText.equals("!sys");
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Boolean data) throws Throwable {
        StringBuilder sb = new StringBuilder();
        INFO_MAP.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));

        var m = ManagementFactory.getMemoryMXBean();
        var t = ManagementFactory.getThreadMXBean();

        sb.append("HeapMemory 已使用: ").append(m.getHeapMemoryUsage().getUsed() / 1024 / 1024)
                .append("M\n");
        sb.append("当前线程数: ").append(t.getThreadCount());

        event.getSubject().sendMessage(sb.toString());
    }
}
