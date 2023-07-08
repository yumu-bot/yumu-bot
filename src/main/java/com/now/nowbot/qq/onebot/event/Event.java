package com.now.nowbot.qq.onebot.event;


import com.now.nowbot.qq.Bot;

public class Event implements com.now.nowbot.qq.event.Event {
    private final com.now.nowbot.qq.onebot.Bot bot;

    public Event(com.mikuac.shiro.core.Bot bot) {
        this.bot = new com.now.nowbot.qq.onebot.Bot(bot);
    }

    public com.now.nowbot.qq.onebot.Bot getBot() {
        return bot;
    }
}
