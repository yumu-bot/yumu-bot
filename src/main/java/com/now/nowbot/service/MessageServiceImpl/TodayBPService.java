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
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.TodayBPException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    ImageService imageService;
    @Resource
    BindDao bindDao;

    public record TodayBPParam(BinUser user, OsuMode mode, int day, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<TodayBPParam> data) throws Throwable {
        var matcher = Instructions.TODAY_BP.matcher(messageText);
        if (!matcher.find()) return false;

        var name = matcher.group("name");
        var dayStr = matcher.group("day");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        // 时间计算
        int day = 1;

        var noSpaceAtEnd = StringUtils.hasText(name) && ! name.endsWith(" ");

        if (StringUtils.hasText(dayStr)) {
            if (noSpaceAtEnd) {
                // 如果名字后面没有空格，并且有 n 匹配，则主观认为后面也是名字的一部分（比如 !t lolol233）
                name += dayStr;
                dayStr = "";
            } else {
                // 如果输入的有空格，并且有名字，后面有数字，则主观认为后面的是天数（比如 !t osu 420），如果找不到再合起来
                // 没有名字，但有 n 匹配的也走这边 parse
                try {
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException e) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
            }
        }

        //避免 !b 970 这样子被错误匹配
        if (day < 1 || day > 999) {
            if (hasHash) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_TooLongAgo);
            }

            if (StringUtils.hasText(name)) {
                name += dayStr;
            } else {
                name = dayStr;
            }

            dayStr = "";
            day = 1;
        }

        // 传递其他参数
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qqStr = matcher.group("qq");

        if (Objects.nonNull(at)) {
            try {
                data.setValue(new TodayBPParam(
                        bindDao.getUserFromQQ(at.getTarget()), mode, day, false));
                return true;
            } catch (BindException e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_TokenExpired);
            }
        }
        if (Objects.nonNull(qqStr)) {
            try {
                data.setValue(new TodayBPParam(
                        bindDao.getUserFromQQ(Long.parseLong(qqStr)), mode, day, false));
                return true;
            } catch (BindException e) {
                throw new TodayBPException(TodayBPException.Type.TBP_QQ_NotFound, qqStr);
            }
        }
        if (Strings.isNotBlank(name)) {
            long id;
            var user = new BinUser();

            try {
                id = userApiService.getOsuId(name.trim());
            } catch (WebClientResponseException.NotFound e) {
                if (StringUtils.hasText(dayStr)) {
                    // 补救机制 1
                    try {
                        id = userApiService.getOsuId(name.concat(dayStr));
                    } catch (WebClientResponseException.NotFound e1) {
                        throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound, name.concat(dayStr));
                    }
                } else {
                    throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound, name.trim());
                }
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound, name.trim());
            }
            user.setOsuID(id);
            user.setMode(mode);
            data.setValue(new TodayBPParam(user, mode, day, false));
            return true;
        } else {
            try {
                data.setValue(
                        new TodayBPParam(bindDao.getUserFromQQ(event.getSender().getId()), mode, day, true));
                return true;
            } catch (BindException e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Me_TokenExpired);
            }
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, TodayBPParam param) throws Throwable {
        var from = event.getSubject();

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
            throw new TodayBPException(TodayBPException.Type.TBP_List_FetchError);
        }

        if (CollectionUtils.isEmpty(BPs)) {
            if (param.day <= 1) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP, param.user.getOsuName());
            }

            // 补救机制 2
            try {
                var id = userApiService.getOsuId(param.user.getOsuName() + param.day);
                user = userApiService.getPlayerInfo(id, param.mode);
                BPs = scoreApiService.getBestPerformance(id, param.mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP, param.user.getOsuName() + param.day);
            }
        }

        //筛选
        LocalDateTime dayBefore = LocalDateTime.now().minusDays(param.day);

        for (int i = 0; i < BPs.size(); i++) {
            var bp = BPs.get(i);

            if (dayBefore.isBefore(bp.getCreateTime())){
                todayBPs.add(bp);
                BPRanks.add(i + 1);
            }
        }

        //没有的话
        if (todayBPs.isEmpty()) {
            if (! user.getActive()) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_Inactive, user.getUsername());
            }
            if (param.day <= 1) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_No24H, user.getUsername());
            } else {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoPeriod, user.getUsername());
            }
        }

        byte[] image;

        try {
            image = imageService.getPanelA4(user, todayBPs, BPRanks);
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
