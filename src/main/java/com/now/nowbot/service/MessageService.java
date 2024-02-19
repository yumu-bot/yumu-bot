package com.now.nowbot.service;

import com.now.nowbot.qq.event.MessageEvent;

public interface MessageService<T> {

    boolean isHandle(MessageEvent event, String messageText, DataValue<T> data) throws Throwable;

    void HandleMessage(MessageEvent event, T data) throws Throwable;

    class DataValue<T> {
        T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
