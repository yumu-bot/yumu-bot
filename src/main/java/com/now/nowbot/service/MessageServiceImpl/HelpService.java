package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("help")
public class HelpService implements MessageService<Matcher> {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean isSendLink = true; //这是防止web不能用，临时关闭的布尔值
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

        String web = "https://docs.365246692.xyz/help/";
        String html = ".html";
        String link = switch (module) {
            case "bot", "b" -> "bot" + html;
            case "score", "s" -> "score" + html;
            case "player", "p" -> "player" + html;
            case "map", "m" -> "map" + html;
            case "chat", "c" -> "chat" + html;
            case "fun", "f" -> "fun" + html;
            case "aid", "a" -> "aid" + html;
            case "tournament", "t" -> "tournament" + html;
            default -> "";
        };

        QQMsgUtil.sendImage(from, Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve("ExportFileV3/" + path)));

        if (isSendLink) {
            var receipt = from.sendMessage("请参阅：" + web + link);
            //默认110秒后撤回
            from.recallIn(receipt, 110 * 1000);
        }
    }
}
