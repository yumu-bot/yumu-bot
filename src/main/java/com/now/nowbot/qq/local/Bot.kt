package com.now.nowbot.qq.local;

import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Stranger;
import com.now.nowbot.qq.message.MessageChain;

import java.util.List;

public class Bot implements com.now.nowbot.qq.Bot {
    @Override
    public long getSelfId() {
        return 0;
    }

    @Override
    public List<? extends Friend> getFriends() {
        return List.of();
    }

    @Override
    public Friend getFriend(Long id) {
        return null;
    }

    @Override
    public List<? extends Group> getGroups() {
        return List.of();
    }

    @Override
    public Group getGroup(Long id) {
        return null;
    }

    @Override
    public MessageChain getMessage(Long id) {
        return null;
    }

    @Override
    public List<? extends Stranger> getStrangers() {
        return List.of();
    }

    @Override
    public Stranger getStranger(Long id) {
        return null;
    }
}
