package com.now.nowbot.qq.message;

import java.util.Map;

public class TextMessage extends Message {
    String message;

    public TextMessage(String msg) {
        message = msg;
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public JsonMessage toJson() {
        return new JsonMessage("text", Map.of("text", message));
    }
}
