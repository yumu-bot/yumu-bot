package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.message.MessageChain;

public interface MessageEvent extends Event{
    Contact getSubject();
    Contact getSender();
    MessageChain getMessage();

    String getRawMessage();

    String getTextMessage();
}
