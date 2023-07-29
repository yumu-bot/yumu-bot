package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
        int n;
        if (matcher.group("n") == null) throw new BPException(BPException.Type.BP_Map_NoRank);
        try {
            n = Integer.parseInt(matcher.group("n")) - 1;
        } catch (NumberFormatException e) {
            throw new BPException(BPException.Type.BP_Map_RankError);
        }

        if (n < 0) n = 0;
        else if (n > 99) n = 99;
        int m = 1;
        var mStr = matcher.group("m");
        var uStr = matcher.group("name");

        if (mStr != null) {
            m = Integer.parseInt(mStr);
            if (m <= n) m = 1;
            else if (m > 99) m = 99 - n;
            else m = m - n;
        }

        var from = event.getSubject();

        BinUser user;

        if (uStr != null) {
            // 这里不能用bindDao，只能从uStr获取玩家的名字

            try {
                long uid = osuGetService.getOsuId(uStr);
                user = new BinUser();
                user.setOsuID(uid);
            } catch (NullPointerException e) {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        } else {
            try {
                user = bindDao.getUser(event.getSender().getId());
            } catch (NullPointerException e) {
                throw new BPException(BPException.Type.BP_Me_LoseBind);
            }
        }

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, n, m);
        if (bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP);

        //我的构想是 tBP 也可以这么传数据，唉，那个还是交给 tBP 处理吧
        ArrayList<Integer> rankList = new ArrayList<>();

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
