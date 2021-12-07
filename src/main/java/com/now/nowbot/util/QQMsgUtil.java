package com.now.nowbot.util;

import com.now.nowbot.dao.QQMessageDao;
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
    public static <T> T getType(MessageChain msg, Class<T> T){
        return (T) msg.stream().filter(it -> T.isAssignableFrom(it.getClass())).findFirst().orElse(null);
    }
    public static MessageChain getReplyMsg(MessageChain msg){
        var rep = getType(msg, QuoteReply.class);
        return getReply(rep);
    }
    @Nullable
    public static MessageChain getReply(QuoteReply reply){
        return qqMessageDao.getReply(reply);
    }

}
