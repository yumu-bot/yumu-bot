package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("ban")
public class BanService implements MessageService{

    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

    }
}
