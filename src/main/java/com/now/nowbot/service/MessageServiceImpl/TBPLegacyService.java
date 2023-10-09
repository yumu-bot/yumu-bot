package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.TodayBPException;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.TBPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import io.github.humbleui.skija.Color;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
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
import java.util.regex.Pattern;

@Service("TBPLegacy")
public class TBPLegacyService implements MessageService<Matcher> {
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
    public TBPLegacyService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(todaybplegacy|tbpl(?![a-zA-Z_])|tl(?![a-zA-Z_]))+\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?(?!#))?\\s*(#?\\s*(?<day>\\d*)\\s*)$");

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
        var from = event.getSubject();
        var name = matcher.group("name");
        var mode = OsuMode.getMode(matcher.group("mode"));
        var day = matcher.group("day");

        List<Score> bpList = null;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        OsuUser user = null;
        if (at != null) {
            var bUser = bindDao.getUser(at.getTarget());
            if (mode == OsuMode.DEFAULT) mode = bUser.getMode();
            user = osuGetService.getPlayerInfo(bUser, mode);
            bpList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
        } else if (!name.isEmpty()) {
            user = osuGetService.getPlayerInfo(name, mode);
            bpList = osuGetService.getBestPerformance(user.getUID(), mode, 0, 100);
        } else {
            var bUser = bindDao.getUser(event.getSender().getId());
            if (mode == OsuMode.DEFAULT) mode = bUser.getMode();
            user = osuGetService.getPlayerInfo(bUser, mode);
            bpList = osuGetService.getBestPerformance(bUser, mode, 0, 100);
        }

        if (CollectionUtils.isEmpty(bpList)) {
            throw new TodayBPException(TodayBPException.Type.TBP_BP_NoBP);
        }

        var lines = new ArrayList<Image>();

        // 时间计算
        int _day = -1;
        if (day == null) day = "1";

        if (!day.isEmpty()){
            _day = - Integer.parseInt(day);
        }
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(_day);

        //生成hcard
        for (int i = 0; i < bpList.size(); i++) {
            var bp = bpList.get(i);
            if (dayBefore.isBefore(bp.getCreateTime()) || user.getUID().equals(17064371L)){
                lines.add(new HCardBuilder(bp, i+1).build());
            }
        }
        //没有的话
        if (lines.size() < 1){
            if (day.isEmpty() || Objects.equals(day, "1")) {
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
