package com.now.nowbot.qq.message;

public class ReplyMessage extends Message {
    final long id;
    String text;

    public ReplyMessage(long messageId) {
        id = messageId;
    }

    public ReplyMessage(long messageId, String text) {
        id = messageId;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
