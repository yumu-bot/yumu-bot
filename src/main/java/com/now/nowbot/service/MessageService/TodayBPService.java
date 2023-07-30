package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.TodayBPException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("TodayBP")
public class TodayBPService implements MessageService{
    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public TodayBPService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var mode = OsuMode.getMode(matcher.group("mode"));
        var name = matcher.group("name");

        List<Score> bpAllList;
        List<Score> bpList = new ArrayList<>();
        OsuUser ouMe;
        var rankList = new ArrayList<Integer>();

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (at != null) {
            try {
                var bUser = bindDao.getUser(at.getTarget());
                ouMe = osuGetService.getPlayerInfo(bUser, mode);
                bpAllList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_FetchFailed);
            }
        } else if (!name.isEmpty()) {
            try {
                ouMe = osuGetService.getPlayerInfo(name, mode);
                bpAllList = osuGetService.getBestPerformance(ouMe.getId(), mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Player_NotFound);
            }
        } else {
            try {
                var bUser = bindDao.getUser(event.getSender().getId());
                ouMe = osuGetService.getPlayerInfo(bUser, mode);
                bpAllList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
            } catch (Exception e) {
                throw new TodayBPException(TodayBPException.Type.TBP_Me_LoseBind);
            }
        }

        if (CollectionUtils.isEmpty(bpAllList)) throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP);

        // 时间计算
        int day = -1;
        if (matcher.group("day") != null){
            day = - Integer.parseInt(matcher.group("day"));
        }
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(day);

        if (day > 999) throw new TodayBPException(TodayBPException.Type.TBP_BP_TooLongAgo);

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
            if (matcher.group("day") == null || Objects.equals(matcher.group("day"), "1")) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_No24H);
            } else {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoPeriod);
            }
        }

        try {
            var data = imageService.getPanelA4(ouMe, bpList, rankList);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("TBP Error: ", e);
            throw new TodayBPException(TodayBPException.Type.TBP_Send_Error);
        }

    }

}
