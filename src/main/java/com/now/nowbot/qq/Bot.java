package com.now.nowbot.qq;

import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Stranger;
import com.now.nowbot.qq.message.MessageChain;

import java.util.List;

public interface Bot {
    long getSelfId();
    List<? extends Friend> getFriends();

    Friend getFriend(Long id);

    List<? extends Group> getGroups();

    Group getGroup(Long id);

    MessageChain getMessage(Long id);

    List<? extends Stranger> getStrangers();

    MessageChain getMessage(Long id);

    Stranger getStranger(Long id);
}
