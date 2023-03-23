package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Service("bp-show")
public class BpShowService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;

    @Autowired
    public BpShowService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var m = Integer.parseInt(matcher.group("n"));

        var from = event.getSubject();

        var user = bindDao.getUser(event.getSender().getId());

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, m, 1);

        try {
            var u = osuGetService.getPlayerInfo(user, mode);
            if (bpList.size() == 0) {

                return;
            }
            var score = bpList.get(0);
            var data = YmpService.postImage(u, score, osuGetService, template);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception ignored) {
            from.sendMessage("查询异常,请等一会再试");
        }
    }

    private void oldSend(Contact from, BinUser user, OsuMode mode, List<Score> bpList, int m) throws IOException {
        var lines = new ArrayList<Image>();
        //生成hcard
        int min = Math.min(bpList.size(), m);
        from.sendMessage("0-" + min);
        for (int i = 0; i < min; i++) {
            var bp = bpList.get(i);
            lines.add(new HCardBuilder(bp, i + 1).build());
        }

        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(user, mode);
        var card = CardBuilder.getUserCard(infoMe);

        var panel = new TBPPanelBuilder(lines.size());
        panel.drawBanner(PanelUtil.getBanner(user)).mainCrawCard(card.build()).drawBp(lines);
        QQMsgUtil.sendImage(from, panel.build(mode == OsuMode.DEFAULT ? user.getMode() : mode).encodeToData(EncodedImageFormat.JPEG, 80).getBytes());

    }
}
