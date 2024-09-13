package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;

import java.net.URL;
import java.util.List;

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

    default MessageReceipt reply(Exception e) {
        return getSubject().sendMessage(e.getMessage());
    }

    default MessageReceipt reply(byte[] image) {
        return getSubject().sendImage(image);
    }
    default MessageReceipt reply(byte[] image, String message) {
        return getSubject().sendMessage(new MessageChain.MessageChainBuilder().addImage(image).addText(message).build());
    }

    default MessageReceipt reply(URL url) {
        return getSubject().sendImage(url);
    }

    default MessageReceipt sendVoice(byte[] voice) {
        return getSubject().sendVoice(voice);
    }

    String getRawMessage();

    String getTextMessage();
}
