package com.now.nowbot.util;

import com.now.nowbot.config.YumuConfig;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.Message;
import com.now.nowbot.qq.message.MessageChain;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public class QQMsgUtil {
    private static final Base64.Encoder base64Util = Base64.getEncoder();
    private static final Map<String, FileData> FILE_DATA = new HashMap<>();
    private static List<Long>   LocalBotList;
    private static String       LocalUrl;
    private static String       PublicUrl;

    public record FileData(String name, ByteBuffer bytes) {}

    public static void init(YumuConfig yumuConfig) {
        LocalBotList = yumuConfig.getPrivateDevice();
        LocalUrl = STR."\{yumuConfig.getPrivateUrl()}/pub/file/%s";
        PublicUrl = STR."\{yumuConfig.getPublicUrl()}/pub/file/%s";
    }

    public static String byte2str(byte[] data) {
        if (Objects.isNull(data)) return "";
        return base64Util.encodeToString(data);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T extends Message> T getType(MessageChain msg, Class<T> T) {
        return (T) msg.getMessageList().stream().filter(m -> T.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }

    public static MessageChain getImage(byte[] image) {
        return new MessageChain.MessageChainBuilder().addImage(image).build();
    }

    public static MessageChain getTextAndImage(String text,byte[] image) {
        return new MessageChain.MessageChainBuilder().addText(text).addImage(image).build();
    }


    public static void sendImages(MessageEvent event, List<byte[]> images) throws InterruptedException {
        var b = new MessageChain.MessageChainBuilder();

        for (int i = 0; i < images.size(); i++) {
            var image = images.get(i);

            // qq 一次性只能发 20 张图
            if (i >= 20 && i % 20 == 0) {
                event.reply(b.build());
                Thread.sleep(1000L);
                b = new MessageChain.MessageChainBuilder();
            }

            b.addImage(image);
        }

        event.reply(b.build());
    }

    private static void beforeContact(Contact from) {
        //from.sendMessage("正在处理图片请稍候...");
    }

    @SuppressWarnings("unchecked")
    public static <T extends com.now.nowbot.qq.message.Message> List<T> getTypeAll(MessageChain msg, Class<T> T) {
        return msg.getMessageList().stream().filter(m ->T.isAssignableFrom(m.getClass())).map(it -> (T) it).toList();
    }

    @Deprecated
    public static void sendImageAndText(MessageEvent event, byte[] image, String text) {
        var from = event.getSubject();
        sendImageAndText(from, image, text);
    }

    @Deprecated
    public static void sendImageAndText(Contact from, byte[] image, String text) {
        beforeContact(from);
        from.sendMessage(new MessageChain.MessageChainBuilder().addImage(image).addText(text).build());
    }

    public static void sendGroupFile(MessageEvent event, String name, byte[] data) {
        var from = event.getSubject();

        if (from instanceof Group group) {
            group.sendFile(data, Optional.ofNullable(name).orElse("Yumu-file"));
        }
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
