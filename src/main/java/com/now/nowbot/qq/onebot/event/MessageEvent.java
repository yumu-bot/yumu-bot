package com.now.nowbot.qq.onebot.event;

import com.now.nowbot.qq.onebot.contact.Contact;
import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.onebot.contact.Group;
import com.now.nowbot.qq.onebot.contact.GroupContact;

public class MessageEvent extends Event implements com.now.nowbot.qq.event.MessageEvent{
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
            return new Group(getBot().getTrueBot(), Long.parseLong(e.getSender().getUserId()));
        }
        return new Contact(getBot().getTrueBot(), event.getUserId());
    }

    @Override
    public String getMessage() {
        return event.getRawMessage();
    }
}
