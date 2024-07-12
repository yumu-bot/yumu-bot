package com.now.nowbot.qq.local.contact;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.message.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class LocalContact implements com.now.nowbot.qq.contact.Contact {
    @Override
    public long getId() {
        return - 10086;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public MessageReceipt sendMessage(MessageChain msg) {
        LocalContact contact = this;
        var message = msg.getMessageList().stream().map(m -> switch (m) {
            case AtMessage at -> STR."[@\{at.getQQ()}]";
            case ReplyMessage re -> STR."[回复:\{re.getId()}]";
            case TextMessage text -> text.toString();
            case VoiceMessage voiceMessage -> {
                var data = voiceMessage.getData();
                var path = saveFile(STR."\{System.currentTimeMillis()}.mp3", data);
                yield STR."[voice: \{path}]";
            }
            case ImageMessage image -> {
                String localPath;
                if (image.isByteArray()) {
                    localPath = saveFile(STR."\{System.currentTimeMillis()}.jpg", image.getData());
                } else if (image.isUrl()) {
                    localPath = downloadFile(image.getPath());
                } else {
                    localPath = copyFile(Path.of(URI.create(image.getPath().replaceAll("\\\\", "/"))));
                }
                yield STR."[图片: \{localPath}]";
            }
            case null, default -> "[未知类型]";
        }).collect(Collectors.joining());
        log.info("bot: \"{}\"", message);
        return new MessageReceipt() {

            @Override
            public void recall() {
                log.info("bot 撤回消息: {}", message);
            }

            @Override
            public void recallIn(long time) {
                Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(time);
                        recall();
                    } catch (InterruptedException e) {
                    }
                });
            }

            @Override
            public com.now.nowbot.qq.contact.Contact getTarget() {
                return contact;
            }

            @Override
            public ReplyMessage reply() {
                return new ReplyMessage(0, "本地消息");
            }
        };
    }

    String saveFile(String name, byte[] data) {
        var path = Path.of(NowbotConfig.RUN_PATH, "debug");
        try {
            if (! Files.isDirectory(path)) Files.createDirectories(path);
            var nPath = path.resolve(name);
            Files.write(nPath, data);
            return nPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.info("创建文件夹 [{}] 失败", path.toAbsolutePath());
            return "err";
        }
    }

    String downloadFile(String url) {
        var path = Path.of(NowbotConfig.RUN_PATH, "debug");
        try {
            if (! Files.isDirectory(path)) Files.createDirectories(path);
            var urlObj = URI.create(url).toURL();
            var connection = urlObj.openConnection();
            var data = connection.getInputStream().readAllBytes();
            var nPath = path.resolve(STR."\{System.currentTimeMillis()}.jpg");
            Files.write(nPath, data);
            return nPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.info("创建文件夹 [{}] 失败", path.toAbsolutePath());
            return "err";
        }
    }

    String copyFile(Path source) {
        var path = Path.of(NowbotConfig.RUN_PATH, "debug");
        try {
            if (! Files.isDirectory(path)) Files.createDirectories(path);
            var nPath = path.resolve(STR."\{System.currentTimeMillis()}.jpg");
            Files.copy(source, nPath);
            return nPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.info("创建文件夹 [{}] 失败", path.toAbsolutePath());
            return "err";
        }
    }
}
