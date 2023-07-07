package com.now.nowbot.listener;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;


@Shiro
@Order(5)
@Component("OneBotListener")
public class OneBotListener {
    Logger log = LoggerFactory.getLogger(OneBotListener.class);

    @GroupMessageHandler(cmd = "114514")
    public void test(Bot bot, GroupMessageEvent event) {
        log.info("收到[{}]消息: {}", event.getGroupId(), event.getMessage());
        try {
            bot.sendGroupMsg(event.getGroupId(), MsgUtils.builder().img("base64://" + Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of("/home/spring/f.png")))).build(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
