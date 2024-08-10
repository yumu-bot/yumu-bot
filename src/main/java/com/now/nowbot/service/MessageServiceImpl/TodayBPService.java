package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("TODAY_BP")
public class TodayBPService implements MessageService<TodayBPService.TodayBPParam> {
    private static final Logger log = LoggerFactory.getLogger(TodayBPService.class);
    @Resource
    ImageService       imageService;
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    public record TodayBPParam(OsuUser user, OsuMode mode, Map<Integer, Score> scores, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<TodayBPParam> data) throws Throwable {
        var matcher = Instruction.TODAY_BP.matcher(messageText);
        if (!matcher.find()) return false;
        var mode = CmdUtil.getMode(matcher);
        var isMyself = new AtomicBoolean();
        var range = CmdUtil.getUserWithRange(event, matcher, mode, isMyself);
        var user = range.getData();
        int dayStart = range.getValue(1, false) - 1;
        int dayEnd = range.getValue(1, true);
        dayStart = Math.min(0, dayStart);
        dayEnd = Math.max(dayEnd, dayStart + 1);

        if (Objects.isNull(user)) {
            throw new TipsException("没找到玩家");
        }

        List<Score> bpList;
        try {
            bpList = scoreApiService.getBestPerformance(user.getUserID(), mode.getData(), 0, 100);
        } catch (WebClientResponseException.Forbidden e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user.getUsername());
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.getUsername());
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取今日最好成绩失败！", e);
            throw new TipsException("HandleUtil：获取今日最好成绩失败！");
        }
        LocalDateTime laterDay = LocalDateTime.now().minusDays(dayStart);
        LocalDateTime earlierDay = LocalDateTime.now().minusDays(dayEnd);
        var dataMap = new TreeMap<Integer, Score>();

        bpList.forEach(
                ContextUtil.consumerWithIndex(
                        (s, index) -> {
                            if (s.getCreateTimePretty().isBefore(laterDay) && s.getCreateTimePretty().isAfter(earlierDay)) {
                                dataMap.put(index, s);
                            }
                        }
                )
        );
        var param = new TodayBPParam(user, mode.getData(), dataMap, isMyself.get());
        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, TodayBPParam param) throws Throwable {
        var from = event.getSubject();

        var todayMap = param.scores();
        var mode = param.mode();
        var user = param.user();

        if (CollectionUtils.isEmpty(todayMap)) {
            if (! user.getActive()) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerInactive, user.getUsername());
            }
            throw new GeneralTipsException(GeneralTipsException.Type.G_Empty_PeriodBP, param.user().getUsername(), mode);
        }

        var ranks = new ArrayList<Integer>();
        var scores = new ArrayList<Score>();
        for (var e : todayMap.entrySet()) {
            ranks.add(e.getKey() + 1);
            scores.add(e.getValue());
        }

        beatmapApiService.applySRAndPP(scores);

        byte[] image;

        try {
            image = imageService.getPanelA4(user, scores, ranks);
        } catch (Exception e) {
            log.error("今日最好成绩：图片渲染失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "今日最好成绩");
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("今日最好成绩：发送失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "今日最好成绩");
        }

    }

}
