package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MapScoreListException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("MSList")
public class MapScoreListService implements MessageService {
    OsuGetService osuGetService;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public MapScoreListService (OsuGetService osuGetService, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.template = template;
        imageService = image;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        long bid = 0L;
        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (Exception e) {
            throw new MapScoreListException(MapScoreListException.Type.LIST_Map_BidError);
        }

        throw new MapScoreListException(MapScoreListException.Type.LIST_Map_NotRanked);

    }
}
