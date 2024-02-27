package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.now.nowbot.config.OneBotConfig;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.OneBotMessageReceipt;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.util.QQMsgUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Contact implements com.now.nowbot.qq.contact.Contact {
    String name;
    Bot bot;
    final long id;

    public Contact(Bot bot, long id) {
        this.id = id;
        this.bot = bot;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public OneBotMessageReceipt sendMessage(MessageChain msg) {
        try {
            getIfNewBot();
        } catch (NullPointerException e) {
            throw new LogException(STR."获取bot为空, 本次消息内容:[\{getMsg4Chain(msg)}]");
        }
        long id = 0;
        ActionData<MsgId> d;
        if (this instanceof Group g) {
            d = bot.customRequest(() -> "send_group_msg", Map.of(
                    "group_id", getId(),
                    "message", getMsgJson(msg),
                    "auto_escape", false
            ), MsgId.class);
            id = getId();
        } else if (this instanceof GroupContact g) {
            d = bot.customRequest(() -> "send_private_msg", Map.of(
                    "group_id", g.getGroupId(),
                    "user_id", g.getId(),
                    "message", getMsgJson(msg),
                    "auto_escape", false
            ), MsgId.class);
            id = g.getGroupId();
        } else {
            d = bot.customRequest(() -> "send_private_msg", Map.of(
                    "user_id", getId(),
                    "message", getMsgJson(msg),
                    "auto_escape", false
            ), MsgId.class);
            id = getId();
        }
        if (d != null && d.getData() != null && d.getData().getMessageId() != null) {
            return OneBotMessageReceipt.create(bot, d.getData().getMessageId(), this);
        } else {
            throw new LogException(STR."发送消息时获取回执失败, 发送到[\{id}] 内容:[\{getMsg4Chain(msg)}]");
        }
    }

    protected static String getMsg4Chain(MessageChain messageChain) {
        var s = messageChain.getMessageList().stream().map(Message::toString).collect(Collectors.joining());
        if (Objects.nonNull(s)) return s;
        var builder = MsgUtils.builder();
        for (var msg : messageChain.getMessageList()) {
            switch (msg) {
                case ImageMessage img -> {
                    if (img.isByteArray()) builder.img(STR."base64://\{QQMsgUtil.byte2str(img.getData())}");
                    else builder.img(img.getPath());
                }
                case VoiceMessage voice -> builder.voice(STR."base64://\{QQMsgUtil.byte2str(voice.getData())}");
                case AtMessage at -> {
                    if (! at.isAll()) builder.at(at.getQQ());
                    else builder.atAll();
                }
                case TextMessage text -> builder.text(text.toString());
                case ReplyMessage re -> builder.reply(Long.toString(re.getId()));
                case null, default -> {
                }
            }
        }
        return builder.build();
    }

    protected static List<Message.JsonMessage> getMsgJson(MessageChain messageChain) {
        return messageChain.getMessageList().stream().map(Message::toJson).filter(Objects::nonNull).toList();
    }

    private boolean testBot() {
        try {
            bot.getLoginInfo().getData();
            return true;
        } catch (Exception e) {
            log.error("test bot only", e);
            return false;
        }
    }

    private void getIfNewBot() {
        if (testBot()) {
            return;
        } else if (OneBotConfig.getBotContainer().robots.containsKey(bot.getSelfId())) {
            bot = OneBotConfig.getBotContainer().robots.get(bot.getSelfId());
            if (testBot()) return;
        }
        for (var botEntry : OneBotConfig.getBotContainer().robots.entrySet()) {
            var newBot = botEntry.getValue();
            if (! newBot.getStatus().getGood()) continue;
            if (this instanceof Group g) {
                var groups = newBot.getGroupInfo(g.getId(), false).getData();
                if (groups.getMemberCount() > 0) {
                    this.bot = newBot;
                    return;
                }
            } else if (this instanceof GroupContact c) {
                var groups = newBot.getGroupInfo(c.getGroupId(), false).getData();
                if (groups.getMemberCount() > 0) {
                    this.bot = newBot;
                    return;
                }
            } else {
                var friends = newBot.getFriendList().getData();
                AtomicBoolean has = new AtomicBoolean(false);
                friends.forEach(f -> {
                    if (f.getUserId() == getId()) has.set(true);
                });
                if (has.get()) {
                    this.bot = newBot;
                    return;
                }
            }
        }
        throw new RuntimeException("当前bot离线, 且未找到代替bot");
    }
}
