package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("help")
public class HelpService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
//        StringBuilder sb = new StringBuilder();
//        for(var ins: Instruction.values()) {
//            if(ins.getDesc()!=null)
//                sb.append(ins.getDesc()).append("\n");
//        }
//        from.sendMessage(sb.toString());
//        QQMsgUtil.sendImage(from,Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve("ExportFileV3/panel-help.png")));

        String module = ""; //传东西进来
        String path;
        switch (module) {
            case "bot", "b" : path = "help-bot.png"; break;
            case "score", "s" : path = "help-score.png"; break;
            case "player", "p" : path = "help-player.png"; break;
            case "map", "m" : path = "help-map.png"; break;
            case "custom", "c" : path = "help-custom.png"; break;
            case "fun", "f" : path = "help-fun.png"; break;
            case "aid", "a" : path = "help-aid.png"; break;
            case "tournament", "t" : path = "help-tournament.png"; break;
            case default : path = "help-default.png"; break;
        }

        path = "ExportFileV3/" + path;

        QQMsgUtil.sendImage(from,Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve(path)));
    }
}
