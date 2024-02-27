package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.now.nowbot.config.OneBotConfig;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.OneBotMessageReceipt;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.JacksonUtil;
import com.now.nowbot.util.QQMsgUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
//        if (msg != null) {
//            log.info("send: [{}]", msg.getRawMessage());
//            return null;
//        }
        getIfNewBot();
        int id = 0;
        ActionData<MsgId> d;
        if (this instanceof Group g) {
            var test = ContextUtil.getContext("isTest", Boolean.class);
            if (test != null && test) {
                d = bot.sendGroupMsg(g.getId(), getMsgJson(msg), false);
            } else {
                d = bot.sendGroupMsg(g.getId(), getMsg4Chain(msg), false);
            }
        } else if (this instanceof GroupContact g) {
            d = bot.sendPrivateMsg(g.getGroupId(), g.getId(), getMsg4Chain(msg), false);
        } else {
            d = bot.sendGroupMsg(getId(), getMsg4Chain(msg), false);
        }
        if (d != null && d.getData() != null && d.getData().getMessageId() != null) {
            id = d.getData().getMessageId();
        }
        return OneBotMessageReceipt.create(bot, id, this);
    }

    protected static String getMsgJson(MessageChain messageChain) {
        List<Message.JsonMessage> l = messageChain.getMessageList().stream().map(Message::toJson).filter(Objects::nonNull).toList();
        return JacksonUtil.objectToJson(l);
    }
    protected static String getMsg4Chain(MessageChain messageChain) {
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
