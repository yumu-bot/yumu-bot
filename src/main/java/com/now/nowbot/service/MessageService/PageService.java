package com.now.nowbot.service.MessageService;

import com.now.nowbot.throwable.TipsRuntimeException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("page")
public class PageService implements MessageService{
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String p1 = matcher.group("p1");
        String p2 = matcher.group("p2");
        String p3 = matcher.group("p3");
        //todo 预留 为设置指令
        throw new TipsRuntimeException("what's up");
    }
}
