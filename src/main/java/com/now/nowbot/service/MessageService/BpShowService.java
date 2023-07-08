package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
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
    ImageService imageService;

    @Autowired
    public BpShowService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var n = Integer.parseInt(matcher.group("n")) - 1;
        if (n < 0) n = 0; else if (n > 99) n = 99;
        int m = 1;
        var mStr = matcher.group("m");
        var uStr = matcher.group("name");

        if (mStr != null) {
            m = Integer.parseInt(mStr);
            if (m <= n) m = 1; else if (m > 99) m = 99-n; else m = m - n;
        }

        var from = event.getSubject();

        BinUser user ;

        if (uStr != null) {
            // 这里不能用bindDao，只能从uStr获取玩家的名字
            long uid = osuGetService.getOsuId(uStr);
            user = new BinUser();
            user.setOsuID(uid);
        } else {
            user = bindDao.getUser(event.getSender().getId());
        }

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, n, m);

        try {
            if (m > 1){
                oldSend(from, user, mode, bpList);
                return;
            }
            var u = osuGetService.getPlayerInfo(user, mode);
            if (bpList.size() == 0) {

                return;
            }
            var score = bpList.get(0);
            var data = imageService.drawScore(u, score,osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception ignored) {
            from.sendMessage("查询异常,请等一会再试");
        }
    }

    private void oldSend(Contact from, BinUser user, OsuMode mode, List<Score> bpList) throws IOException {
        var lines = new ArrayList<Image>();
        //生成hcard
        int min = bpList.size();
//        from.sendMessage("0-" + min);
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
