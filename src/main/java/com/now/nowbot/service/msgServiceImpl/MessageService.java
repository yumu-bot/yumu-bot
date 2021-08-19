package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.service.messageService;
import net.mamoe.mirai.event.events.*;

import java.util.HashMap;

public abstract class MessageService implements messageService {
    protected String key;
    public static HashMap<String, MessageService> servicesName = new HashMap<>();
    public MessageService(String key){
        this.key = key;
        MessageService.servicesName.put(key, this);
    }
    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void handleMsg(GroupMessageEvent event) {
        handleMsg((MessageEvent) event);
    }

    @Override
    public void handleMsg(FriendMessageEvent event) {
        handleMsg((MessageEvent) event);
    }

    @Override
    public void handleMsg(GroupTempMessageEvent event) {
        handleMsg((MessageEvent) event);
    }

    @Override
    public void handleMsg(StrangerMessageEvent event) {
        handleMsg((MessageEvent) event);
    }

    @Override
    public void handleMsg(MessageEvent event) {

    }
    @Override
    public void handleMsg(GroupMessageEvent event, String[] page) {
        handleMsg((MessageEvent) event, page);
        handleMsg(event);
    }

    @Override
    public void handleMsg(FriendMessageEvent event, String[] page) {
        handleMsg((MessageEvent) event, page);
        handleMsg(event);
    }

    @Override
    public void handleMsg(GroupTempMessageEvent event, String[] page) {
        handleMsg((MessageEvent) event, page);
        handleMsg(event);
    }

    @Override
    public void handleMsg(StrangerMessageEvent event, String[] page) {
        handleMsg((MessageEvent) event, page);
        handleMsg(event);
    }

    @Override
    public void handleMsg(MessageEvent event, String[] page) {

    }
}
