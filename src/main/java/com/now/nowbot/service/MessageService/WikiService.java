package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.throwable.LogException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("wiki")
public class WikiService implements MessageService{
    static JSONObject WIKI;
    WikiService(){
        String datestr = null;
        try {
            datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH,"wiki.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        WIKI = JSONObject.parseObject(datestr);
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String key = matcher.group("key");
        var msg = event.getSubject().sendMessage(getWiki(key));
        msg.recallIn(60*1000);
    }
    String getWiki(String key) throws IOException, LogException {
        StringBuffer sb = new StringBuffer();
        if (null == key || "null".equals(key) || "".equals(key.trim()) || "index".equals(key)) {
            if (WIKI == null){
                String datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH +"wiki.json"));
                WIKI = JSONObject.parseObject(datestr);
            }
            var dates = WIKI.getJSONArray("index");
            for (int i = 0; i < dates.size(); i++) {
                var jdate = dates.getJSONObject(i);
                jdate.forEach((name, array)->{
                    sb.append(name).append(':').append('\n');
                    JSONArray array1 = (JSONArray) array;
                    for (int j = 0; j < array1.size(); j++) {
                        sb.append(" ")
                                .append(array1.getString(j));
                    }
                    sb.append('\n');
                });
            }
        }else {
            key = key.toUpperCase();
            String r = WIKI.getString(key);
            if (r == null) throw new LogException("没有找到"+key,null);
            sb.append(key).append(':').append('\n');
            sb.append(r);
        }
        return sb.toString();
    }
}
