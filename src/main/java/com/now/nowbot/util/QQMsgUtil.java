package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.message.Message;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.qq.message.ReplayMessage;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public class QQMsgUtil {
    private static final Base64.Encoder base64Util = Base64.getEncoder();
    private static final Map<String, FileData> FILE_DATA = new HashMap<>();
    private static       QQMessageDao   qqMessageDao;

    public record FileData(String name, ByteBuffer bytes) {}

    public static void init(QQMessageDao qqMessageDao) {
        QQMsgUtil.qqMessageDao = qqMessageDao;
    }

    public static String byte2str(byte[] data) {
        return base64Util.encodeToString(data);
    }
    @Nullable
    public static <T extends Message> T getType(MessageChain msg, Class<T> T) {
        return (T) msg.getMessageList().stream().filter(m ->T.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }


    public static void sendImage(Contact from, Image img) {
        befor(from);
        from.sendImage(img.encodeToData(EncodedImageFormat.JPEG, 60).getBytes());
    }
    public static void sendImage(Contact from, byte[] img) {
        befor(from);
        from.sendImage(img);
    }
    private static void befor(Contact from) {
//        from.sendMessage("正在处理图片请稍候...");
    }
    public static <T extends com.now.nowbot.qq.message.Message> List<T> getTypeAll(MessageChain msg, Class<T> T) {
        return msg.getMessageList().stream().filter(m ->T.isAssignableFrom(m.getClass())).map(it -> (T) it).toList();
    }
    @Nullable
    public static MessageChain getReply(ReplayMessage reply) {
        return qqMessageDao.getReply(reply);
    }

    public static MessageReceipt sendTextAndImage(Contact from, String text, byte[] imgData) {
        befor(from);
        return from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgData).addText(text).build());
    }

    public static void sendGroupFile(Group group, String name, byte[] data) {
        group.sendFile(data, name);
    }

    public static String getFileUrl(byte[] data, String name) {
        var key = UUID.randomUUID().toString();
        FILE_DATA.put(key, new FileData(name, ByteBuffer.wrap(data)));
        return String.format("http://127.0.0.1:%d/pub/file/%s",NowbotConfig.PORT,key);
    }
    public static String getFilePubUrl(byte[] data, String name) {
        var key = UUID.randomUUID().toString();
        FILE_DATA.put(key, new FileData(name, ByteBuffer.wrap(data)));
        return "https://bot.365246692.xyz/pub/file/" + key;
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
        boolean f = false;
        if (botQQ != null && botQQ == 1563653406L) {
            return true;
        }
        return false;
    }
}
