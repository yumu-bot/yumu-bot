package com.now.nowbot.qq;

import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Stranger;

import java.util.List;

public class BotFromWebsocket implements Bot{


    @Override
    public List<Friend> getFriends() {
        return null;
    }

    @Override
    public Friend getFriend(Long id) {
        return null;
    }

    @Override
    public List<Group> getGroups() {
        return null;
    }

    @Override
    public Group getGroup(Long id) {
        return null;
    }

    @Override
    public List<Stranger> getStrangers() {
        return null;
    }

    @Override
    public Stranger getStranger(Long id) {
        return null;
    }
}
