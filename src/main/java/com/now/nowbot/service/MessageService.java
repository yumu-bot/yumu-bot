package com.now.nowbot.service;

import com.now.nowbot.qq.event.MessageEvent;

public interface MessageService<T> {

    default boolean isHandle(MessageEvent event, DataValue<T> data) throws Throwable {
        return false;
    }

    void HandleMessage(MessageEvent event, T matcher) throws Throwable;

    ;

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
