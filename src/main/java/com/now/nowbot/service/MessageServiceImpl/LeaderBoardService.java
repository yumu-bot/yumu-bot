package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.LeaderBoardException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("Leader")
public class LeaderBoardService implements MessageService<Matcher> {
    OsuGetService osuGetService;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public LeaderBoardService (OsuGetService osuGetService, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.template = template;
        imageService = image;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(friendlegacy|fl(?![a-zA-Z_]))+(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        long bid;
        int range;

        if (matcher.group("bid") == null) throw new LeaderBoardException(LeaderBoardException.Type.LIST_Parameter_BidError);
        try {
            bid = Long.parseLong(matcher.group("bid"));
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
            beatMap = osuGetService.getBeatMapInfo(bid);
            isRanked = beatMap.isRanked();
        } catch (Exception e) {
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Map_NotFound);
        }

        try {
            // Mode 新增一个默认处理,以后用这个
            // if (matcher.group("mode") == null) mode = OsuMode.getMode(beatMap.getMode());
            mode = OsuMode.getMode(matcher.group("mode"), beatMap.getMode());
            scores = osuGetService.getBeatmapScores(bid, mode);
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
            var data = imageService.getPanelA3(beatMap, subScores);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("Leader", e);
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
