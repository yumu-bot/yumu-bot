package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("BPLegacy")
public class BPLegacyService implements MessageService<Matcher> {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public BPLegacyService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(bestperformancelegacy|bpl(?![a-zA-Z_])|bl(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)(-(?<m>\\d+))?)?$");

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
        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var uStr = matcher.group("name");

        if (nStr == null) nStr = "1";
        if (mStr == null) mStr = "";
        if (uStr == null) uStr = "";

        var n = Integer.parseInt(nStr) - 1;
        if (n < 0) n = 0; else if (n > 99) n = 99;
        int m = 1;

        if (!mStr.isEmpty()) {
            m = Integer.parseInt(mStr);
            if (m <= n) m = 1; else if (m > 100) m = 100 - n; else m = m - n;
        }

        var from = event.getSubject();

        BinUser user ;

        if (!uStr.isEmpty()) {
            // 这里不能用bindDao，只能从uStr获取玩家的名字
            long uid = osuGetService.getOsuId(uStr);
            user = new BinUser();
            user.setOsuID(uid);
            user.setMode(OsuMode.DEFAULT);
        } else {
            user = bindDao.getUser(event.getSender().getId());
        }

        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) mode = user.getMode();

        var bpList = osuGetService.getBestPerformance(user, mode, n, m);

        try {
            if (m > 1){
                oldSend(from, user, mode, bpList);
                return;
            }
            var u = osuGetService.getPlayerInfo(user, mode);
            if (bpList.isEmpty()) {

                return;
            }
            var score = bpList.get(0);
            var data = imageService.getPanelE(u, score,osuGetService);
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
