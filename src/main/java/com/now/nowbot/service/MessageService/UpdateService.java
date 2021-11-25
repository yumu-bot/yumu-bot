package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("update")
public class UpdateService implements MessageService{
    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (Permission.isSupper(event.getSender().getId())){
            event.getSubject().sendMessage("即将更新重启...");
            Runtime.getRuntime().exec("/root/update.sh");
            System.exit(1);
        }
    }
}
