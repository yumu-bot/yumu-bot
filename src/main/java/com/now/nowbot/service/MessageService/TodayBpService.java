package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.TodayBPException;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        var mode = OsuMode.getMode(matcher.group("mode"));
        List<Score> bpList = null;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        OsuUser user = null;
        String name = null;
        if (at != null) {
            var bUser = bindDao.getUser(at.getTarget());
            user = osuGetService.getPlayerInfo(bUser, mode);
            bpList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
        } else if ((name = matcher.group("name")) != null) {
            user = osuGetService.getPlayerInfo(name, mode);
            bpList = osuGetService.getBestPerformance(user.getId(), mode, 0, 100);
        } else {
            var bUser = bindDao.getUser(event.getSender().getId());
            user = osuGetService.getPlayerInfo(bUser, mode);
            bpList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
        }

        if (CollectionUtils.isEmpty(bpList)) {
            throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP);
        }

        var lines = new ArrayList<Image>();

        // 时间计算
        int dat = -1;
        if (matcher.group("day") != null){
            dat = -Integer.parseInt(matcher.group("day"));
        }
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(dat);

        //生成hcard
        for (int i = 0; i < bpList.size(); i++) {
            var bp = bpList.get(i);
            if (dayBefore.isBefore(bp.getCreateTime()) || user.getId().equals(17064371L)){
                lines.add(new HCardBuilder(bp, i+1).build());
            }
        }
        //没有的话
        if (lines.size() < 1){
            if (matcher.group("day") == null || Objects.equals(matcher.group("day"), "1")) {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_No24H);
            } else {
                throw new TodayBPException(TodayBPException.Type.TBP_BP_NoPeriod);
            }
            //from.sendMessage("多打打");
            //return;//此处结束
        }
        //绘制自己的卡片;
        var card = CardBuilder.getUserCard(user);

        var panel = new TBPPanelBuilder(lines.size());
        panel.drawBanner(PanelUtil.getBanner(null)).mainCrawCard(card.build()).drawBp(lines);
        try {
            QQMsgUtil.sendImage(from, panel.build(mode==OsuMode.DEFAULT ? user.getPlayMode() : mode).encodeToData(EncodedImageFormat.JPEG, 80).getBytes());
        } catch (Exception e) {
            throw new TodayBPException(TodayBPException.Type.TBP_Send_Error);
        }

    }

    int randomColor(){
        var t = new Random();
        var t1 = new Random();
        var t2 = new Random();
        return Color.makeRGB(128+t.nextInt(125),128+t1.nextInt(125),128+t2.nextInt(125));
    }

}
