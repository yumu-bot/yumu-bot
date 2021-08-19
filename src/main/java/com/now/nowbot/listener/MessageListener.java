package com.now.nowbot.listener;

import com.now.nowbot.service.MessageService.MsgSTemp;
import com.now.nowbot.service.msgServiceImpl.MessageService;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public class MessageListener extends SimpleListenerHost {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception){
        log.error(context.toString(),exception);
    }

    @Async
    @EventHandler
    public void msg(MessageEvent event) throws Throwable{
        MsgSTemp.handel(event);
    }
    @Async
    @EventHandler
    public void msg(MessagePreSendEvent event){
        System.out.println(event.getMessage().contentToString());
    }
    @Async
    @EventHandler
    public void msg(GroupMessageEvent event){
        MessageChain data = event.getMessage();
        if ( data.size() >= 2 ){
            String[] page = data.get(1).contentToString().trim().split("\\s+");
            if(MessageService.servicesName.containsKey(page[0].toLowerCase())) {
                log.info(event.getGroup().getName()+":"+page[0]);
                MessageService.servicesName.get(page[0].toLowerCase()).handleMsg(event, page);
            }
        }
    }
    @Async
    @EventHandler
    public void msg(FriendMessageEvent event){
        MessageChain data = event.getMessage();
        if ( data.size() >= 2 ){
            String[] page = data.get(1).contentToString().trim().split("\\s+");
            if(MessageService.servicesName.containsKey(page[0].toLowerCase())) {
                log.info(page[0].toLowerCase());
                MessageService.servicesName.get(page[0].toLowerCase()).handleMsg(event, page);
            }
        }
    }
    @Async
    @EventHandler
    public void msg(StrangerMessageEvent event){
        MessageChain data = event.getMessage();
        if ( data.size() >= 2 ){
            String[] page = data.get(1).contentToString().trim().split("\\s+");
            if(MessageService.servicesName.containsKey(page[0].toLowerCase())) {
                log.info(page[0].toLowerCase());
                MessageService.servicesName.get(page[0].toLowerCase()).handleMsg(event, page);
            }
        }
    }
    @Async
    @EventHandler
    public void msg(GroupTempMessageEvent event){
        MessageChain data = event.getMessage();
        if ( data.size() >= 2 ){
            String[] page = data.get(1).contentToString().trim().split("\\s+");
            if(MessageService.servicesName.containsKey(page[0].toLowerCase())) {
                log.info(page[0].toLowerCase());
                MessageService.servicesName.get(page[0].toLowerCase()).handleMsg(event, page);
            }
        }
    }
    /***
     * ImageUploadEvent 图片上传事件
     */
    @Async
    @EventHandler
    public void msg(ImageUploadEvent event){
        if(event instanceof ImageUploadEvent.Failed){
            log.info("图片上传失败");
        }
        if(event instanceof ImageUploadEvent.Succeed){
            log.info("图片上传成功");
        }
    }
    @Async
    @EventListener(BotInvitedJoinGroupRequestEvent.class)
    public void joinGroup(BotInvitedJoinGroupRequestEvent e){
        e.getBot().getFriend(365246692L).sendMessage("被邀请了").recallIn(100*1000);
        e.isIntercepted();
    }
}
