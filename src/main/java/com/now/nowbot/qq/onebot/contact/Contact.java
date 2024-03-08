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
            log.error("获取bot信息为空, 可能为返回数据超时, 但是仍然尝试发送");
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
            log.error(STR."发送消息时获取回执失败, 发送到[\{id}] 内容:[\{getMsg4Chain(msg)}]");
            return OneBotMessageReceipt.create();
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
        // 移除冗余
        throw new LogException("当前bot离线, 且未找到代替bot");
    }
}
