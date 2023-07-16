package com.now.nowbot.qq.onebot.event;

import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.onebot.contact.Group;
import com.now.nowbot.qq.onebot.contact.Contact;
import com.now.nowbot.qq.onebot.contact.GroupContact;

public class GroupMessageEvent extends MessageEvent implements com.now.nowbot.qq.event.GroupMessageEvent {
    com.mikuac.shiro.dto.event.message.GroupMessageEvent event;
    public GroupMessageEvent(Bot bot, com.mikuac.shiro.dto.event.message.GroupMessageEvent event) {
        super(event, bot);
        this.event = event;
    }

    @Override
    public Group getGroup() {
        return new Group(getBot().getTrueBot(), event.getGroupId());
    }

    public GroupContact getSender() {
        return new GroupContact(getBot().getTrueBot(), event.getSender().getUserId(), event.getSender().getNickname(), event.getSender().getRole(), event.getGroupId());
    }

    @Override
    public Group getSubject() {
        return new Group(getBot().getTrueBot(), event.getGroupId());
    }
}
