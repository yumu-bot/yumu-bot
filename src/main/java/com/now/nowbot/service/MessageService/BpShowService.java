package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("bp-show")
public class BpShowService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var m = Integer.parseInt(matcher.group("n"));
        event.getSubject().sendMessage(new At(event.getSender().getId()).plus("来了"));
    }
}
