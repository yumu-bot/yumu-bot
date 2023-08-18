package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Service("BP")
public class BPService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public BPService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var uStr = matcher.group("name");

        int n;
        int m = 1;

        if (nStr == null) nStr = "1";
        if (mStr == null) mStr = "";
        if (uStr == null) uStr = "";

        if (nStr.isEmpty()) throw new BPException(BPException.Type.BP_Map_NoRank);
        try {
            n = Integer.parseInt(nStr) - 1;
        } catch (NumberFormatException e) {
            throw new BPException(BPException.Type.BP_Map_RankError);
        }

        if (n < 0) n = 0;
        else if (n > 99) n = 99;

        if (!mStr.isEmpty()) {
            m = Integer.parseInt(mStr);
            if (m <= n) throw new BPException(BPException.Type.BP_Map_RankError); //!bp 55-45
            else if (m > 100) m = 100 - n; //!bp 45-101
            else m = m - n;//正常
        }

        var from = event.getSubject();

        BinUser user;
        List<Score> bpList;
        ArrayList<Integer> rankList = new ArrayList<>();

        if (uStr.isEmpty()) {
            try {
                user = bindDao.getUser(event.getSender().getId());
            } catch (Exception e) {
                throw new BPException(BPException.Type.BP_Me_LoseBind);
            }
        } else {
            try {
                long uid = osuGetService.getOsuId(uStr);
                user = new BinUser();
                user.setOsuID(uid);
                user.setMode(OsuMode.DEFAULT);
            } catch (Exception e) {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        }

        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) mode = user.getMode();

        try {
            bpList = osuGetService.getBestPerformance(user, mode, n, m);
        } catch (Exception e) {
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }
        if (bpList == null || bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP);

        try {
            var ouMe = osuGetService.getPlayerInfo(user, mode);

            if (m > 1) {
                for (int i = n; i <= (m + n); i++) rankList.add(i + 1);
                var data = imageService.getPanelA4(ouMe, bpList, rankList);
                QQMsgUtil.sendImage(from, data);
            } else {
                var score = bpList.get(0);
                var data = imageService.getPanelE(ouMe, score, osuGetService);
                QQMsgUtil.sendImage(from, data);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("BP Error: ", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }
}
