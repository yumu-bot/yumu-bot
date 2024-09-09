package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;

public interface MessageEvent extends Event{
    Contact getSubject();

    Contact getSender();

    MessageChain getMessage();

    default MessageReceipt reply(MessageChain message) {
        return getSubject().sendMessage(message);
    }

    default MessageReceipt reply(String message) {
        return getSubject().sendMessage(message);
    }

    String getRawMessage();

    String getTextMessage();
}
