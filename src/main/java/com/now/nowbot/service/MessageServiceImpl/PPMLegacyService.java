package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.PPm.action.Func3;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.PPMinusException;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import com.now.nowbot.util.Panel.PPMVSPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaImageUtil;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ppm")
public class PPMLegacyService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger("PPmService");
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(pl(?![a-zA-Z_])|ppminuslegacy)+(?<vs>vs)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");

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
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }

        var from = event.getSubject();
        // 获得可能的 at
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        Ppm ppm;
        OsuUser user;
        List<Score> bps;
        var mode = OsuMode.getMode(matcher.group("mode"));

        if (at != null) {
            // 包含有@
            var userBin = bindDao.getUser(at.getTarget());
            //处理默认mode
            if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
            user = osuGetService.getPlayerInfo(userBin, mode);
            bps = osuGetService.getBestPerformance(userBin, mode, 0, 100);
            ppm = Ppm.getInstance(mode, user, bps);
        } else {
            // 不包含@ 分为查自身/查他人
            if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
                // 查他人
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = osuGetService.getPlayerInfo(id, mode);
                bps = osuGetService.getBestPerformance(id, mode, 0, 100);
                //默认无主模式
                if (mode == OsuMode.DEFAULT && user.getPlayMode() != null) mode = user.getPlayMode();
                ppm = Ppm.getInstance(mode, user, bps);
            } else {
                var userBin = bindDao.getUser(event.getSender().getId());//处理默认mode
                if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
                user = osuGetService.getPlayerInfo(userBin, mode);
                // 触发彩蛋
                if (Math.random() < 0.004){
                    var subject = event.getSubject();
                    var card = CardBuilder.getUserCard(user);
                    egg(card, subject);
                    return;
                }
                bps = osuGetService.getBestPerformance(userBin, mode, 0, 100);
                ppm = Ppm.getInstance(mode, user, bps);
            }
        }
        //生成panel名
        String panelName = "PPM" + switch (mode) {
            case OSU -> ":O";
            case MANIA -> ":M";
            case CATCH -> ":C";
            case TAIKO -> ":T";
            default -> ":?";
        };
        long now = System.currentTimeMillis();
        //绘制卡片A
        var card = CardBuilder.getUserCard(user);
        //计算六边形数据
        float[] HexData = ppm.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));

        // panel new
        var ppmPanel = new PPMPanelBuilder();
        ppmPanel.drawBanner(PanelUtil.getBanner(null));

        if (false){//底图自定义预留,当有自定义的底图时,走true
            ppmPanel.drawOverImage( null );
        } else {
            ppmPanel.drawOverImage();
        }
        //左侧 名称+数据
        ppm.drawValueName(ppmPanel::drawLeftNameN);
        ppm.drawValue(ppmPanel::drawLeftValueN);
        //左侧 rank
        ppm.drawRank(ppmPanel::switchRank);

        ppm.drawTitleName(ppmPanel::drawLeftTitleName, ppmPanel::drawRightTitleName);
        ppm.drawTitleValue(ppmPanel::drawLeftTotal, ppmPanel::drawRightTotal);

        ppmPanel.drawLeftCard(card.build());
        ppmPanel.drawPanelName(panelName);
        ppmPanel.drawHexagon(HexData, true);
        ppm.drawOverImage(ppmPanel::drawOverImage, null);//第二个作为用户自定义预留

        var panelImage = ppmPanel.build("YumuBot v0.2.0 Debug // PPMinus");
        try ( panelImage) {
            card.build().close();
            QQMsgUtil.sendImage(from, panelImage);
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws Exception {
        var from = event.getSubject();
        // 获得可能的 at
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        OsuUser userMe;
        var userBin = bindDao.getUser(event.getSender().getId());
        List<Score> bpListMe;
        OsuUser userOther;
        List<Score> bpListOther;
        Ppm ppmMe;
        Ppm ppmOther;

        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
        me://自己的信息
        {

            userMe = osuGetService.getPlayerInfo(userBin, mode);
            bpListMe = osuGetService.getBestPerformance(userBin, mode,0,100);
            ppmMe = Ppm.getInstance(mode, userMe, bpListMe);
            if (userMe.getStatistics().getPlayTime() < 60 || userMe.getStatistics().getPlayCount() < 30) {
                throw new PPMinusException(PPMinusException.Type.PPM_Me_PlayTimeTooShort);
            }
        }
        //生成panel名
        String panelName = "VS" + switch (mode) {
            case OSU -> ":O";
            case MANIA -> ":M";
            case CATCH -> ":C";
            case TAIKO -> ":T";
            default -> ":?";
        };
        if (at != null) {//被对比人的信息
            // 包含有@
            var OtherBin = bindDao.getUser(at.getTarget());
            userOther = osuGetService.getPlayerInfo(OtherBin, mode);
            bpListOther = osuGetService.getBestPerformance(OtherBin, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            userOther = osuGetService.getPlayerInfo(id, mode);
            bpListOther = osuGetService.getBestPerformance(id, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_VSNotFound);
        }
        if (userOther.getStatistics().getPlayTime() < 60 || userOther.getStatistics().getPlayCount() < 30) {
            throw new PPMinusException(PPMinusException.Type.PPM_Player_PlayTimeTooShort);
        }
        if (userOther.getUID() == 17064371L){
            PPMinusService.setUser(ppmOther, 999.99f);
        }

        //背景绘制
        Image uBgMe = PanelUtil.getBgUrl("用户自定义路径", userMe.getCoverUrl(), true);
        Image uBgOther = PanelUtil.getBgUrl("用户自定义路径", userOther.getCoverUrl(), true);

        //卡片生成
        var cardMe = CardBuilder.getUserCard(userMe);
        var cardOther = CardBuilder.getUserCard(userOther);

        //六边形数据
        float[] HexMe = ppmMe.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));
        float[] HexOther = ppmOther.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));

        //六边形缩放
        if(userMe.getStatistics().getPP() > userOther.getStatistics().getPP()){
            float n = (float) (userOther.getStatistics().getPP()/userMe.getStatistics().getPP());
            for (int i = 0; i < HexMe.length; i++) {
                HexOther[i] *= n;
            }
        } else {
            float n = (float) (userMe.getStatistics().getPP()/userOther.getStatistics().getPP());
            for (int i = 0; i < HexMe.length; i++) {
                HexMe[i] *= n;
            }
        }

        var panel = new PPMVSPanelBuilder()
                .drawBanner(PanelUtil.getBanner(null))
                .drawOverImage();
        //左侧 名称+数据
        ppmMe.drawValueName(panel::drawLeftNameN);
        ppmMe.drawValue(panel::drawLeftValueN);
        ppmMe.drawRank(panel::switchLeftRank);
        ppmMe.drawTitleValue(panel::drawLeftTotal, (a, b)-> null);
//        ppmMe.drawTitleName(panel::drawLeftTitleName, panel::drawRightTitleName);

        ppmOther.drawValueName(panel::drawRightNameN);
        ppmOther.drawValue(panel::drawRightValueN);
        ppmOther.drawRank(panel::switchRightRank);
        ppmOther.drawTitleValue(panel::drawRightTotal, (a, b)-> null);
//        ppmOther.drawTitleName(panel::drawLeftTitleName, panel::drawRightTitleName);
        panel.drawLeftCard(cardMe.build());
        panel.drawRightCard(cardOther.build());
        panel.drawHexagon(HexOther,false);
        panel.drawHexagon(HexMe,true);
        panel.drawPanelName(panelName);
        panel.drawLeftTitleName("Overall.L");
        panel.drawRightTitleName("Overall.R");
        try (uBgMe; uBgOther) {
            cardMe.build().close();
            cardOther.build().close();
            QQMsgUtil.sendImage(from, panel.build().encodeToData().getBytes());
            panel.build().close();
        }
    }

    private String getOff(double v1, double v2){
        return v1>v2 ? "+"+(int)(v1-v2) : "-"+(int)(v2-v1);
    }

    private void switchRank(int i, double date, Func3<Integer, String, Integer, Object> temp) {
        if (date == 1.2D){
            temp.call(i, "X+", PanelUtil.COLOR_X_PLUS);
        } else if (date >= 1){
            temp.call(i, "SS", PanelUtil.COLOR_SS);
        } else if (date >= 0.95) {
            temp.call(i, "S+", PanelUtil.COLOR_S_PLUS);
        } else if (date >= 0.90) {
            temp.call(i, "S", PanelUtil.COLOR_S);
        } else if (date >= 0.85) {
            temp.call(i, "A+", PanelUtil.COLOR_A_PLUS);
        } else if (date >= 0.80) {
            temp.call(i, "A", PanelUtil.COLOR_A);
        } else if (date >= 0.70) {
            temp.call(i, "B", PanelUtil.COLOR_B);
        } else if (date >= 0.60) {
            temp.call(i, "C", PanelUtil.COLOR_C);
        } else if (date > 0) {
            temp.call(i, "D", PanelUtil.COLOR_D);
        } else {
            temp.call(i, "F", PanelUtil.COLOR_F);
        }
    }

    private static void egg(ACardBuilder card, Contact subject) throws IOException {
        var di = SkiaImageUtil.getImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule-special.png");
        Surface surface = Surface.makeRasterN32Premul(1920, 1080);
        try (surface){
            Canvas canvas = surface.getCanvas();
            canvas.drawImage(di,0,0);
            canvas.translate(40, 40);
            canvas.drawImage(card.build(), 0, 0);
            card.build().close();
            QQMsgUtil.sendImage(subject, surface.makeImageSnapshot().encodeToData().getBytes());
        }
    }
}
