package com.now.nowbot.qq;

import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Stranger;

import java.util.List;

public interface Bot {
    List<Friend> getFriends();
    Friend getFriend(Long id);

    List<Group> getGroups();
    Group getGroup(Long id);

    List<Stranger> getStrangers();
    Stranger getStranger(Long id);
}
