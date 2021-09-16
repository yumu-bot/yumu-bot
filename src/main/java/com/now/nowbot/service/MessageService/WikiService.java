package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("wiki")
public class WikiService implements MessageService{
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String key = matcher.group("key");
        var msg = event.getSubject().sendMessage(getWiki(key));
        msg.recallIn(60*1000);
    }
    String getWiki(String key) throws IOException, TipsException {
        String datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH +"wiki.json"));
        JSONObject date = JSONObject.parseObject(datestr);
        StringBuffer sb = new StringBuffer();
        if ("".equals(key)||"index".equals(key)) {
            var dates = date.getJSONArray("index");
            for (int i = 0; i < dates.size(); i++) {
                var jdate = dates.getJSONObject(i);
                jdate.forEach((name, array)->{
                    sb.append(name).append(':').append('\n');
                    JSONArray array1 = (JSONArray) array;
                    for (int j = 0; j < array1.size(); j++) {
                        sb.append("   ")
                                .append(array1.getString(j))
                                .append(' ');
                    }
                });
            }
        }else {
            String r = date.getString(key);
            if (r == null) throw new TipsException("没有找到"+key);
            sb.append(key).append(':').append('\n');
            sb.append(r);
        }
        return sb.toString();
    }
}
