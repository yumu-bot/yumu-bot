package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.qq.onebot.OneBotMessageReceipt;
import com.now.nowbot.util.QQMsgUtil;

public class Contact implements com.now.nowbot.qq.contact.Contact {
    String name;
    final Bot  bot;
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
        if (this instanceof Group g) {
            return g.sendMessage(msg);
        } else if (this instanceof GroupContact g) {
            int id = bot.sendPrivateMsg(g.groupId, g.getId(), getMsg4Chain(msg), false).getData().getMessageId();
            return OneBotMessageReceipt.create(bot, id, this);
        } else {
            int id = bot.sendPrivateMsg(getId(), getMsg4Chain(msg), false).getData().getMessageId();
            return OneBotMessageReceipt.create(bot, id, this);
        }
    }

    protected static String getMsg4Chain(MessageChain messageChain) {
        var builder = MsgUtils.builder();
        for (var msg : messageChain.getMessageList()) {
            switch (msg) {
                case ImageMessage img -> {
                    if (img.isByteArray()) builder.img("base64://" + QQMsgUtil.byte2str(img.getData()));
                    else builder.img(img.getPath());
                }
                case AtMessage at -> {
                    if (!at.isAll()) builder.at(at.getQQ());
                    else builder.atAll();
                }
                case TextMessage text -> {
                    builder.text(text.toString());
                }
                case ReplayMessage re -> {
                    builder.reply(Long.toString(re.getId()));
                }
                default -> {

                }
            }
        }
        return builder.build();
    }
}
