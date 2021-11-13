package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.functions.Action3;

import java.util.regex.Matcher;

@Service("testppm")
public class TestPPmService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null) {
            // 就不写一堆了,整个方法把
            doVs(event, matcher);
            return;
        }
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        PPmObject userinfo = null;
        JSONObject userdate;
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) mode = OsuMode.OSU;
        if (mode == OsuMode.MANIA) {
            throw new TipsException("等哪天mania社区风气变好了，或许就有PPM-mania了吧...");
        }
        if (at != null) {
            // 包含有@
            var user = BindingUtil.readUser(at.getTarget());
            userdate = osuGetService.getPlayerInfo(user, mode.toString());
            var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
            userinfo = PPmObject.pres(userdate, bpdate, mode);
        } else {
            // 不包含@ 分为查自身/查他人
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                // 查他人
                int id = osuGetService.getOsuId(matcher.group("name").trim());
                userdate = osuGetService.getPlayerInfo(id, mode.toString());
                var bpdate = osuGetService.getBestMap(id, mode.toString(), 0, 100);
                userinfo = PPmObject.pres(userdate, bpdate, mode);
            } else {
                var user = BindingUtil.readUser(event.getSender().getId());
                userdate = osuGetService.getPlayerInfo(user, mode.toString());
                var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
                userinfo = PPmObject.pres(userdate, bpdate, mode);
            }
        }
        if (userinfo == null) throw new TipsException("波特被玩坏了uwu");
        if (userinfo.getPtime() < 60 || userinfo.getPcont() < 30) {
            throw new TipsException("游戏时长太短了，快去多玩几局吧！");
        }
        //生成panel名
        String panelName = "PPM:" + switch (mode) {
            case OSU -> "O";
            case MANIA -> "M";
            case CATCH -> "C";
            case TAIKO -> "T";
            default -> "?";
        };
        //获得背景
        Image uBg = PanelUtil.getBgUrl("用户自定义路径", userinfo.getBackgroundURL());

        //绘制卡片A
        var card = PanelUtil.getA1Builder(uBg)
                .drawA1(userinfo.getHeadURL())
                .drawA2(PanelUtil.getFlag(userdate.getJSONObject("country").getString("code")))
                .drawA3(userinfo.getName());
        card.drawB3("")
                .drawB2("#" + userdate.getJSONObject("statistics").getString("global_rank"))
                .drawB1(userdate.getJSONObject("country").getString("code") + "#" + userdate.getJSONObject("statistics").getString("country_rank"))
                .drawC3("")
                .drawC2(userdate.getJSONObject("statistics").getString("hit_accuracy").substring(0, 4) + "% Lv." +
                        userdate.getJSONObject("statistics").getJSONObject("level").getString("current") +
                        "(" + userdate.getJSONObject("statistics").getJSONObject("level").getString("progress") + "%)")
                .drawC1(userdate.getJSONObject("statistics").getIntValue("pp") + "PP");
        if (userdate.getBoolean("is_supporter")) {
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        //计算六边形数据
        float[] hex = new float[]{
                (float) Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getFacc() < 0.6 ? 0 : userinfo.getFacc() - 0.6) * 2.5f, 0.8),
        };
        //生成panel
        var panel = PanelUtil.getPPMBulider()
                .drawBanner(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/Banner/b3.png"))
                .drawOverImage()
                .drawValueName()
                .drawLeftCard(card.build())
                .drawLeftValueN(0, String.valueOf((int) (userinfo.getFacc() * 100)), PanelUtil.cutDecimalPoint(userinfo.getFacc() * 100))
                .drawLeftValueN(1, String.valueOf((int) (userinfo.getPtt() * 100)), PanelUtil.cutDecimalPoint(userinfo.getPtt() * 100))
                .drawLeftValueN(2, String.valueOf((int) (userinfo.getSta() * 100)), PanelUtil.cutDecimalPoint(userinfo.getSta() * 100))
                .drawLeftValueN(3, String.valueOf((int) (userinfo.getStb() * 100)), PanelUtil.cutDecimalPoint(userinfo.getStb() * 100))
                .drawLeftValueN(4, String.valueOf((int) (userinfo.getEng() * 100)), PanelUtil.cutDecimalPoint(userinfo.getEng() * 100))
                .drawLeftValueN(5, String.valueOf((int) (userinfo.getSth() * 100)), PanelUtil.cutDecimalPoint(userinfo.getSth() * 100))
                //评级
                .switchRank(0, userinfo.getFacc())
                .switchRank(1, userinfo.getPtt())
                .switchRank(2, userinfo.getSta())
                .switchRank(3, userinfo.getStb())
                .switchRank(4, userinfo.getEng())
                .switchRank(5, userinfo.getSth())
                .drawLeftTotal(String.valueOf((int) (userinfo.getTtl() * 100)), PanelUtil.cutDecimalPoint(userinfo.getTtl()))
                .drawRightTotal(String.valueOf((int) (userinfo.getSan())), PanelUtil.cutDecimalPoint(userinfo.getSan()))
                .drawPanelName(panelName)
                .drawHexagon(hex, true);
        var panelImage = panel.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/overlay-ppminusv3.2.png")).build("PANEL-PPM dev.0.0.1");
        try (uBg; panelImage) {
            card.build().close();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panelImage.encodeToData().getBytes()), from));
        }
    }

    private void doVs(MessageEvent event, Matcher matcher) throws Exception {
        var from = event.getSubject();
        // 获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        PPmObject userinfoMe;
        JSONObject userdateMe;
        PPmObject userinfoOther;
        JSONObject userdateOther;
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) mode = OsuMode.OSU;
        if (mode == OsuMode.MANIA) {
            throw new TipsException("等哪天mania社区风气变好了，或许就有PPM-mania了吧...");
        }
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
            var user = BindingUtil.readUser(event.getSender().getId());
            userdateMe = osuGetService.getPlayerInfo(user, mode.toString());
            var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
            userinfoMe = PPmObject.pres(userdateMe, bpdate, mode);
            if (userinfoMe.getPtime() < 60 || userinfoMe.getPcont() < 30) {
                throw new TipsException("你的游戏时长太短了，快去多玩几局吧！");
            }
        }
        if (at != null) {//被对比人的信息
            // 包含有@
            var user = BindingUtil.readUser(at.getTarget());
            userdateOther = osuGetService.getPlayerInfo(user, mode.toString());
            var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
            userinfoOther = PPmObject.pres(userdateOther, bpdate, mode);
        } else if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            int id = osuGetService.getOsuId(matcher.group("name").trim());
            userdateOther = osuGetService.getPlayerInfo(id, mode.toString());
            var bpdate = osuGetService.getBestMap(id, mode.toString(), 0, 100);
            userinfoOther = PPmObject.pres(userdateOther, bpdate, mode);
        } else {
            throw new TipsException("你想要对比谁呢");
        }
        if (userinfoOther.getPtime() < 60 || userinfoOther.getPcont() < 30) {
            throw new TipsException("你的游戏时长太短了，快去多玩几局吧！");
        }

        //背景绘制
        Image uBgMe = PanelUtil.getBgUrl("用户自定义路径", userinfoMe.getBackgroundURL());
        Image uBgOther = PanelUtil.getBgUrl("用户自定义路径", userinfoOther.getBackgroundURL());

        //卡片生成
        var cardMe = PanelUtil.getA1Builder(uBgMe).drawA1(userinfoMe.getHeadURL())
                .drawA2(PanelUtil.getFlag(userdateMe.getJSONObject("country").getString("code")))
                .drawA3(userinfoMe.getName());
        if (userdateOther.getBoolean("is_supporter")) {
            cardMe.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        cardMe.drawB3("")
                .drawB2("#" + userdateMe.getJSONObject("statistics").getString("global_rank"))
                .drawB1(userdateMe.getJSONObject("country").getString("code") + "#" + userdateMe.getJSONObject("statistics").getString("country_rank"))
                .drawC3("")
                .drawC2(userdateMe.getJSONObject("statistics").getString("hit_accuracy").substring(0, 4) + "% Lv." +
                        userdateMe.getJSONObject("statistics").getJSONObject("level").getString("current") +
                        "(" + userdateMe.getJSONObject("statistics").getJSONObject("level").getString("progress") + "%)")
                .drawC1(userdateMe.getJSONObject("statistics").getIntValue("pp") + "PP");
        var cardOther = PanelUtil.getA1Builder(uBgOther).drawA1(userinfoOther.getHeadURL())
                .drawA2(PanelUtil.getFlag(userdateOther.getJSONObject("country").getString("code")))
                .drawA3(userinfoOther.getName());
        if (userdateOther.getBoolean("is_supporter")) {
            cardOther.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        cardOther.drawB3("")
                .drawB2("#" + userdateOther.getJSONObject("statistics").getString("global_rank"))
                .drawB1(userdateOther.getJSONObject("country").getString("code") + "#" + userdateOther.getJSONObject("statistics").getString("country_rank"))
                .drawC3("")
                .drawC2(userdateOther.getJSONObject("statistics").getString("hit_accuracy").substring(0, 4) + "% Lv." +
                        userdateOther.getJSONObject("statistics").getJSONObject("level").getString("current") +
                        "(" + userdateOther.getJSONObject("statistics").getJSONObject("level").getString("progress") + "%)")
                .drawC1(userdateOther.getJSONObject("statistics").getIntValue("pp") + "PP");
        //六边形数据
        float[] hexMe = new float[]{
                (float) Math.pow((userinfoMe.getPtt() < 0.6 ? 0 : userinfoMe.getPtt() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoMe.getSta() < 0.6 ? 0 : userinfoMe.getSta() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoMe.getStb() < 0.6 ? 0 : userinfoMe.getStb() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoMe.getEng() < 0.6 ? 0 : userinfoMe.getEng() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoMe.getSth() < 0.6 ? 0 : userinfoMe.getSth() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoMe.getFacc() < 0.6 ? 0 : userinfoMe.getFacc() - 0.6) * 2.5f, 0.8),
        };
        float[] hexOther = new float[]{
                (float) Math.pow((userinfoOther.getPtt() < 0.6 ? 0 : userinfoOther.getPtt() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoOther.getSta() < 0.6 ? 0 : userinfoOther.getSta() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoOther.getStb() < 0.6 ? 0 : userinfoOther.getStb() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoOther.getEng() < 0.6 ? 0 : userinfoOther.getEng() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoOther.getSth() < 0.6 ? 0 : userinfoOther.getSth() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfoOther.getFacc() < 0.6 ? 0 : userinfoOther.getFacc() - 0.6) * 2.5f, 0.8),
        };
        //六边形缩放
        if(userinfoMe.getPp() > userinfoOther.getPp()){
            float n = userinfoOther.getPp()/userinfoMe.getPp();
            for (int i = 0; i < hexMe.length; i++) {
                hexOther[i] *= n;
            }
        } else {
            float n = userinfoMe.getPp()/userinfoOther.getPp();
            for (int i = 0; i < hexMe.length; i++) {
                hexMe[i] *= n;
            }
        }

        var panel = PanelUtil.getPPMBulider()
                .drawBanner(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/Banner/b3.png"))
                .drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/panel-ppmodule.png"));
        switchRank(0, userinfoMe.getFacc(), panel::drawLeftRankN);
        switchRank(1, userinfoMe.getPtt(), panel::drawLeftRankN);
        switchRank(2, userinfoMe.getSta(), panel::drawLeftRankN);
        switchRank(3, userinfoMe.getStb(), panel::drawLeftRankN);
        switchRank(4, userinfoMe.getEng(), panel::drawLeftRankN);
        switchRank(5, userinfoMe.getSth(), panel::drawLeftRankN);

        switchRank(0, userinfoOther.getFacc(), panel::drawRightRankN);
        switchRank(1, userinfoOther.getPtt(), panel::drawRightRankN);
        switchRank(2, userinfoOther.getSta(), panel::drawRightRankN);
        switchRank(3, userinfoOther.getStb(), panel::drawRightRankN);
        switchRank(4, userinfoOther.getEng(), panel::drawRightRankN);
        switchRank(5, userinfoOther.getSth(), panel::drawRightRankN);
        //进行因子修正,上方的是原数据
        userinfoMe.dovs();
        userinfoOther.dovs();
        //下方为修正后的数据
        panel.drawLeftCard(cardMe.build())
                .drawLeftValueN(0, String.valueOf((int) (userinfoMe.getFacc())))
                .drawLeftValueN(1, String.valueOf((int) (userinfoMe.getPtt())))
                .drawLeftValueN(2, String.valueOf((int) (userinfoMe.getSta())))
                .drawLeftValueN(3, String.valueOf((int) (userinfoMe.getStb())))
                .drawLeftValueN(4, String.valueOf((int) (userinfoMe.getEng())))
                .drawLeftValueN(5, String.valueOf((int) (userinfoMe.getSth())))
                .drawRightCard(cardOther.build())
                .drawRightNameN(0, String.valueOf((int) (userinfoOther.getFacc())))
                .drawRightNameN(1, String.valueOf((int) (userinfoOther.getPtt())))
                .drawRightNameN(2, String.valueOf((int) (userinfoOther.getSta())))
                .drawRightNameN(3, String.valueOf((int) (userinfoOther.getStb())))
                .drawRightNameN(4, String.valueOf((int) (userinfoOther.getEng())))
                .drawRightNameN(5, String.valueOf((int) (userinfoOther.getSth())))
                .drawRightValueN(0, getOff(userinfoMe.getFacc(),userinfoOther.getFacc()))
                .drawRightValueN(1, getOff(userinfoMe.getPtt(),userinfoOther.getPtt()))
                .drawRightValueN(2, getOff(userinfoMe.getSta(),userinfoOther.getSta()))
                .drawRightValueN(3, getOff(userinfoMe.getStb(),userinfoOther.getStb()))
                .drawRightValueN(4, getOff(userinfoMe.getEng(),userinfoOther.getEng()))
                .drawRightValueN(5, getOff(userinfoMe.getSth(),userinfoOther.getSth()))
                .drawLeftTotal(String.valueOf((int)userinfoMe.getTtl()))
                .drawRightTotal(String.valueOf((int) userinfoOther.getTtl()))
                .drawPanelName(panelName)
                .drawHexagon(hexOther, false)
                .drawHexagon(hexMe, true);
        //生成
        var panelImage = panel.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/overlay-ppminusv3.2.png"))//叠加层
                .drawPanelName(panelName)//panel名
                .build("PANEL-PPMVS dev.0.0.1");
        try (uBgMe; uBgOther; panelImage) {
            cardMe.build().close();
            cardOther.build().close();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panelImage.encodeToData().getBytes()), from));
        }
    }

    private String getOff(double v1, double v2){
        return v1>v2 ? "+"+(int)(v1-v2) : "-"+(v2-v1);
    }

    private void switchRank(int i, double date, Action3<Integer, String, Integer> temp) {
        if (date > 0.95) {
            temp.call(i, "SS", PanelUtil.COLOR_SS);
        } else if (date > 0.90) {
            temp.call(i, "S", PanelUtil.COLOR_S);
        } else if (date > 0.85) {
            temp.call(i, "A+", PanelUtil.COLOR_A_PLUS);
        } else if (date > 0.80) {
            temp.call(i, "A", PanelUtil.COLOR_A);
        } else if (date > 0.70) {
            temp.call(i, "B", PanelUtil.COLOR_B);
        } else if (date > 0.60) {
            temp.call(i, "C", PanelUtil.COLOR_C);
        } else if (date > 0) {
            temp.call(i, "D", PanelUtil.COLOR_D);
        } else {
            temp.call(i, "F", PanelUtil.COLOR_F);
        }
    }
}
