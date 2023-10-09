package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.InfoLegacyPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("InfoLegacy")
public class InfoLegacyService implements MessageService<Matcher> {
    OsuGetService osuGetService;
    BindDao bindDao;

    public InfoLegacyService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }


    Pattern pattern = Pattern.compile("[!！]\\s*(?i)(testinfo)([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
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
