package com.now.nowbot.qq.onebot;

import com.mikuac.shiro.model.ArrayMsg;
import com.now.nowbot.qq.contact.Stranger;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.onebot.contact.Group;
import com.now.nowbot.qq.onebot.event.MessageEvent;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

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

    @Override
    public MessageChain getMessage(Long id) {
        var action = bot.getMsg(id.intValue());

        if (Objects.isNull(action.getData())) return null;

        var data = JacksonUtil.parseObjectList(action.getData().getMessage(), ArrayMsg.class);

        if (CollectionUtils.isEmpty(data)) return null;

        return MessageEvent.getMessageChain(data);
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

    public com.mikuac.shiro.core.Bot getTrueBot() {
        return bot;
    }
}
