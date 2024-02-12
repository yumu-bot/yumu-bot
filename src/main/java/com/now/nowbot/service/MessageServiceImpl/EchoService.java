package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instructions;
import org.springframework.stereotype.Service;

@Service("ECHO")
public class EchoService implements MessageService<String> {
    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<String> data) throws Throwable {
        var m = Instructions.ECHO.matcher(messageText);

        if (!Permission.isSuperAdmin(event.getSender().getId())) {
            throw new TipsException("只有超级管理员 (OP，原批) 可以使用此功能！");
        }
        if (m.find()) {
            data.setValue(m.group("any"));
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
