package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("help")
public class helpService extends MsgSTemp implements MessageService {
    public helpService(){
        super(Pattern.compile("[!！](?i)help"),"help");
    }

    @Autowired
    ApplicationContext applicationContext;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        StringBuffer sb = new StringBuffer();
        MsgSTemp.services.forEach((p,v)->{
            var s = applicationContext.getBean(v,MsgSTemp.class).getInfo();
            if (s != null){
                sb.append(s).append('\n');
            }
        });

        from.sendMessage(sb.toString());
    }
}
