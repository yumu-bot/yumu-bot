package com.now.nowbot.qq.onebot.event;


public class Event implements com.now.nowbot.qq.event.Event {
    private final com.now.nowbot.qq.onebot.Bot bot;

    public Event(long botId) {
        this.bot = new com.now.nowbot.qq.onebot.Bot(botId);
    }

    public com.now.nowbot.qq.onebot.Bot getBot() {
        return bot;
    }
}
