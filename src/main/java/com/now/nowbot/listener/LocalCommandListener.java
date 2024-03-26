package com.now.nowbot.listener;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.local.Bot;
import com.now.nowbot.qq.local.Event;
import com.now.nowbot.qq.local.contact.LocalGroup;
import com.now.nowbot.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Scanner;

public class LocalCommandListener {
    static final   Logger                      log = LoggerFactory.getLogger(LocalCommandListener.class);
    private static Map<String, MessageService> handler;
    private final  Bot                         bot;

    public LocalCommandListener() {
        bot = new Bot();
    }

    public static void setHandler(Map<String, MessageService> handler) {
        LocalCommandListener.handler = handler;
    }

    public static void startListener() {
        var listener = new LocalCommandListener();
        Thread.startVirtualThread(() -> {
            Scanner sc = new Scanner(System.in);
            String cmd;
            while (StringUtils.hasText((cmd = sc.nextLine()))) {
                listener.onMessage(cmd);
            }
        });
    }

    void onMessage(String message) {
        var group = new LocalGroup();
        var event = new Event.GroupMessageEvent(bot, group, message);
        handler.forEach((name, action) -> doAction(event, name, action));
    }

    public void doAction(MessageEvent event, String serviceName, MessageService service) {
        try {
            var data = new MessageService.DataValue();
            boolean ihandle = service.isHandle(event, event.getTextMessage(), data);
            if (ihandle) {
                log.debug("{} 被本地调用", serviceName);
                service.HandleMessage(event, data.getValue());
            }
        } catch (Throwable e) {
            log.info("bot err: {}", e.getMessage());
            log.debug("详细信息: ", e);
        }
    }
}
