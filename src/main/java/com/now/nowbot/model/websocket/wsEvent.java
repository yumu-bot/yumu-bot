package com.now.nowbot.model.websocket;

import com.now.nowbot.util.JacksonUtil;

public class wsEvent {
    String event;

    public wsEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public static wsEvent chatStart(){
        return new wsEvent("chat.start");
    }

    public static wsEvent chatEnd(){
        return new wsEvent("chat.end");
    }

    public String toJsonStr() {
        return JacksonUtil.objectToJson(this);
    }
}
