package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import org.jetbrains.annotations.NotNull;

//@Service("MP")
public class MultiplePlayService implements MessageService<String> {
    //TODO 这个干啥的？

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<String> data) throws Throwable {
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, String data) throws Throwable {

    }
}
