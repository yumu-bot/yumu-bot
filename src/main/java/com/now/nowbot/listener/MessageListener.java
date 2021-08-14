package com.now.nowbot.listener;

import com.now.nowbot.service.msgServiceImpl.MessageService;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.MessageChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    @Async
    @EventListener(GroupMessageEvent.class)
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
    @EventListener(FriendMessageEvent.class)
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
    @EventListener(StrangerMessageEvent.class)
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
    @EventListener(GroupTempMessageEvent.class)
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
    @EventListener(ImageUploadEvent.class)
    public void msg(ImageUploadEvent event){
        if(event instanceof ImageUploadEvent.Failed){
            log.info("图片上传失败");
        }
        if(event instanceof ImageUploadEvent.Succeed){
            log.info("图片上传成功");
        }
    }
}
