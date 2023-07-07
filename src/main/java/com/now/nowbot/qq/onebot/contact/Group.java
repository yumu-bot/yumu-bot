package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.message.MessageChain;

import java.util.List;

public class Group extends User implements com.now.nowbot.qq.contact.Group {
    String name = null;

    public Group(Bot bot, long id) {
        super(bot, id);
    }

    public Group(Bot bot, long id, String name) {
        super(bot, id);
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) return name;
        var data = bot.getGroupInfo(getId(), false).getData();
        return data.getGroupName();
    }

    @Override
    public int sendMessage(MessageChain msg) {
        return bot.sendGroupMsg(getId(), getMsg4Chain(msg), false).getData().getMessageId();
    }

    @Override
    public boolean isAdmin() {
        var data = bot.getGroupMemberInfo(getId(), bot.getSelfId(), false).getData();
        return data.getRole().equals("owner") || data.getRole().equals("admin");
    }

    @Override
    public Friend getUser(long qq) {
        var data = bot.getGroupMemberInfo(getId(), qq, false).getData();
        return new com.now.nowbot.qq.onebot.contact.Friend(bot, data.getUserId(), data.getNickname());
    }

    @Override
    public List<? extends Friend> getAllUser() {
        var data = bot.getGroupMemberList(getId()).getData();
        return data.stream().map(f -> new com.now.nowbot.qq.onebot.contact.Friend(bot, f.getUserId(), f.getNickname())).toList();
    }
}
