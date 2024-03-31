package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.TodayBPException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service("TODAY_BP")
public class TodayBPService implements MessageService<TodayBPService.TodayBPParam> {
    private static final Logger log = LoggerFactory.getLogger(TodayBPService.class);
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    BindDao bindDao;
    ImageService imageService;

    public record TodayBPParam(BinUser user, OsuMode mode, int day, boolean isMyself) {}

    @Autowired
    public TodayBPService(OsuUserApiService userApiService, OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<TodayBPParam> data) throws Throwable {
        var matcher = Instructions.TODAY_BP.matcher(messageText);
        if (!matcher.find()) return false;

        var name = matcher.group("name");
        var dayStr = matcher.group("day");

        // 时间计算
        int day = 1;

        if (StringUtils.hasText(dayStr)) {
            try {
                day = Integer.parseInt(dayStr);
            } catch (NumberFormatException ignored) {

            }
        }

        if (day > 999) throw new TodayBPException(TodayBPException.Type.TBP_BP_TooLongAgo);

        // 传递其他参数
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new TodayBPParam(
                    new BinUser(at.getTarget(), messageText.toLowerCase()), mode, day, false));
            return true;
        }
        if (Objects.nonNull(qq)) {
            data.setValue(new TodayBPParam(
                    new BinUser(Long.parseLong(qq), messageText.toLowerCase()), mode, day, false));
            return true;
        }
        if (Strings.isNotBlank(name)) {
            long id;
            var user = new BinUser();

            try {
                id = userApiService.getOsuId(name);
            } catch (WebClientResponseException.NotFound e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound);
            }
            user.setOsuID(id);
            user.setMode(mode);
            data.setValue(new TodayBPParam(user, mode, day, false));
            return true;
        } else {
            data.setValue(new TodayBPParam(
                    new BinUser(event.getSender().getId(), messageText.toLowerCase()), mode, day, true));
            return true;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, TodayBPParam param) throws Throwable {
        var from = event.getSubject();

        List<Score> BPs;
        List<Score> TodayBPs = new ArrayList<>();
        OsuUser user;
        var rankList = new ArrayList<Integer>();

        try {
            user = userApiService.getPlayerInfo(param.user, param.mode);
            BPs = scoreApiService.getBestPerformance(param.user, param.mode, 0, 100);
        } catch (Exception e) {
            throw new TodayBPException(TodayBPException.Type.TBP_Me_TokenExpired);
        }

        if (CollectionUtils.isEmpty(BPs)) throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP);

        //挑出来符合要求的

        LocalDateTime dayBefore = LocalDateTime.now().minusDays(param.day);

        for (int i = 0; i < BPs.size(); i++) {
            var bp = BPs.get(i);

            if (dayBefore.isBefore(bp.getCreateTime())){
                TodayBPs.add(bp);
                rankList.add(i + 1);
            }
        }

        //没有的话
        if (TodayBPs.isEmpty()){
            if (param.day <= 1) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_No24H);
            } else {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoPeriod);
            }
        }

        byte[] image;

        try {
            image = imageService.getPanelA4(user, TodayBPs, rankList);
        } catch (Exception e) {
            log.error("今日最好成绩：图片渲染失败", e);
            throw new TodayBPException(TodayBPException.Type.TBP_Fetch_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("今日最好成绩：发送失败", e);
            throw new TodayBPException(TodayBPException.Type.TBP_Send_Error);
        }

    }

}
