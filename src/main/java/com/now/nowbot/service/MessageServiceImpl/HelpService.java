package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.Pattern4ServiceImpl;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("HELP")
public class HelpService implements MessageService<Matcher> {

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.HELP.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean isSendLink = true; //这是防止web不能用，临时关闭的布尔值
        var from = event.getSubject();
        String module = matcher.group("module").trim().toLowerCase(); //传东西进来
        String path = switch (module) {
            case "bot", "b" -> "help-bot.png";
            case "score", "s" -> "help-score.png";
            case "player", "p" -> "help-player.png";
            case "map", "m" -> "help-map.png";
            case "chat", "c" -> "help-chat.png";
            case "fun", "f" -> "help-fun.png";
            case "aid", "a" -> "help-aid.png";
            case "tournament", "t" -> "help-tournament.png";
            case "" -> "help-default.png";
            default -> "";
        };

        boolean isSendPic = !path.isEmpty();

        String web = "https://docs.365246692.xyz/help/";
        String link = switch (module) {
            case "bot", "b" -> "bot";
            case "score", "s" -> "score";
            case "player", "p" -> "player";
            case "map", "m" -> "map";
            case "chat", "c" -> "chat";
            case "fun", "f" -> "fun";
            case "aid", "a" -> "aid";
            case "tournament", "t" -> "tournament";
            default -> "";
        };

        //这个是细化的功能
        String link2 = switch (module) {
            case "help", "h" -> "bot.html#help";
            case "ping", "pi" -> "bot.html#ping";
            case "bind", "bi" -> "bot.html#bind";
            case "ban", "bq", "bu", "bg" -> "bot.html#ban";
            case "switch", "sw" -> "bot.html#switch";
            case "antispam", "as" -> "bot.html#antispam";

            case "mode", "setmode", "sm", "mo" -> "score.html#mode";
            case "pass", "pr" -> "score.html#pass";
            case "recent", "re" -> "score.html#recent";
            case "score", "s" -> ""; //这个会和上面重复
            case "bestperformance", "bp" -> "score.html#bestperformance";
            case "todaybp", "tbp" -> "score.html#todaybp";
            case "bpanalysis", "bpa", "ba" -> "score.html#bpanalysis";

            case "information", "info", "i" -> "player.html#info";
            case "immapper", "imapper", "im" -> "player.html#immapper";
            case "friend", "friends", "fr" -> "player.html#friend";
            case "mutual", "mu" -> "player.html#mutual";
            case "ppminus", "ppm", "pm" -> "player.html#ppminus";
            case "ppplus", "ppp" -> "player.html#ppplus";

            case "map", "m" -> ""; //这个会和上面重复
            case "audio", "song", "au" -> "map.html#audio";
            case "search", "sh" -> "map.html#search";
            case "course", "c" -> "map.html#course";
            case "danacc", "da" -> "map.html#danacc";
            case "qualified", "q" -> "map.html#qualified";
            case "leader", "l" -> "map.html#leader";

            case "match", "ma" -> "tournament.html#match";
            case "rating", "mra", "ra" -> "tournament.html#rating";
            case "monitornow", "mn" -> "tournament.html#monitornow";
            case "round", "ro", "mr" -> "tournament.html#round";

            case "oldavatar", "oa" -> "aid.html#oldavatar";
            case "overrating", "oversr", "or" -> "aid.html#overrating";
            case "kita", "k" -> "aid.html#kita";

            default -> "";
        };
        
        if (isSendPic) {
            QQMsgUtil.sendImage(from, Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve("ExportFileV3/" + path)));
        }

        if (isSendLink) {
            String msg = "";

            if (link.isEmpty() && link2.isEmpty()) {
                msg =  "请参阅：" + web;
            } else if (!link.isEmpty() && link2.isEmpty()) {
                msg = "请参阅：" + web + link + ".html";
            } else if (link.isEmpty()) {
                msg = "请参阅功能介绍：" + web + link2;
            }

            if (!msg.isEmpty()) {
                var receipt = from.sendMessage(msg);
                //默认110秒后撤回
                from.recallIn(receipt, 110 * 1000);
            }
        }
    }
}
