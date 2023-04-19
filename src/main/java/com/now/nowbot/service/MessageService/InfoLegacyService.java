package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.InfoLegacyPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("InfoLegacy")
public class InfoLegacyService implements MessageService{
    OsuGetService osuGetService;
    BindDao bindDao;

    public InfoLegacyService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var bUser = bindDao.getUser(event.getSender().getId());

        OsuUser user;
        List<Score> bpList;
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            user = osuGetService.getPlayerOsuInfo(id);
            bpList = osuGetService.getBestPerformance(id, mode, 0, 100);
        } else {
            var userBin = bindDao.getUser(event.getSender().getId());
            user = osuGetService.getPlayerOsuInfo(userBin);
            bpList = osuGetService.getBestPerformance(userBin, mode, 0, 100);
        }

        var img = new InfoLegacyPanelBuilder();
        img.drawBanner(PanelUtil.getBanner(bUser));
        var i = img.build(user,bpList);
        try (i){
            QQMsgUtil.sendImage(event.getSubject(), i);
        }
    }
}
