package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;

//@Service("MP")
public class MultiplePlayService implements MessageService<String> {
    //TODO 这个干啥的？

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<String> data) throws Throwable {
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, String data) throws Throwable {

    }
}