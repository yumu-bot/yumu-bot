package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MapScoreListException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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
        var from = event.getSubject();

        long bid;

        if (matcher.group("bid") == null) throw new MapScoreListException(MapScoreListException.Type.LIST_Map_BidError);
        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (NumberFormatException e) {
            throw new MapScoreListException(MapScoreListException.Type.LIST_Map_BidError);
        }

        var mode = OsuMode.getMode(matcher.group("mode"));

        List<Score> scores;
        BeatMap beatMap;
        String status;

        try {
            beatMap = osuGetService.getMapInfo(bid);
        } catch (Exception e) {
            throw new MapScoreListException(MapScoreListException.Type.LIST_Map_NotFound);
        }

        try {
            status = beatMap.getStatus();

            if (mode == OsuMode.DEFAULT) mode = OsuMode.getMode(beatMap.getMode());
            scores = osuGetService.getBeatmapScores(bid, mode);

        } catch (NullPointerException e) {
            throw new MapScoreListException(MapScoreListException.Type.LIST_Map_NotFound);
        }

        if (!(status.equals("ranked") || status.equals("qualified") || status.equals("loved"))) {
            throw new MapScoreListException(MapScoreListException.Type.LIST_Map_NotRanked);
        }

        if (scores == null) throw new MapScoreListException(MapScoreListException.Type.LIST_Score_NotFound);

        try {
            var data = imageService.getPanelA3(beatMap, scores);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("MSList", e);
            throw new MapScoreListException(MapScoreListException.Type.LIST_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
