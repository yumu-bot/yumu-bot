package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("TODAYBP")
public class TodayBPService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(TodayBPService.class);
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public TodayBPService(OsuUserApiService userApiService, OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.TODAYBP.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var mode = OsuMode.getMode(matcher.group("mode"));
        var name = matcher.group("name");
        var day = matcher.group("day");

        if (day == null) day = "1";
        if (name == null) name = "";

        List<Score> bpAllList;
        List<Score> bpList = new ArrayList<>();
        OsuUser ouMe;
        var rankList = new ArrayList<Integer>();

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (at != null) {
            try {
                var bUser = bindDao.getUserFromQQ(at.getTarget());
                if (mode == OsuMode.DEFAULT) mode = bUser.getMode();
                ouMe = userApiService.getPlayerInfo(bUser, mode);
                bpAllList = scoreApiService.getBestPerformance(bUser, mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_FetchFailed);
            }
        } else if (!name.isEmpty()) {
            try {
                ouMe = userApiService.getPlayerInfo(name, mode);
                bpAllList = scoreApiService.getBestPerformance(ouMe.getUID(), mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound);
            }
        } else {
            try {
                var buMe = bindDao.getUserFromQQ(event.getSender().getId());
                if (mode == OsuMode.DEFAULT) mode = buMe.getMode();
                ouMe = userApiService.getPlayerInfo(buMe, mode);
                bpAllList = scoreApiService.getBestPerformance(buMe, mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Me_TokenExpired);
            }
        }

        if (CollectionUtils.isEmpty(bpAllList)) throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP);

        // 时间计算
        int _day = -1;

        if (!day.isEmpty()){
            _day = - Integer.parseInt(day);
        }
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(_day);

        if (_day > 999) throw new TodayBPException(TodayBPException.Type.TBP_BP_TooLongAgo);

        //挑出来符合要求的
        for (int i = 0; i < bpAllList.size(); i++) {
            var bp = bpAllList.get(i);

            // || user.getId().equals(17064371L) 你真的需要用这个功能？ !b 1-100
            if (dayBefore.isBefore(bp.getCreateTime())){
                bpList.add(bp);
                rankList.add(i + 1);
            }
        }
        //没有的话
        if (bpList.isEmpty()){
            if (day.isEmpty() || Objects.equals(day, "1")) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_No24H);
            } else {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoPeriod);
            }
        }

        try {
            var data = imageService.getPanelA4(ouMe, bpList, rankList);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("TBP Error: ", e);
            throw new TodayBPException(TodayBPException.Type.TBP_Send_Error);
        }

    }

}
