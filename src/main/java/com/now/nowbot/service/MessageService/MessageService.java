package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;

import java.util.regex.Matcher;

public interface MessageService {
    void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable;
}
