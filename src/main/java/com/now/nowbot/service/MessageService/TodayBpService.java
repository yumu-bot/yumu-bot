package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TbpPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;

@Service("T-BP")
public class TodayBpService implements MessageService{
    private static final int FONT_SIZE = 30;
    private Font font;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private Font getFont() {
        if (font == null) {
            font = new Font(SkiaUtil.getPUHUITI(), FONT_SIZE);
        }
        return font;
    }
    OsuGetService osuGetService;
    BindDao bindDao;
    DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    @Autowired
    public TodayBpService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        var from = event.getSubject();
        var user = bindDao.getUser(event.getSender().getId());

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, 0,100);
        var lines = new ArrayList<Image>();

        // 时间计算
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(-1);

        //生成hcard
        for (int i = 0; i < bpList.size(); i++) {
            var bp = bpList.get(i);
            if (dayBefore.isBefore(bp.getTime())){
                lines.add(new HCardBuilder(bp, i+1).build());
            }
        }
        //没有的话
        if (lines.size() <= 1){
            from.sendMessage("多打打");
            return;//此处结束
        }
        //绘制自己的卡片
        var infoMe = osuGetService.getPlayerInfo(user, mode);
        var card = new ACardBuilder(PanelUtil.getBgUrl(null/*"自定义路径"*/,infoMe.getCoverUrl(),true));
        card.drawA1(infoMe.getAvatarUrl())
                .drawA2(PanelUtil.getFlag(infoMe.getCountry().countryCode()))
                .drawA3(infoMe.getUsername());
        if (infoMe.getSupportLeve() != 0){
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        card.drawB3("")
                .drawB2(infoMe.getCountry().countryCode() + "#" + infoMe.getStatustucs().getCountryRank())
                .drawB1("U" + infoMe.getId())
                .drawC2(infoMe.getStatustucs().getAccuracy(2) + "% Lv." +
                        infoMe.getStatustucs().getLevelCurrent() +
                        "(" + infoMe.getStatustucs().getLevelProgress() + "%)")
                .drawC1(infoMe.getStatustucs().getPp(0) + "PP");

        var panel = new TbpPanelBuilder(lines.size());
        panel.drawBanner(PanelUtil.getBanner(user)).mainCrawCard(card.build()).drawBp(lines);
        QQMsgUtil.sendImage(from, panel.build());
    }

    int randomColor(){
        var t = new Random();
        var t1 = new Random();
        var t2 = new Random();
        return Color.makeRGB(128+t.nextInt(125),128+t1.nextInt(125),128+t2.nextInt(125));
    }

}
