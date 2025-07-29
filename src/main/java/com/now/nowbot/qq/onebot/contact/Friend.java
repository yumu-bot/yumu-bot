package com.now.nowbot.qq.onebot.contact;

public class Friend extends Contact implements com.now.nowbot.qq.contact.Friend {
    final String name;

    public Friend(long botId, long id, String name) {
        super(botId, id);
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
