package com.now.nowbot.qq.event;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.message.*;
import org.springframework.lang.Nullable;

import java.net.URL;

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

    default MessageReceipt replyVoice(byte[] voice) {
        return getSubject().sendVoice(voice);
    }

    String getRawMessage();

    String getTextMessage();

    // 这个太常用了，所以写进来了，本来是 QQMsgUtil 的 getType
    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Message> T getMessageType(MessageEvent event, Class<T> T) {
        return (T) event.getMessage().getMessageList().stream().filter(m -> T.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }

    @Nullable
    default ImageMessage getImage() {
        return getMessageType(this, ImageMessage.class);
    }

    default boolean isImage() {
        return getImage() != null;
    }

    @Nullable
    default AtMessage getAt() {
        return getMessageType(this, AtMessage.class);
    }

    default boolean isAt() {
        return getAt() != null;
    }

    default long getTarget() {
        if (getAt() != null) {
            return getAt().getTarget();
        } else {
            return 0L;
        }
    }
}
