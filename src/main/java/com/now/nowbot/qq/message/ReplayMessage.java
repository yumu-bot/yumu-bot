package com.now.nowbot.qq.message;

public class ReplayMessage extends Message {
    final long id;
    String text;

    public ReplayMessage(long messageId) {
        id = messageId;
    }

    public ReplayMessage(long messageId, String text) {
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
