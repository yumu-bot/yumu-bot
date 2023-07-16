package com.now.nowbot.qq.onebot.event;

import com.mikuac.shiro.model.ArrayMsg;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.contact.Contact;
import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.onebot.contact.Group;
import com.now.nowbot.qq.onebot.contact.GroupContact;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MessageEvent extends Event implements com.now.nowbot.qq.event.MessageEvent {
    com.mikuac.shiro.dto.event.message.MessageEvent event;

    public MessageEvent(com.mikuac.shiro.dto.event.message.MessageEvent event, Bot bot) {
        super(bot);
        this.event = event;
    }

    @Override
    public Contact getSubject() {
        if (event instanceof com.mikuac.shiro.dto.event.message.GroupMessageEvent e) {
            return new Group(getBot().getTrueBot(), e.getGroupId());
        }
        return new Contact(getBot().getTrueBot(), event.getUserId());
    }

    @Override
    public Contact getSender() {
        if (event instanceof com.mikuac.shiro.dto.event.message.GroupMessageEvent e) {
            return new Group(getBot().getTrueBot(), e.getSender().getUserId());
        }
        return new Contact(getBot().getTrueBot(), event.getUserId());
    }

    @Override
    public MessageChain getMessage() {
        return getMessageChain(event.getArrayMsg());
    }

    public String getRawMessage() {
        return event.getRawMessage();
    }

    public static MessageChain getMessageChain(List<ArrayMsg> msgs) {
        var msg = msgs.stream().map(arrayMsg -> {
            Message m;
            switch (arrayMsg.getType()) {
                case at -> m = new AtMessage(Long.parseLong(arrayMsg.getData().getOrDefault("qq", "0")));
                case text -> m = new TextMessage(arrayMsg.getData().getOrDefault("text", ""));
                case reply -> m = new ReplayMessage(Integer.parseInt(arrayMsg.getData().getOrDefault("id", "0")),
                        arrayMsg.getData().getOrDefault("text", ""));
                case image -> {
                    try {
                        m = new ImageMessage(new URL(arrayMsg.getData().getOrDefault("url", "")));
                    } catch (MalformedURLException e) {
                        m = new TextMessage("[图片;加载异常]");
                    }
                }
                default -> m = new TextMessage(String.format("[%s;不支持的操作类型]", arrayMsg.getType().toString()));
            }
            return m;
        }).toList();
        return new MessageChain(msg);
    }
}
