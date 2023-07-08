package com.now.nowbot.qq.onebot;

import com.now.nowbot.qq.contact.Stranger;
import com.now.nowbot.qq.onebot.contact.Group;

import java.util.List;

public class Bot implements com.now.nowbot.qq.Bot {
    private final com.mikuac.shiro.core.Bot bot;

    public Bot(com.mikuac.shiro.core.Bot bot) {
        this.bot = bot;
    }

    @Override
    public long getSelfId() {
        return bot.getSelfId();
    }

    @Override
    public List<com.now.nowbot.qq.onebot.contact.Friend> getFriends() {
        var data = bot.getFriendList().getData();
        return data.stream()
                .map(f -> new com.now.nowbot.qq.onebot.contact.Friend(bot, f.getUserId(), f.getNickname()))
                .toList();
    }

    @Override
    public com.now.nowbot.qq.onebot.contact.Friend getFriend(Long id) {
        var data = bot.getStrangerInfo(id, false).getData();
        return new com.now.nowbot.qq.onebot.contact.Friend(bot, data.getUserId(), data.getNickname());
    }

    @Override
    public List<com.now.nowbot.qq.onebot.contact.Group> getGroups() {
        var data = bot.getGroupList().getData();
        return data.stream()
                .map(g -> new com.now.nowbot.qq.onebot.contact.Group(bot, g.getGroupId(), g.getGroupName()))
                .toList();
    }

    @Override
    public com.now.nowbot.qq.onebot.contact.Group getGroup(Long id) {
        var data = bot.getGroupInfo(id, false).getData();
        return new Group(bot, data.getGroupId(), data.getGroupName());
    }

    /***
     * 未实现
     * @return null
     */
    @Override
    public List<Stranger> getStrangers() {
        return null;
    }

    /***
     * 未实现
     * @return null
     */
    @Override
    public Stranger getStranger(Long id) {
        return null;
    }
    public com.mikuac.shiro.core.Bot getTrueBot(){
        return bot;
    }
}
