package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.regex.Matcher;

@Service("d")
public class A2Service implements MessageService {
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService imageService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        // 获取参数

        var parm = new HashMap<String, Object>();
        parm.put("key", "value");
        parm.put("no value key", null);

        try {
            var d =  osuGetService.serchBeatmap(parm);
            d.setRule("rule");
            var img = imageService.getPanelA2(d);
            event.getSubject().sendImage(img);
        } catch (Exception e) {
            throw new RuntimeException("出错了");
        }
    }
}
