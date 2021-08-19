package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class MsgSTemp {
    private static final Map<Pattern, MessageService> services = new HashMap<>();

    MsgSTemp(Pattern pattern){
        /***
         * name  (?<name>[0-9a-zA-Z\[\]\-_ ]*)?
         */
        services.put(pattern,(MessageService)this);
    }
    public static void handel(MessageEvent event) throws Throwable{

            for (var k : services.keySet()){
                var matcher = k.matcher(event.getMessage().contentToString());
                if(matcher.find()){
                    services.get(k)
                            .HandleMessage(event, matcher);
                }
            }
    }
}
