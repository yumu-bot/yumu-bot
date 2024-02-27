package com.now.nowbot.qq.message;

import java.util.Map;

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

    @Override
    public JsonMessage toJson() {
        return new JsonMessage("reply", Map.of("id", id));
    }

    @Override
    public String toString() {
        return STR."[at:\{id}]";
    }
}
