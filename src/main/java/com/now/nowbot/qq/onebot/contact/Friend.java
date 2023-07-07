package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;

public class Friend extends User implements com.now.nowbot.qq.contact.Friend {
    final String name;

    public Friend(Bot bot, long id, String name) {
        super(bot, id);
        this.name = name;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
