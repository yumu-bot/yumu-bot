package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.throwable.ServiceException.LeaderBoardException;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Matcher;

@Service("LEADER_BOARD")
public class LeaderBoardService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(LeaderBoardService.class);
    OsuBeatmapApiService beatmapApiService;
    OsuScoreApiService scoreApiService;
    ImageService imageService;

    @Autowired
    public LeaderBoardService(OsuBeatmapApiService beatmapApiService, OsuScoreApiService scoreApiService, ImageService image) {
        this.beatmapApiService = beatmapApiService;
        this.scoreApiService = scoreApiService;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.LEADER_BOARD.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        long BID;
        int range;
        var BIDstr = matcher.group("bid");

        if (BIDstr == null || BIDstr.isBlank()) throw new LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_NoBid);

        try {
            BID = Long.parseLong(BIDstr);
        } catch (NumberFormatException e) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_BidError);
        }

        if (matcher.group("range") == null) {
            range = 50;
        } else {
            try {
                range = Integer.parseInt(matcher.group("range"));
            } catch (NumberFormatException e) {
                throw new LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_RangeError);
            }

            if (range < 1) range = 1;
            else if (range > 50) range = 50;
        }


        OsuMode mode;
        List<Score> scores;
        BeatMap beatMap;
        boolean isRanked;

        try {
            beatMap = beatmapApiService.getBeatMapInfo(BID);
            isRanked = beatMap.hasLeaderBoard();
        } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Map_NotFound);
        } catch (Exception e) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Map_FetchFailed);
        }

        try {
            // Mode 新增一个默认处理,以后用这个
            // if (matcher.group("mode") == null) mode = OsuMode.getMode(beatMap.getMode());
            mode = OsuMode.getMode(matcher.group("mode"), beatMap.getMode());
            scores = scoreApiService.getBeatmapScores(BID, mode);
        } catch (Exception e) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Score_FetchFailed);
        }

        if (!isRanked) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Map_NotRanked);
        }

        // 对 可能 null 以及 empty 的用这玩意判断
        if (CollectionUtils.isEmpty(scores)) throw new LeaderBoardException(LeaderBoardException.Type.LIST_Score_NotFound);

        List<Score> subScores;
        if (range > scores.size()) range = scores.size();
        subScores = scores.subList(0, range);

        try {
            var image = imageService.getPanelA3(beatMap, subScores);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("Leader", e);
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Send_Error);
        }
    }
}
