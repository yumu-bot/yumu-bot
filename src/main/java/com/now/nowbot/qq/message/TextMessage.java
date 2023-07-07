package com.now.nowbot.qq.message;

public class TextMessage extends Message {
    String message;

    public TextMessage(String msg) {
        message = msg;
    }

    @Override
    public String toString() {
        return message;
    }
}
