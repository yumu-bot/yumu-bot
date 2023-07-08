package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Contact;

public interface MessageEvent extends Event{
    Contact getSubject();
    Contact getSender();
    String getMessage();
}
