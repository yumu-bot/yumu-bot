package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.onebot.OneBotMessageReceipt;

public class Friend extends Contact implements com.now.nowbot.qq.contact.Friend {
    final String name;

    public Friend(Bot bot, long id, String name) {
        super(bot, id);
        this.name = name;
    }

    @Override
    public OneBotMessageReceipt sendMessage(MessageChain msg) {
        int id = 0;
        var d = bot.sendGroupMsg(getId(), getMsg4Chain(msg), false).getData();
        if (d != null) {
            id = d.getMessageId();
        }
        return OneBotMessageReceipt.create(bot, id, this);
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
