package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("kumo")
public class kumoService extends MsgSTemp implements MessageService{
    kumoService(){
        super(Pattern.compile(".*"),"kumo");
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

    }
}
