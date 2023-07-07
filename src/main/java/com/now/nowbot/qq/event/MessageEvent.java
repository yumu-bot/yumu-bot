package com.now.nowbot.qq.event;

import com.now.nowbot.qq.Bot;
import com.now.nowbot.qq.contact.User;

public interface MessageEvent extends Event{
    User getSubject();
    User getSender();
}
