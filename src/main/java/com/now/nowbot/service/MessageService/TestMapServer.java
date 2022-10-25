package com.now.nowbot.service.MessageService;

import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Matcher;

@Service("test-map")
public class TestMapServer implements MessageService{
    @Resource
    OsuGetService osuGetService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int bid = Integer.parseInt(matcher.group("d"));

        var data = osuGetService.getAttributes(bid);
    }
}
