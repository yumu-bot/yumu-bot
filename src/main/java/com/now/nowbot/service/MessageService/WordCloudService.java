package com.now.nowbot.service.MessageService;

import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("word-cloud")
public class WordCloudService implements MessageService{
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        //todo 词云 https://github.com/kennycason/kumo
    }
}
