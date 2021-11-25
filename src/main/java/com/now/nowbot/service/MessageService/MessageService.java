package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;

import java.util.regex.Matcher;

public interface MessageService {
    void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable;
}
