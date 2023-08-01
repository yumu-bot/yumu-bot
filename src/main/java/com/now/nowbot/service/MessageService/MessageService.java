package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;

import java.util.Optional;
import java.util.regex.Matcher;

public interface MessageService<T> {

    static DataValue createData() {
        return new DataValue();
    }

    default boolean isHandle(MessageEvent event, DataValue data) {
        return false;
    }

    default void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
    }

    ;

    default void HandleMessage(MessageEvent event, T matcher) throws Throwable {
    }

    ;

    class DataValue {
        Object value;

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
