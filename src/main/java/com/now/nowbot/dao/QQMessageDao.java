package com.now.nowbot.dao;

import com.now.nowbot.mapper.MessageMapper;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.ReplayMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QQMessageDao {
    MessageMapper messageMapper;

    @Autowired
    QQMessageDao(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public MessageChain getReply(ReplayMessage reply) {
        // todo 待实现
        var id = reply.getId();
        return null;
        /*
        var msglite = messageMapper.getAllByRawIdAndInternalAndTime(rawid,internalId, (long) time);
        return msglite.getMessage();
        */
    }
}
