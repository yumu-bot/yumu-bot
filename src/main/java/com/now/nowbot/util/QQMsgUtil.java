package com.now.nowbot.util;

import com.now.nowbot.dao.QQMessageDao;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.Nullable;

public class QQMsgUtil {

    private static QQMessageDao qqMessageDao;
    public static void init(QQMessageDao qqMessageDao){
        QQMsgUtil.qqMessageDao = qqMessageDao;
    }
    public static void recall(MessageChain msg){
        MessageSource.recall(msg);
    }
    @Nullable
    public static At getAt(MessageChain msg){
        return (At) msg.stream().filter(it -> it instanceof At).findFirst().orElse(null);
    }
    @Nullable
    public static MessageChain getReply(QuoteReply reply){
        return qqMessageDao.getReply(reply);
    }
}
