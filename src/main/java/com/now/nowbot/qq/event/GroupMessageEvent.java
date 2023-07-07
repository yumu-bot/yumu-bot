package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.User;

public interface GroupMessageEvent extends MessageEvent{
    Group getSubject();
    User getSender();
}
