package com.now.nowbot.qq;

import com.now.nowbot.qq.contact.Friend;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.Stranger;
import com.now.nowbot.qq.message.FileMessage;
import com.now.nowbot.qq.message.MessageChain;

import java.util.List;

public interface Bot {
    long getBotID();

    List<? extends Friend> getFriends();

    Friend getFriend(Long id);

    List<? extends Group> getGroups();

    Group getGroup(Long id);

    MessageChain getMessage(Long id);

    /**
     * 补全文件消息的下载地址。默认直接使用消息里带的 url。
     *
     * @param groupID 群号，私聊时为 null
     */
    default String getFileUrl(FileMessage file, Long groupID) {
        return file.getUrl();
    }

    List<? extends Stranger> getStrangers();

    Stranger getStranger(Long id);
}
