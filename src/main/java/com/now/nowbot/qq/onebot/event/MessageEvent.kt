package com.now.nowbot.qq.onebot.event;

import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.contact.Contact;
import com.now.nowbot.qq.onebot.contact.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MessageEvent extends Event implements com.now.nowbot.qq.event.MessageEvent {
    private static final Logger log = LoggerFactory.getLogger("msg");
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
        return getMessageChain(event.getArrayMsg());
    }

    public String getRawMessage() {
        return event.getRawMessage();
    }

    @Override
    public String getTextMessage() {
        return getMessage().getMessageList()
                .stream()
                .filter(m -> m instanceof TextMessage)
                .map(Message::toString)
                .collect(Collectors.joining());
    }

    public static MessageChain getMessageChain(List<ArrayMsg> msgs) {
        var msg = msgs.stream().map(arrayMsg -> {
            Message m;
            switch (arrayMsg.getType()) {
                case at -> {
                    var qqStr = arrayMsg.getData().getOrDefault("qq", "0");

                    try {
                        m = new AtMessage(Long.parseLong(qqStr));
                    } catch (NumberFormatException e) {
                        //艾特全体是 -1。扔过来的可能是 "all"
                        m = new AtMessage(-1L);
                    }
                }
                case text -> m = new TextMessage(decodeArr(arrayMsg.getData().getOrDefault("text", "")));
                case reply -> m = new ReplyMessage(Integer.parseInt(arrayMsg.getData().getOrDefault("id", "0")),
                        decodeArr(arrayMsg.getData().getOrDefault("text", "")));
                case image -> {
                    try {
                        m = new ImageMessage(new URL(arrayMsg.getData().getOrDefault("url", "")));
                    } catch (MalformedURLException e) {
                        m = new TextMessage("[图片;加载异常]");
                    }
                }

                case null, default -> m = new TextMessage(
                        String.format("[%s;不支持的操作类型]", Optional.ofNullable(arrayMsg.getType()).orElse(MsgTypeEnum.unknown))
                );
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
