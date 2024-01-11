package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("HELP")
public class HelpService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(HelpService.class);

    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.HELP.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        String module = matcher.group("module").trim().toLowerCase(); //传东西进来

        try {
            var pic = getHelpPicture(module, imageService);

            if (Objects.nonNull(pic)) {
                from.sendImage(pic);
            } else {
                throw new TipsException("窝趣，找不到文件");
            }
        } catch (Exception e) {
            log.error("Help A6 输出错误，使用默认方法", e);

            var picLegacy = getHelpPictureLegacy(module);
            var msgLegacy = getHelpLinkLegacy(module);

            if (Objects.nonNull(picLegacy)) {
                from.sendImage(picLegacy);
            }

            if (Objects.nonNull(msgLegacy)) {
                var receipt = from.sendMessage(msgLegacy);
                //默认110秒后撤回
                from.recallIn(receipt, 110 * 1000);
            }
        }

    }

    /**
     * 目前的 help 方法，走 panel A6
     * @param module 需要查询的功能名字
     * @return 图片流
     */
    private static byte[] getHelpPicture(String module, ImageService imageService) {
        String fileName = switch (module) {
            case "bot", "b" -> "bot";
            case "score", "s" -> "score";
            case "player", "p" -> "player";
            case "map", "m" -> "map";
            case "chat", "c" -> "chat";
            case "fun", "f" -> "fun";
            case "aid", "a" -> "aid";
            case "tournament", "t" -> "tournament";
            
            case "help", "h" -> "help";
            case "ping", "pi" -> "ping";
            case "bind", "bi" -> "bind";
            case "ban", "bq", "bu", "bg" -> "ban";
            case "switch", "sw" -> "switch";
            case "antispam", "as" -> "antispam";

            case "mode", "setmode", "sm", "mo" -> "mode";
            case "pass", "pr" -> "pass";
            case "recent", "re" -> "recent";
            case "scores" -> "scores";
            case "bestperformance", "bp" -> "bestperformance";
            case "todaybp", "tbp" -> "todaybp";
            case "bpanalysis", "bpa", "ba" -> "bpanalysis";

            case "information", "info", "i" -> "info";
            case "immapper", "imapper", "im" -> "immapper";
            case "friend", "friends", "fr" -> "friend";
            case "mutual", "mu" -> "mutual";
            case "ppminus", "ppm", "pm" -> "ppminus";
            case "ppplus", "ppp" -> "ppplus";

            case "maps" -> "maps";
            case "audio", "song", "au" -> "audio";
            case "search", "sh" -> "search";
            case "course", "co" -> "course";
            case "danacc", "da" -> "danacc";
            case "qualified", "q" -> "qualified";
            case "leader", "l" -> "leader";

            case "match", "ma" -> "match";
            case "rating", "mra", "ra" -> "rating";
            case "series", "sra", "sa" -> "series";
            case "matchlisten", "listen", "ml", "li" -> "listen";
            case "matchnow", "now", "mn" -> "matchnow";
            case "matchround", "round", "ro", "mr" -> "round";
            case "mappool", "pool", "po" -> "pool";

            case "oldavatar", "oa" -> "oldavatar";
            case "overrating", "oversr", "or" -> "overrating";
            case "trans", "tr" -> "trans";
            case "kita", "k" -> "kita";

            case null, default -> "";
        };
        
        StringBuilder sb = new StringBuilder();

        try {
            fileName += ".md";
            var bufferedReader = Files.newBufferedReader(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("Help").resolve(fileName)
            );

            // 逐行读取文本内容
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            // 关闭流
            bufferedReader.close();

            return imageService.getPanelA6(sb.toString(), "help");
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 老旧的 help 方法，可以备不时之需
     * @param module 需要查询的功能名字
     * @return 图片流
     */
    private static byte[] getHelpPictureLegacy(@Nullable String module) {
        String fileName = switch (module) {
            case "bot", "b" -> "help-bot";
            case "score", "s" -> "help-score";
            case "player", "p" -> "help-player";
            case "map", "m" -> "help-map";
            case "chat", "c" -> "help-chat";
            case "fun", "f" -> "help-fun";
            case "aid", "a" -> "help-aid";
            case "tournament", "t" -> "help-tournament";
            case "" -> "help-default";
            case null -> "help-default";
            default -> "";
        };

        if (fileName.isEmpty()) return null;

        byte[] file;

        try {
            fileName += ".png";
            file = Files.readAllBytes(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(fileName)
            );
        } catch (IOException e) {
            return null;
        }

        return file;
    }

    /**
     * 老旧的 help 方法，可以备不时之需
     * @param module 需要查询的功能名字
     * @return 请参阅：link
     */
    private static String getHelpLinkLegacy(@Nullable String module) {
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
            case null, default -> "";
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
            case "scorehelp" -> "score.html#score";
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
            case "maphelp" -> "map.html#map";
            case "audio", "song", "au" -> "map.html#audio";
            case "search", "sh" -> "map.html#search";
            case "course", "c" -> "map.html#course";
            case "danacc", "da" -> "map.html#danacc";
            case "qualified", "q" -> "map.html#qualified";
            case "leader", "l" -> "map.html#leader";

            case "match", "ma" -> "tournament.html#match";
            case "rating", "mra", "ra" -> "tournament.html#rating";
            case "series", "sra", "sa" -> "tournament.html#series";
            case "matchlisten", "listen", "ml", "li" -> "tournament.html#listen";
            case "matchnow", "now", "mn" -> "tournament.html#matchnow";
            case "matchround", "round", "ro", "mr" -> "tournament.html#round";
            case "mappool", "pool", "po" -> "tournament.html#pool";

            case "oldavatar", "oa" -> "aid.html#oldavatar";
            case "overrating", "oversr", "or" -> "aid.html#overrating";
            case "trans", "tr" -> "aid.html#trans";
            case "kita", "k" -> "aid.html#kita";

            case null, default -> "";
        };

        if (link.isEmpty() && link2.isEmpty()) {
            return STR."请参阅：\{web}";
        } else if (!link.isEmpty() && link2.isEmpty()) {
            return STR."请参阅：\{web}\{link}.html";
        } else if (link.isEmpty()) {
            return STR."请参阅功能介绍：\{web}\{link2}";
        } else {
            return null;
        }
    }
}
