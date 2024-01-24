package com.now.nowbot.util;

import com.now.nowbot.config.YumuConfig;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.message.Message;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.qq.message.ReplyMessage;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public class QQMsgUtil {
    private static final Base64.Encoder base64Util = Base64.getEncoder();
    private static final Map<String, FileData> FILE_DATA = new HashMap<>();
    private static QQMessageDao qqMessageDao;
    private static List<Long>   LocalBotList;
    private static String       LocalUrl;
    private static String       PublicUrl;

    public record FileData(String name, ByteBuffer bytes) {}

    public static void init(QQMessageDao qqMessageDao, YumuConfig yumuConfig) {
        QQMsgUtil.qqMessageDao = qqMessageDao;
        LocalBotList = yumuConfig.getPrivateDevice();
        LocalUrl = STR."\{yumuConfig.getPrivateUrl()}/pub/file/%s";
        PublicUrl = STR."\{yumuConfig.getPublicUrl()}/pub/file/%s";
    }

    public static String byte2str(byte[] data) {
        return base64Util.encodeToString(data);
    }
    @Nullable
    public static <T extends Message> T getType(MessageChain msg, Class<T> T) {
        return (T) msg.getMessageList().stream().filter(m ->T.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }


    @Deprecated
    public static void sendImage(Contact from, Image img) {
        beforeContact(from);
        from.sendImage(img.encodeToData(EncodedImageFormat.JPEG, 60).getBytes());
    }

    //这个太坑了，如果数据是空的，好像抛不出来
    @Deprecated
    public static void sendImage(Contact from, byte[] img) {
        beforeContact(from);
        from.sendImage(img);
    }
    private static void beforeContact(Contact from) {
//        from.sendMessage("正在处理图片请稍候...");
    }
    public static <T extends com.now.nowbot.qq.message.Message> List<T> getTypeAll(MessageChain msg, Class<T> T) {
        return msg.getMessageList().stream().filter(m ->T.isAssignableFrom(m.getClass())).map(it -> (T) it).toList();
    }
    @Nullable
    public static MessageChain getReply(ReplyMessage reply) {
        return qqMessageDao.getReply(reply);
    }

    public static MessageReceipt sendImageAndText(Contact from, byte[] imgData, String text) {
        beforeContact(from);
        return from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgData).addText(text).build());
    }

    public static void sendGroupFile(Group group, String name, byte[] data) {
        group.sendFile(data, name);
    }

    public static String getFileUrl(byte[] data, String name) {
        var key = UUID.randomUUID().toString();
        FILE_DATA.put(key, new FileData(name, ByteBuffer.wrap(data)));
        return String.format(LocalUrl, key);
    }
    public static String getFilePubUrl(byte[] data, String name) {
        var key = UUID.randomUUID().toString();
        FILE_DATA.put(key, new FileData(name, ByteBuffer.wrap(data)));
        return String.format(PublicUrl, key);
    }
    public static FileData getFileData(String key) {
        return FILE_DATA.get(key);
    }
    public static void  removeFileUrl(String url) {
        var index = url.lastIndexOf("/pub/file") + 10;
        var key = url.substring(index);
        System.out.println(key);
        FILE_DATA.remove(key);
    }

    public static boolean botInLocal(Long botQQ){
        return LocalBotList.contains(botQQ);
    }
}
