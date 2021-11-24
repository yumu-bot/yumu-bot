package com.now.nowbot.dao;

import com.now.nowbot.mapper.MessageMapper;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QQMessageDao {
    MessageMapper messageMapper;
    @Autowired
    QQMessageDao(MessageMapper messageMapper){
        this.messageMapper = messageMapper;
    }

    public MessageChain getReply(QuoteReply reply){
        var rawid = reply.getSource().getIds()[0];
        var internalId = reply.getSource().getInternalIds()[0];
        var time = reply.getSource().getTime();
        var msglite = messageMapper.getAllByRawIdAndInternalAndTime(rawid,internalId, (long) time);
        return msglite.getMessage();
    }
}
