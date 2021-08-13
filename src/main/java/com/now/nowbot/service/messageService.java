package com.now.nowbot.service;

import net.mamoe.mirai.event.events.*;

public interface messageService {
    public String getKey();
    public void setKey(String key);
    public void handleMsg(GroupMessageEvent event);
    public void handleMsg(FriendMessageEvent event);
    public void handleMsg(GroupTempMessageEvent event);
    public void handleMsg(StrangerMessageEvent event);
    public void handleMsg(MessageEvent event);
    public void handleMsg(GroupMessageEvent event, String[] page);
    public void handleMsg(FriendMessageEvent event, String[] page);
    public void handleMsg(GroupTempMessageEvent event, String[] page);
    public void handleMsg(StrangerMessageEvent event, String[] page);
    public void handleMsg(MessageEvent event, String[] page);
}
