package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.message.*;
import com.now.nowbot.util.QQMsgUtil;

public class User implements com.now.nowbot.qq.contact.User {
    final Bot bot;
    final long id;
    protected User(Bot bot, long id) {
        this.id = id;
        this.bot = bot;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int sendMessage(MessageChain msg) {
        return bot.sendGroupMsg(getId(), getMsg4Chain(msg), false).getData().getMessageId();
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
                    if (at.isAll()) builder.at(at.getQQ());
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
