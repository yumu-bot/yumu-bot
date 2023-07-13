package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.response.DownloadFileResp;
import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.onebot.OneBotMessageReceipt;
import com.now.nowbot.util.QQMsgUtil;

import java.util.List;

public class Group extends Contact implements com.now.nowbot.qq.contact.Group {
    String name = null;

    public Group(Bot bot, long id) {
        super(bot, id);
    }

    public Group(Bot bot, long id, String name) {
        super(bot, id);
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) return name;
        var data = bot.getGroupInfo(getId(), false).getData();
        return data.getGroupName();
    }

    @Override
    public OneBotMessageReceipt sendMessage(MessageChain msg) {
        int id = 0;
        if (bot.sendGroupMsg(getId(), getMsg4Chain(msg), false).getData() != null) {
            id = bot.sendGroupMsg(getId(), getMsg4Chain(msg), false).getData().getMessageId();
        }
        return OneBotMessageReceipt.create(bot, id, this);
    }

    @Override
    public boolean isAdmin() {
        var data = bot.getGroupMemberInfo(getId(), bot.getSelfId(), false).getData();
        return data.getRole().equals("owner") || data.getRole().equals("admin");
    }

    @Override
    public GroupContact getUser(long qq) {
        var data = bot.getGroupMemberInfo(getId(), qq, false).getData();
        return new com.now.nowbot.qq.onebot.contact.GroupContact(bot, data.getUserId(), data.getNickname(), data.getRole(), this.getId());
    }

    @Override
    public List<? extends GroupContact> getAllUser() {
        var data = bot.getGroupMemberList(getId()).getData();
        return data.stream().map(f -> new com.now.nowbot.qq.onebot.contact.GroupContact(bot, f.getUserId(), f.getNickname(), f.getRole(), this.getId())).toList();
    }

    @Override
    public void sendFile(byte[] data, String name) {
        var url = QQMsgUtil.getFilePubUrl(data);
        try {
            DownloadFileResp rep;
            do {
                rep = bot.downloadFile(url).getData();
            } while (rep.getFile() != null);
            bot.uploadGroupFile(getId(), rep.getFile(), name);
        } finally {
            QQMsgUtil.removeFileUrl(url);
        }
    }
}
