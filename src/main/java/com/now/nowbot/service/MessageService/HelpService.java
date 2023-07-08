package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("help")
public class HelpService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        String module = matcher.group("module").trim(); //传东西进来
        String path = switch (module) {
            case "bot", "b" -> "help-bot.png";
            case "score", "s" -> "help-score.png";
            case "player", "p" -> "help-player.png";
            case "map", "m" -> "help-map.png";
            case "chat", "c" -> "help-chat.png";
            case "fun", "f" -> "help-fun.png";
            case "aid", "a" -> "help-aid.png";
            case "tournament", "t" -> "help-tournament.png";
            default -> "help-default.png";
        };

        path = "ExportFileV3/" + path;

        QQMsgUtil.sendImage(from,Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve(path)));

    }
}
