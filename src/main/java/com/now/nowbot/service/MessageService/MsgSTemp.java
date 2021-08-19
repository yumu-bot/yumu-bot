package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.regex.Pattern;

public abstract class MsgSTemp {
    private static ApplicationContext applicationContext;

    @Autowired
    void setApplicationContext(ApplicationContext a){
        applicationContext = a;
    }
    MsgSTemp(String name,Pattern pattern){
        /***
         * name  (?<name>[0-9a-zA-Z\[\]\-_ ]*)?
         */
        if (applicationContext != null) {
            Map<Pattern, String> services = applicationContext.getBean("MessageServices", Map.class);
            services.put(pattern,name);
        }

    }
    public static void handel(MessageEvent event) throws Throwable{
        if (applicationContext != null) {
            Map<Pattern, String> services = applicationContext.getBean("MessageServices", Map.class);
            for (var k : services.keySet()){
                var matcher = k.matcher(event.getMessage().contentToString());
                if(matcher.find()){
                    applicationContext.getBean(services.get(k), MessageService.class)
                            .HandleMessage(event, matcher);
                }
            }
        }
    }
}
