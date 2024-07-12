package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Service("TODAY_BP")
public class TodayBPService implements MessageService<TodayBPService.TodayBPParam> {
    private static final Logger log = LoggerFactory.getLogger(TodayBPService.class);
    @Resource
    ImageService imageService;

    public record TodayBPParam(OsuUser user, OsuMode mode, Map<Integer, Score> scores, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<TodayBPParam> data) throws Throwable {
        var matcher = Instructions.TODAY_BP.matcher(messageText);
        if (!matcher.find()) return false;
        data.setValue(HandleUtil.getTodayBPList(event, matcher));
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

        /*

        List<Score> BPs;
        List<Score> todayBPs = new ArrayList<>();
        OsuUser user;
        var BPRanks = new ArrayList<Integer>();

        try {
            user = userApiService.getPlayerInfo(param.user, param.mode);
        } catch (Exception e) {
            throw new TodayBPException(TodayBPException.Type.TBP_Me_TokenExpired);
        }

        try {
            BPs = scoreApiService.getBestPerformance(param.user, param.mode, 0, 100);
        } catch (Exception e) {
            log.error("今日最好成绩：获取列表失败");
            throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_List);
        }

        if (CollectionUtils.isEmpty(BPs)) {
            if (param.day <= 1) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.user.getOsuName());
            }

            // 补救机制 2
            try {
                var id = userApiService.getOsuId(param.user.getOsuName() + param.day);
                user = userApiService.getPlayerInfo(id, param.mode);
                BPs = scoreApiService.getBestPerformance(id, param.mode, 0, 100);
            } catch (Exception e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.user.getOsuName() + param.day);
            }
        }

        //筛选
        LocalDateTime dayBefore = LocalDateTime.now().minusDays(param.day);

        for (int i = 0; i < BPs.size(); i++) {
            var bp = BPs.get(i);

            if (dayBefore.isBefore(bp.getCreateTimePretty())){
                todayBPs.add(bp);
                BPRanks.add(i + 1);
            }
        }

        //没有的话
        if (todayBPs.isEmpty()) {
            if (! user.getActive()) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerInactive, user.getUsername());
            }
            if (param.day <= 1) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Empty_TodayBP, user.getUsername());
            } else {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Empty_PeriodBP, user.getUsername());
            }
        }

         */

            var rankList = new ArrayList<Integer>();
            var todayList = new ArrayList<Score>();
            for (var e : todayMap.entrySet()) {
                rankList.add(e.getKey() + 1);
                todayList.add(e.getValue());
            }

        byte[] image;

        try {
            image = imageService.getPanelA4(user, todayList, rankList);
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
