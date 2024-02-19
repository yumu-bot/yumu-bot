package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.FileConfig;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xyz.spring.oirc.IRCClient;
import xyz.spring.oirc.IRCConfig;
import xyz.spring.oirc.MpChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
public class MpService implements MessageService<String> {
    private static IRCClient client    = null;
    private static MpChannel mpChannel = null;
    Proxy proxy = null;

    public MpService(NowbotConfig config, FileConfig fileConfig) throws IOException {
        proxy = new Proxy(Proxy.Type.valueOf(config.proxyType), new InetSocketAddress(config.proxyHost, config.proxyPort));
        var path = Path.of(fileConfig.getRoot(), "irc.conf");
        if (! Files.isRegularFile(path)) {
            return;
        }
        var line = Files.readAllLines(path);
        String[] name = line.getFirst().split("\\|");
        if (name.length != 2) return;
        client = createClient(name[0], name[1]);
    }

    IRCClient createClient(String name, String password) {
        var conf = IRCConfig.Companion.build(set -> {
            set.setUsername(name);
            set.setPassword(password);
            set.setProxy(proxy);
            return null;
        });
        return new IRCClient(conf);
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<String> data) throws Throwable {
        if (! messageText.startsWith("!mp-")) return false;
        data.setValue(messageText.substring(4));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, String data) throws Throwable {
        var from = event.getSubject();
        switch (data) {
            case "connect" -> {
                connent();
            }
            case "disconnect" -> {
                close();
            }
            case "make" -> {
                if (mpChannel != null) {
                    tips("已经存在该房间了");
                }
                makeMp("yumu auto host", from);
                if (mpChannel == null) tips("创建失败");
                mpChannel.setPassword("114514");
            }
            case "auto" -> {
                var list = mpChannel.getEventHandler().get(MpChannel.EventType.AllReady);
                // 如果不存在自动启动监听则添加
                if (CollectionUtils.isEmpty(list)) {
                    mpChannel.onEvent(MpChannel.EventType.AllReady, s -> {
                        mpChannel.startMp(0);
                    });
                    from.sendMessage("自动开始比赛");
                } else {
                    from.sendMessage("已经是自动开房了");
                }
            }
            case "disable-auto" -> {
                var list = mpChannel.getEventHandler().get(MpChannel.EventType.AllReady);
                if (list != null) list.clear();
                from.sendMessage("关闭自动开始");
            }
            case "info" -> {
                mpChannel.getMpInfo();
                Thread.sleep(Duration.ofSeconds(3));
                from.sendMessage(STR."""
                        link: \{mpChannel.getMpLink()}
                        host name: \{mpChannel.getHostName()}
                        players: \{getPlayersInfo(mpChannel.getPlayers())}
                        map: \{mpChannel.getBeatmapId()}
                        """);
            }
            case null -> {
                // impossible
            }
            default -> {
                if (mpChannel == null) tips("目前不存在房间");
                mpChannel.sendMessage(data);
            }
        }
    }

    void connent() {
        checkClient();
        Thread.startVirtualThread(() -> {
            client.start();
        });
    }

    void close() {
        checkClient();
        mpChannel = null;
        client.close();
    }

    void makeMp(String name, Contact contact) {
        checkClient();
        if (mpChannel != null) tips("已经有一个房间了!");
        client.send("BanchoBot", STR."!mp make \{name}");
        mpChannel = (MpChannel) client.getChannel(null);
        if (mpChannel == null) tips("创建失败");
        mpChannel.onEvent(MpChannel.EventType.UserMessage, contact::sendMessage);
        mpChannel.onEvent(MpChannel.EventType.BeatmapChange, s -> {
            contact.sendMessage(STR."更换谱面: \{s}");
        });
        mpChannel.onEvent(MpChannel.EventType.HostChange, s -> {
            contact.sendMessage(STR."房主变更: \{s}");
        });
        mpChannel.onEvent(MpChannel.EventType.UserJoin, s -> {
            boolean hasUser = false;

            for (var u : mpChannel.getPlayers()) {
                if (u != null) {
                    hasUser = true;
                    break;
                }
            }

            if (! hasUser) {
                var userName = s.split("\\|")[0];
                mpChannel.hostPlayer(userName);
            }
        });
    }

    void checkClient() {
        if (client == null) tips("加载配置文件失败");
    }

    void tips(String s) {
        throw new TipsRuntimeException(s);
    }

    String getPlayersInfo(MpChannel.Player[] players) {
        StringBuilder sb = new StringBuilder();
        for (var p : players) {
            if (p == null) continue;
            sb.append(p.getName());
            if (p.isHost()) sb.append("<host>");
            sb.append(" ");
        }
        return sb.toString();
    }
}
