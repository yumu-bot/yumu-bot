package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import org.springframework.stereotype.Service;

@Service("ECHO")
public class EchoService implements MessageService<String> {
    @Override
    public boolean isHandle(MessageEvent event, DataValue<String> data) {
        if (event.getRawMessage().startsWith("#echo")) {
            data.setValue(event.getRawMessage().substring(5).trim());
            return true;
        }
        return false;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, String data) throws Throwable {
        event.getSubject().sendMessage(data);
    }
}
