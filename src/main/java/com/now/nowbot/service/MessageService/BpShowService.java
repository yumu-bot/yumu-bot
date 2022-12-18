package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.regex.Matcher;

@Service("bp-show")
public class BpShowService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    @Autowired
    public BpShowService(OsuGetService osuGetService, BindDao bindDao) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var m = Integer.parseInt(matcher.group("n"));

        var from = event.getSubject();

        var user = bindDao.getUser(event.getSender().getId());

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, 0,100);
        var lines = new ArrayList<Image>();

        //生成hcard
        int min = Math.min(bpList.size(), m);
        from.sendMessage("0-" + min);
        for (int i = 0; i < min; i++) {
            var bp = bpList.get(i);
            lines.add(new HCardBuilder(bp, i+1).build());
        }

        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(user, mode);
        var card = CardBuilder.getUserCard(infoMe);

        var panel = new TBPPanelBuilder(lines.size());
        panel.drawBanner(PanelUtil.getBanner(user)).mainCrawCard(card.build()).drawBp(lines);
        QQMsgUtil.sendImage(from, panel.build(mode==OsuMode.DEFAULT?user.getMode():mode).encodeToData(EncodedImageFormat.JPEG, 80).getBytes());

    }
}
