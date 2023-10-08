package com.now.nowbot.listener;

import com.now.nowbot.service.MessageService;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.events.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface MiraiListener {
    void init(Map<String, MessageService> beanMap) throws BeansException;

    void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception);

    @Async
    @EventHandler
    void msg(MessageEvent event) throws Throwable;

    @Async
    @EventHandler
    void msg(BotInvitedJoinGroupRequestEvent event) throws Exception;

    @Async
    @EventHandler
    void msg(BotJoinGroupEvent event);

    @Async
    @EventHandler
    void msg(NewFriendRequestEvent event) throws Exception;

    @Async
    @EventHandler
    void msg(MessagePreSendEvent event) throws Exception;

    @Async
    @EventHandler
    void msg(MessagePostSendEvent event);

    @Async
    @EventHandler
    void msg(ImageUploadEvent event);
}
