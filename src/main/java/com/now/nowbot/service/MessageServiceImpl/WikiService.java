package com.now.nowbot.service.MessageServiceImpl;


import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiService implements MessageService<Matcher> {
    Logger log = LoggerFactory.getLogger(WikiService.class);
    static JsonNode WIKI;
    WikiService(){
        String datestr = "";
        try {
            datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH,"wiki.json"));
        } catch (IOException e) {
            log.info("未找到wiki文件路径, 加载失败");
        }
        WIKI = JacksonUtil.jsonToObject(datestr, JsonNode.class);
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Pattern.compile("^[!！]\\s*(?i)(ym)?(wiki)\\s*(?<key>\\s*)?").matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String key = matcher.group("key");
        var msg = event.getSubject().sendMessage(getWiki(key));
        event.getSubject().recallIn(msg, 60*1000);
    }
    String getWiki(String key) throws IOException, LogException {
        StringBuffer sb = new StringBuffer();
        if (null == key || "null".equals(key) || "".equals(key.trim()) || "index".equals(key)) {
            if (WIKI == null){
                String datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH +"wiki.json"));
                WIKI = JacksonUtil.jsonToObject(datestr, JsonNode.class);
            }
            var dates = WIKI.get("index");
            for (Iterator<Map.Entry<String, JsonNode>> it = dates.fields(); it.hasNext(); ) {
                var m = it.next();
                sb.append(m.getKey()).append(':').append('\n');
                if (m.getValue().isArray()){
                    for (int i = 0; i < m.getValue().size(); i++) {
                        sb.append(" ")
                                .append(m.getValue().get(i).asText());
                    }
                }
                sb.append('\n');
            }
        }else {
            key = key.toUpperCase();
            String r = WIKI.findValue(key).asText();
            if (r == null) throw new LogException(STR."没有找到\{key}");
            sb.append(key).append(':').append('\n');
            sb.append(r);
        }
        return sb.toString();
    }
}
