package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.InfoPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("info")
public class InfoService implements MessageService{
    OsuGetService osuGetService;
    BindDao bindDao;

    public InfoService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var bUser = bindDao.getUser(event.getSender().getId());
        var user = osuGetService.getPlayerInfo(bUser);

        var img = new InfoPanelBuilder();
        img.drawBanner(PanelUtil.getBanner(bUser));
        var bps = osuGetService.getBestPerformance(bUser, OsuMode.OSU,0,100);
        var i = img.build(user, bps);
        try (i){
            QQMsgUtil.sendImage(event.getSubject(), i);
        }
    }
}
