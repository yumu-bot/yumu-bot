package com.now.nowbot.qq.onebot.event;

import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.contact.Contact;
import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.onebot.contact.Group;
import com.now.nowbot.qq.onebot.contact.GroupContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MessageEvent extends Event implements com.now.nowbot.qq.event.MessageEvent {
    private static final Logger l = LoggerFactory.getLogger("msg");
    com.mikuac.shiro.dto.event.message.MessageEvent event;

    public MessageEvent(com.mikuac.shiro.dto.event.message.MessageEvent event, Bot bot) {
        super(bot);
        this.event = event;
    }

    @Override
    public Contact getSubject() {
        if (event instanceof com.mikuac.shiro.dto.event.message.GroupMessageEvent e) {
            return new Group(getBot().getTrueBot(), e.getGroupId());
        }
        return new Contact(getBot().getTrueBot(), event.getUserId());
    }

    @Override
    public Contact getSender() {
        if (event instanceof com.mikuac.shiro.dto.event.message.GroupMessageEvent e) {
            return new Group(getBot().getTrueBot(), e.getSender().getUserId());
        }
        return new Contact(getBot().getTrueBot(), event.getUserId());
    }

    @Override
    public MessageChain getMessage() {
        if (event instanceof com.mikuac.shiro.dto.event.message.GroupMessageEvent c && c.getSender().getUserId().equals(365246692L)) {
            l.error("---------------event-----------------");
            event.getArrayMsg().forEach(e -> {
                l.error(e.toString());
            });
            l.error("---------------event-end-----------------");
        }
        return getMessageChain(event.getArrayMsg());
    }

    public String getRawMessage() {
        return decode(event.getRawMessage());
    }

    public static MessageChain getMessageChain(List<ArrayMsg> msgs) {
        var msg = msgs.stream().map(arrayMsg -> {
            Message m;
            switch (arrayMsg.getType()) {
                case at -> m = new AtMessage(Long.parseLong(arrayMsg.getData().getOrDefault("qq", "0")));
                case text -> m = new TextMessage(decodeArr(arrayMsg.getData().getOrDefault("text", "")));
                case reply -> m = new ReplayMessage(Integer.parseInt(arrayMsg.getData().getOrDefault("id", "0")),
                        decodeArr(arrayMsg.getData().getOrDefault("text", "")));
                case image -> {
                    try {
                        m = new ImageMessage(new URL(arrayMsg.getData().getOrDefault("url", "")));
                    } catch (MalformedURLException e) {
                        m = new TextMessage("[图片;加载异常]");
                    }
                }
                default -> m = new TextMessage(String.format("[%s;不支持的操作类型]", arrayMsg.getType().toString()));
            }
            return m;
        }).toList();
        return new MessageChain(msg);
    }

    private static String decode(String m) {
        return ShiroUtils.unescape(m);
    }
    private static String decodeArr(String m) {
        return m;
    }
}
-