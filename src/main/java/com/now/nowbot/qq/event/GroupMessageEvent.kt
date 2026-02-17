package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Contact;

public interface GroupMessageEvent extends MessageEvent{
    Group getSubject();
    Contact getSender();

    default Group getGroup() {
        return getSubject();
    }
}
