package com.now.nowbot.service;

import com.now.nowbot.qq.event.MessageEvent;

import java.util.Optional;
import java.util.regex.Matcher;

public interface MessageService<T> {

    static DataValue<?> createData() {
        return new DataValue();
    }

    default boolean isHandle(MessageEvent event, DataValue<T> data) throws Throwable {
        return false;
    }

//    default void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
//    }

    ;

    default void HandleMessage(MessageEvent event, T matcher) throws Throwable {
    }

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
