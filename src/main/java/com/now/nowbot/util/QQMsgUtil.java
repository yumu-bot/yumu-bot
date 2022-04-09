package com.now.nowbot.util;

import com.now.nowbot.dao.QQMessageDao;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Image;

import java.util.List;
import java.util.stream.Collectors;

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
    public static <T extends MessageContent> List<T> getTypeAll(MessageChain msg, Class<T> T){
        return msg.stream().filter(it -> T.isAssignableFrom(it.getClass())).map(it->(T)it).collect(Collectors.toList());
    }
    public static MessageChain getReplyMsg(MessageChain msg){
        var rep = getType(msg, QuoteReply.class);
        return getReply(rep);
    }
    @Nullable
    public static MessageChain getReply(QuoteReply reply){
        return qqMessageDao.getReply(reply);
    }

    public static MessageReceipt sendImage(Contact from, Image img){
        var date = img.encodeToData(EncodedImageFormat.PNG).getBytes();
        return from.sendMessage(from.uploadImage(ExternalResource.create(date)));
    }
    public static MessageReceipt sendImage(Contact from, Image img, EncodedImageFormat format){
        var date = img.encodeToData(format).getBytes();
        return from.sendMessage(from.uploadImage(ExternalResource.create(date)));
    }
    public static MessageReceipt sendImage(Contact from, Image img, int size){
        if (size < 0 || size > 100) throw new RuntimeException("范围异常");
        var date = img.encodeToData(EncodedImageFormat.JPEG,size).getBytes();
        return from.sendMessage(from.uploadImage(ExternalResource.create(date)));
    }
    public static MessageReceipt sendImage(Contact from, byte[] imgDate){
        return from.sendMessage(from.uploadImage(ExternalResource.create(imgDate)));
    }
    public static MessageReceipt sendTextAndImage(Contact from,String text, byte[] imgDate){
        return from.sendMessage(new PlainText(text).plus(from.uploadImage(ExternalResource.create(imgDate))));
    }

}
