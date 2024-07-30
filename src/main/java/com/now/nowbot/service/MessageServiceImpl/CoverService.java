package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("GET_COVER")
public class CoverService implements MessageService<Matcher> {
    @Resource
    OsuBeatmapApiService beatmapApiService;
    Pattern pattern = Pattern.compile("!gc\\s*(?<type>[clds][1,2]?)?\\s*(?<bid>\\d+)");

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var matcher = pattern.matcher(messageText);
        if (!matcher.find()) {
            return false;
        }

        data.setValue(matcher);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher data) throws Throwable {
        var bid = Long.parseLong(data.group("bid"));
        var b = beatmapApiService.getBeatMapInfoFromDataBase(bid);
        var cover = b.getBeatMapSet().getCovers();
        String url = switch (data.group("type")) {
            case "l", "l1" -> cover.getList();
            case "d", "d1" -> cover.getCard();
            case "s", "s1" -> cover.getSlimcover();
            case "c2" -> cover.getCover2x();
            case "l2" -> cover.getList2x();
            case "d2" -> cover.getCard2x();
            case "s2" -> cover.getSlimcover2x();
            case null, default -> cover.getCover();
        };
        var urlObj = URI.create(url).toURL();
        event.getSubject().sendMessage(new MessageChain.MessageChainBuilder().addImage(urlObj).build());
    }
}
