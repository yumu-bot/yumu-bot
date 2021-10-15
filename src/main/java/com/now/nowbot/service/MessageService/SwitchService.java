package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("switch")
public class SwitchService implements MessageService{
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        String p1 = matcher.group("p1");
        String p2 = matcher.group("p2");
        String p3 = matcher.group("p3");
        String p4 = matcher.group("p4");

        if (event instanceof GroupMessageEvent){
            var group = ((GroupMessageEvent) event);
            group.getSender().mute(60*60*8);
        }
        if (p1 == null) {
            from.sendMessage("");
            return;
        }
        switch (p1.toLowerCase()){
            case "":{

            }break;
        }
    }
}
