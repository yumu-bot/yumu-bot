package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.serviceException.PpmException;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import com.now.nowbot.util.Panel.PPMVSPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaImageUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Surface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.functions.Action3;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

@Service("ppm")
public class PPmService implements MessageService {
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }
        if (Math.random() < 0.01){
            var userBin = bindDao.getUser(event.getSender().getId());
            var user = osuGetService.getPlayerOsuInfo(userBin);
            var card = getUserCard(user);
            var di = SkiaImageUtil.getImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule-special.png");
            Surface surface = Surface.makeRasterN32Premul(1920, 1080);
            try (surface){
                Canvas canvas = surface.getCanvas();
                canvas.drawImage(di,0,0);
                canvas.translate(40, 40);
                canvas.drawImage(card.build(), 0, 0);
                card.build().close();
                event.getSubject().sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(surface.makeImageSnapshot().encodeToData().getBytes()), event.getSubject()));
            }
            return;
        }
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        Ppm ppm;
        OsuUser user;
        List<BpInfo> bps;
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
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                // 查他人
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = osuGetService.getPlayerInfo(id, mode);
                bps = osuGetService.getBestPerformance(id, mode, 0, 100);
                ppm = Ppm.getInstance(mode, user, bps);
            } else {
                var userBin = bindDao.getUser(event.getSender().getId());//处理默认mode
                if (mode == OsuMode.DEFAULT && userBin.getMode() != null) mode = userBin.getMode();
                user = osuGetService.getPlayerInfo(userBin, mode);
                bps = osuGetService.getBestPerformance(userBin, mode, 0, 100);
                ppm = Ppm.getInstance(mode, user, bps);
            }
        }
        //生成panel名
        String panelName = "PPM:" + switch (mode) {
            case OSU -> "O";
            case MANIA -> "M";
            case CATCH -> "C";
            case TAIKO -> "T";
            default -> "?";
        };

        //绘制卡片A
        var card = getUserCard(user);
        //计算六边形数据
        float[] hexDate = ppm.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));

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

        ppm.drawTotleName(ppmPanel::drawLeftTotleName, ppmPanel::drawRightTotleName);
        ppm.drawTotleValue(ppmPanel::drawLeftTotal, ppmPanel::drawRightTotal);

        ppmPanel.drawLeftCard(card.build());
        ppmPanel.drawPanelName(panelName);
        ppmPanel.drawHexagon(hexDate, true);
        ppm.drawOverImage(ppmPanel::drawOverImage, null);//第二个作为用户自定义预留

        var panelImage = ppmPanel.build("PANEL-PPM dev.0.0.1");
        try ( panelImage) {
            card.build().close();
            QQMsgUtil.sendImage(from, panelImage);
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws Exception {
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        OsuUser userMe;
        var userBin = bindDao.getUser(event.getSender().getId());
        List<BpInfo> bpListMe;
        OsuUser userOther;
        List<BpInfo> bpListOther;
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
            if (userMe.getStatustucs().getPlayTime() < 60 || userMe.getStatustucs().getPlayCount() < 30) {
                throw new PpmException(PpmException.Type.PPM_Me_PlayTimeTooShort);
            }
        }
        //生成panel名
        String panelName = "PPM.v:" + switch (userMe.getPlayMode()) {
            case OSU -> "O";
            case MANIA -> "M";
            case CATCH -> "C";
            case TAIKO -> "T";
            default -> "?";
        };
        if (at != null) {//被对比人的信息
            // 包含有@
            var OtherBin = bindDao.getUser(at.getTarget());
            userOther = osuGetService.getPlayerInfo(OtherBin, mode);
            bpListOther = osuGetService.getBestPerformance(OtherBin, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            userOther = osuGetService.getPlayerInfo(id, mode);
            bpListOther = osuGetService.getBestPerformance(id, mode,0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else {
            throw new PpmException(PpmException.Type.PPM_Player_VSNotFound);
        }
        if (userOther.getStatustucs().getPlayTime() < 60 || userOther.getStatustucs().getPlayCount() < 30) {
            throw new PpmException(PpmException.Type.PPM_Player_PlayTimeTooShort);
        }

        //背景绘制
        Image uBgMe = PanelUtil.getBgUrl("用户自定义路径", userMe.getCoverUrl(), true);
        Image uBgOther = PanelUtil.getBgUrl("用户自定义路径", userOther.getCoverUrl(), true);

        //卡片生成
        var cardMe = getUserCard(userMe);
        var cardOther = getUserCard(userOther);

        //六边形数据
        float[] hexMe = ppmMe.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));
        float[] hexOther = ppmOther.getValues(d ->  (float) Math.pow((d < 0.6 ? 0 : d - 0.6) * 2.5f, 0.8));

        //六边形缩放
        if(userMe.getStatustucs().getPp() > userOther.getStatustucs().getPp()){
            float n = (float) (userOther.getStatustucs().getPp()/userMe.getStatustucs().getPp());
            for (int i = 0; i < hexMe.length; i++) {
                hexOther[i] *= n;
            }
        } else {
            float n = (float) (userMe.getStatustucs().getPp()/userOther.getStatustucs().getPp());
            for (int i = 0; i < hexMe.length; i++) {
                hexMe[i] *= n;
            }
        }

        var panel = new PPMVSPanelBuilder()
                .drawBanner(PanelUtil.getBanner(null))
                .drawOverImage()
                .drawValueName();
        //左侧 名称+数据
        ppmMe.drawValueName(panel::drawLeftNameN);
        ppmMe.drawValue(panel::drawLeftValueN);
        ppmMe.drawRank(panel::switchLeftRank);
        ppmMe.drawTotleValue(panel::drawLeftTotal, (a,b)-> null);

        ppmOther.drawValueName(panel::drawRightNameN);
        ppmOther.drawValue(panel::drawRightValueN);
        ppmOther.drawRank(panel::switchRightRank);
        ppmOther.drawTotleValue(panel::drawRightTotal, (a,b)-> null);
        panel.drawLeftCard(cardMe.build());
        panel.drawRightCard(cardOther.build());
        panel.drawHexagon(hexOther,false);
        panel.drawHexagon(hexMe,true);
        panel.drawPanelName(panelName);
        try (uBgMe; uBgOther) {
            cardMe.build().close();
            cardOther.build().close();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create( panel.build().encodeToData().getBytes()), from));
            panel.build().close();
        }
    }

    private String getOff(double v1, double v2){
        return v1>v2 ? "+"+(int)(v1-v2) : "-"+(int)(v2-v1);
    }

    private void switchRank(int i, double date, Action3<Integer, String, Integer> temp) {
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

    private ACardBuilder getUserCard(OsuUser user) throws IOException {
        Image uBg = PanelUtil.getBgUrl("用户自定义路径", user.getCoverUrl(), true);
        var card = PanelUtil.getA1Builder(uBg)
                .drawA1(user.getAvatarUrl())
                .drawA2(PanelUtil.getFlag(user.getCountry().countryCode()))
                .drawA3(user.getUsername());
        card.drawB2("#" + user.getStatustucs().getGlobalRank())
                .drawB1(user.getCountry().countryCode() + "#" + user.getStatustucs().getCountryRank())
                .drawC2(String.format("%.2f",user.getStatustucs().getAccuracy()) + "% Lv." +
                        user.getStatustucs().getLevelCurrent() +
                        "(" + user.getStatustucs().getLevelProgress() + "%)")
                .drawC1(user.getStatustucs().getPp().intValue() + "PP");
        if (user.getSupportLeve()>0) {
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        return card;
    }
}
