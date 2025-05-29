package com.now.nowbot.listener;

import com.now.nowbot.permission.PermissionImplement;
import com.now.nowbot.qq.local.Bot;
import com.now.nowbot.qq.local.Event;
import com.now.nowbot.qq.local.contact.LocalGroup;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
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
                String finalCmd = cmd;
                Thread.startVirtualThread(() -> listener.onMessage(finalCmd));
            }
        });
    }

    void onMessage(String message) {
        var group = new LocalGroup();
        var event = new Event.GroupMessageEvent(bot, group, message);
        try {
            PermissionImplement.onMessage(event, ((_, throwable) -> {
                if (throwable instanceof TipsException) {
                    log.info("捕捉到提示：{}", throwable.getMessage());
                    log.debug("异常详细信息: ", throwable);
                } else {
                    log.info("捕捉到异常：", throwable);
                }
            }));

        } catch (Exception e) {
            log.info("捕捉到未知异常：{}", e.getMessage());
            log.debug("异常详细信息:", e);
        }

        try {
            PermissionImplement.onTencentMessage(event, (event::reply));
        } catch (Exception e) {
            log.info("捕捉到腾讯异常：{}", e.getMessage());
            log.debug("异常详细信息:", e);
        }
    }
}
