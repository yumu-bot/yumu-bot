package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.serviceException.PpmException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import com.now.nowbot.util.Panel.PPMVSPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Surface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.functions.Action3;

import java.util.List;
import java.util.regex.Matcher;

@Service("ppm")
public class PPmService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }
        if (Math.random() < 0.01){
            var userBin = BindingUtil.readUser(event.getSender().getId());
            var user = osuGetService.getPlayerOsuInfoN(userBin);
            var card = getUserCard(user);
            var di = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule-special.png");
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
        if (mode == OsuMode.DEFAULT) mode = OsuMode.OSU;

        if (at != null) {
            // 包含有@
            var userBin = BindingUtil.readUser(at.getTarget());
            user = osuGetService.getPlayerInfoN(userBin, mode.getName());
            bps = osuGetService.getBestPerformance(userBin, mode.getName(), 0, 100);
            ppm = Ppm.getInstance(mode, user, bps);
        } else {
            // 不包含@ 分为查自身/查他人
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                // 查他人
                int id = osuGetService.getOsuId(matcher.group("name").trim());
                user = osuGetService.getPlayerInfoN(id, mode.getName());
                bps = osuGetService.getBestPerformance(id, mode.getName(), 0, 100);
                ppm = Ppm.getInstance(mode, user, bps);
            } else {
                var userBin = BindingUtil.readUser(event.getSender().getId());
                user = osuGetService.getPlayerInfoN(userBin, mode.getName());
                bps = osuGetService.getBestPerformance(userBin, mode.getName(), 0, 100);
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
        ppmPanel.drawBanner(PanelUtil.getBgFile(null, NowbotConfig.BG_PATH + "ExportFileV3/Banner/b3.jpg", false));

        if (false){//底图自定义预留,当有自定义的底图时,走true
            ppmPanel.drawOverImage( null );
        } else {
            ppmPanel.drawOverImage();
        }
        ppm.drawOverImage(ppmPanel::drawOverImage, null);//第二个作为用户自定义预留
        ppm.drawValueName(ppmPanel::drawLeftNameN);
        ppm.drawValue(ppmPanel::drawLeftValueN);
        ppm.drawRank(ppmPanel::switchRank);
        ppm.drawTotleName(ppmPanel::drawLeftTotleName, ppmPanel::drawRightTotleName);
        ppm.drawTotleValue(ppmPanel::drawLeftTotal, ppmPanel::drawRightTotal);

        ppmPanel.drawLeftCard(card.build());
        ppmPanel.drawPanelName(panelName);
        ppmPanel.drawHexagon(hexDate, true);

        var panelImage = ppmPanel.build("PANEL-PPM dev.0.0.1");
        try ( panelImage) {
            card.build().close();
            byte[] imgData = panelImage.encodeToData().getBytes();
            var image = ExternalResource.uploadAsImage(ExternalResource.create(imgData), from);
            from.sendMessage(image);
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws Exception {
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        OsuUser userMe;
        List<BpInfo> bpListMe;
        OsuUser userOther;
        List<BpInfo> bpListOther;
        Ppm ppmMe;
        Ppm ppmOther;

        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) mode = OsuMode.OSU;
        //生成panel名
        String panelName = "PPM.v:" + switch (mode) {
            case OSU -> "O";
            case MANIA -> "M";
            case CATCH -> "C";
            case TAIKO -> "T";
            default -> "?";
        };
        me://自己的信息
        {
            var userBin = BindingUtil.readUser(event.getSender().getId());
            userMe = osuGetService.getPlayerInfoN(userBin, mode.getName());
            bpListMe = osuGetService.getBestPerformance(userBin, mode.getName(),0,100);
            ppmMe = Ppm.getInstance(mode, userMe, bpListMe);
            if (userMe.getStatustucs().getPlayTime() < 60 || userMe.getStatustucs().getPlayCount() < 30) {
                throw new PpmException(PpmException.Type.PPM_Me_PlayTimeTooShort);
            }
        }
        if (at != null) {//被对比人的信息
            // 包含有@
            var userBin = BindingUtil.readUser(at.getTarget());
            userOther = osuGetService.getPlayerInfoN(userBin, mode.getName());
            bpListOther = osuGetService.getBestPerformance(userBin, mode.getName(),0,100);
            ppmOther = Ppm.getInstance(mode, userOther, bpListOther);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            int id = osuGetService.getOsuId(matcher.group("name").trim());
            userOther = osuGetService.getPlayerInfoN(id, mode.getName());
            bpListOther = osuGetService.getBestPerformance(id, mode.getName(),0,100);
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
                .drawBanner(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/Banner/b3.png"))
                .drawOverImage()
                .drawValueName();
//        switchRank(0, userinfoMe.getFacc(), panel::drawLeftRankN);
//        switchRank(1, userinfoMe.getPtt(), panel::drawLeftRankN);
//        switchRank(2, userinfoMe.getSta(), panel::drawLeftRankN);
//        switchRank(3, userinfoMe.getStb(), panel::drawLeftRankN);
//        switchRank(4, userinfoMe.getEng(), panel::drawLeftRankN);
//        switchRank(5, userinfoMe.getSth(), panel::drawLeftRankN);
//
//        switchRank(0, userinfoOther.getFacc(), panel::drawRightRankN);
//        switchRank(1, userinfoOther.getPtt(), panel::drawRightRankN);
//        switchRank(2, userinfoOther.getSta(), panel::drawRightRankN);
//        switchRank(3, userinfoOther.getStb(), panel::drawRightRankN);
//        switchRank(4, userinfoOther.getEng(), panel::drawRightRankN);
//        switchRank(5, userinfoOther.getSth(), panel::drawRightRankN);
//        //进行因子修正,上方的是原数据
//        userinfoMe.dovs();
//        userinfoOther.dovs();
//        //下方为修正后的数据
//        panel.drawLeftCard(cardMe.build())
//                .drawLeftValueN(0, String.valueOf((int) (userinfoMe.getFacc())))
//                .drawLeftValueN(1, String.valueOf((int) (userinfoMe.getPtt())))
//                .drawLeftValueN(2, String.valueOf((int) (userinfoMe.getSta())))
//                .drawLeftValueN(3, String.valueOf((int) (userinfoMe.getStb())))
//                .drawLeftValueN(4, String.valueOf((int) (userinfoMe.getEng())))
//                .drawLeftValueN(5, String.valueOf((int) (userinfoMe.getSth())))
//                .drawRightCard(cardOther.build())
//                .drawRightNameN(0, String.valueOf((int) (userinfoOther.getFacc())))
//                .drawRightNameN(1, String.valueOf((int) (userinfoOther.getPtt())))
//                .drawRightNameN(2, String.valueOf((int) (userinfoOther.getSta())))
//                .drawRightNameN(3, String.valueOf((int) (userinfoOther.getStb())))
//                .drawRightNameN(4, String.valueOf((int) (userinfoOther.getEng())))
//                .drawRightNameN(5, String.valueOf((int) (userinfoOther.getSth())))
//                .drawRightValueN(0, getOff(userinfoMe.getFacc(),userinfoOther.getFacc()))
//                .drawRightValueN(1, getOff(userinfoMe.getPtt(),userinfoOther.getPtt()))
//                .drawRightValueN(2, getOff(userinfoMe.getSta(),userinfoOther.getSta()))
//                .drawRightValueN(3, getOff(userinfoMe.getStb(),userinfoOther.getStb()))
//                .drawRightValueN(4, getOff(userinfoMe.getEng(),userinfoOther.getEng()))
//                .drawRightValueN(5, getOff(userinfoMe.getSth(),userinfoOther.getSth()))
//                .drawLeftTotal(String.valueOf((int)userinfoMe.getTtl()))
//                .drawRightTotal(String.valueOf((int) userinfoOther.getTtl()))
//                .drawPanelName(panelName)
//                .drawHexagon(hexOther, false)
//                .drawHexagon(hexMe, true);
//        //生成
//        var panelImage = panel.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/overlay-ppminusv3.2.png"))//叠加层
//                .drawPanelName(panelName)//panel名
//                .build("PANEL-PPMVS dev.0.0.1");
//        try (uBgMe; uBgOther; panelImage) {
//            cardMe.build().close();
//            cardOther.build().close();
//            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panelImage.encodeToData().getBytes()), from));
//        }
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

    private ACardBuilder getUserCard(OsuUser user){
        Image uBg = PanelUtil.getBgUrl("用户自定义路径", user.getCoverUrl(), true);
        var card = PanelUtil.getA1Builder(uBg)
                .drawA1(user.getAvatarUrl())
                .drawA2(PanelUtil.getFlag(user.getCountry().countryCode()))
                .drawA3(user.getUsername());
        card.drawB2("#" + user.getStatustucs().getGlobalRank())
                .drawB1(user.getCountry().countryCode() + "#" + user.getStatustucs().getCountryRank())
                .drawC2(String.format("%2f",user.getStatustucs().getAccuracy()) + "% Lv." +
                        user.getStatustucs().getLevelCurrent() +
                        "(" + user.getStatustucs().getLevelProgress() + "%)")
                .drawC1(user.getStatustucs().getPp().intValue() + "PP");
        if (user.getSupportLeve()>0) {
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        return card;
    }
}
