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
public class TestPPmService implements MessageService{
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("vs") != null){
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
        if (mode == OsuMode.DEFAULT ) mode = OsuMode.OSU;
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
            }else {
                var user = BindingUtil.readUser(event.getSender().getId());
                userdate = osuGetService.getPlayerInfo(user, mode.toString());
                var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
                userinfo = PPmObject.pres(userdate, bpdate, mode);
            }
        }
        if (userinfo == null) throw new TipsException("波特被玩坏了uwu");
        if (userinfo.getPtime()<60 || userinfo.getPcont()<30){
            throw new TipsException("游戏时长太短了，快去多玩几局吧！");
        }
        var uHead = SkiaUtil.lodeNetWorkImage(userinfo.getHeadURL());
        var uBgT = SkiaUtil.lodeNetWorkImage(userinfo.getBackgroundURL());
        Surface s = Surface.makeRasterN32Premul(uBgT.getWidth(),uBgT.getHeight());
        Image uBg;
        try (uBgT;s){
            s.getCanvas().clear(Color.makeRGB(255,255,255));
            // 模糊
            s.getCanvas().drawImage(uBgT,0,0,new Paint().setImageFilter(ImageFilter.makeBlur(10,10,FilterTileMode.REPEAT)));
            s.getCanvas().drawRect(Rect.makeWH(s.getWidth(), s.getHeight()),new Paint().setAlphaf(0.4f));
            uBg = s.makeImageSnapshot();
        }
        var card = PanelUtil.getA1Builder(uBg)
                .drowA1(userinfo.getHeadURL())
                .drowA2(PanelUtil.getFlag(userdate.getJSONObject("country").getString("code")))
                .drowA3(userinfo.getName())
                .drowB3("")
                .drowB2("#"+userdate.getJSONObject("statistics").getString("global_rank"))
                .drowB1(userdate.getJSONObject("country").getString("code")+"#"+userdate.getJSONObject("statistics").getString("country_rank"))
                .drowC3("")
                .drowC2(userdate.getJSONObject("statistics").getString("hit_accuracy").substring(0,4)+"% Lv."+
                        userdate.getJSONObject("statistics").getJSONObject("level").getString("current")+
                        "("+userdate.getJSONObject("statistics").getJSONObject("level").getString("progress")+"%)")
                .drowC1(userdate.getJSONObject("statistics").getIntValue("pp")+"PP")
                .build();
        float[] hex = new float[]{
                (float) Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8),
                (float) Math.pow((userinfo.getFacc() < 0.6 ? 0 : userinfo.getFacc() - 0.6) * 2.5f, 0.8),
        };
        var panel = PanelUtil.getPPMBulider()
                .drowBanner()
                .drowOverImage()
                .drowValueName()
                .drowLeftCard(card)
                .drowLeftValueN(0, String.valueOf((int)(userinfo.getFacc()*100)),PanelUtil.cutDecimalPoint(userinfo.getFacc()*100))
                .drowLeftValueN(1, String.valueOf((int)(userinfo.getPtt()*100)),PanelUtil.cutDecimalPoint(userinfo.getPtt()*100))
                .drowLeftValueN(2, String.valueOf((int)(userinfo.getSta()*100)),PanelUtil.cutDecimalPoint(userinfo.getSta()*100))
                .drowLeftValueN(3, String.valueOf((int)(userinfo.getStb()*100)),PanelUtil.cutDecimalPoint(userinfo.getStb()*100))
                .drowLeftValueN(4, String.valueOf((int)(userinfo.getEng()*100)),PanelUtil.cutDecimalPoint(userinfo.getEng()*100))
                .drowLeftValueN(5, String.valueOf((int)(userinfo.getSth()*100)),PanelUtil.cutDecimalPoint(userinfo.getSth()*100))
                .switchRank(0, userinfo.getFacc())
                .switchRank(1, userinfo.getPtt())
                .switchRank(2, userinfo.getSta())
                .switchRank(3, userinfo.getStb())
                .switchRank(4, userinfo.getEng())
                .switchRank(5, userinfo.getSth())
                .drowLeftTotal(String.valueOf((int)(userinfo.getTtl()*100)), PanelUtil.cutDecimalPoint(userinfo.getTtl()))
                .drowRightTotal(String.valueOf((int)(userinfo.getSan())), PanelUtil.cutDecimalPoint(userinfo.getSan()))
                .drowHexagon(hex, true);
        var panelImage = panel.drowImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/overlay-ppminusv3.2.png")).build("PANEL-PPM dev.0.0.1");
        try (uHead;uBg;card; panelImage){
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
        if (mode == OsuMode.DEFAULT ) mode = OsuMode.OSU;
        if (mode == OsuMode.MANIA) {
            throw new TipsException("等哪天mania社区风气变好了，或许就有PPM-mania了吧...");
        }
        me:{
            var user = BindingUtil.readUser(event.getSender().getId());
            userdateMe = osuGetService.getPlayerInfo(user, mode.toString());
            var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
            userinfoMe = PPmObject.pres(userdateMe, bpdate, mode);
            if (userinfoMe.getPtime()<60 || userinfoMe.getPcont()<30){
                throw new TipsException("你的游戏时长太短了，快去多玩几局吧！");
            }
        }
        if (at != null) {
            // 包含有@
            var user = BindingUtil.readUser(at.getTarget());
            userdateOther = osuGetService.getPlayerInfo(user, mode.toString());
            var bpdate = osuGetService.getBestMap(user, mode.toString(), 0, 100);
            userinfoOther = PPmObject.pres(userdateOther, bpdate, mode);
        }else if (matcher.group("name") != null && !matcher.group("name").trim().equals("")){
            int id = osuGetService.getOsuId(matcher.group("name").trim());
            userdateOther = osuGetService.getPlayerInfo(id, mode.toString());
            var bpdate = osuGetService.getBestMap(id, mode.toString(), 0, 100);
            userinfoOther = PPmObject.pres(userdateOther, bpdate, mode);
        } else {
            throw new TipsException("你想要对比谁呢");
        }
        if (userinfoOther.getPtime()<60 || userinfoOther.getPcont()<30){
            throw new TipsException("你的游戏时长太短了，快去多玩几局吧！");
        }

        var uBgTMe = SkiaUtil.lodeNetWorkImage(userinfoMe.getBackgroundURL());
        var uBgTOther = SkiaUtil.lodeNetWorkImage(userinfoOther.getBackgroundURL());
        Surface s1 = Surface.makeRasterN32Premul(uBgTMe.getWidth(),uBgTMe.getHeight());
        Surface s2 = Surface.makeRasterN32Premul(uBgTOther.getWidth(),uBgTOther.getHeight());
        Image uBgMe;
        Image uBgOther;
        try (uBgTMe;s1;uBgTOther;s2){
            s1.getCanvas().clear(Color.makeRGB(255,255,255));
            s2.getCanvas().clear(Color.makeRGB(255,255,255));
            s1.getCanvas().drawImage(uBgTMe,0,0);
            s2.getCanvas().drawImage(uBgTOther,0,0);
            s1.getCanvas().drawRect(Rect.makeWH(s1.getWidth(), s1.getHeight()),new Paint().setAlphaf(0.4f));
            s2.getCanvas().drawRect(Rect.makeWH(s2.getWidth(), s2.getHeight()),new Paint().setAlphaf(0.4f));
            uBgMe = s1.makeImageSnapshot();
            uBgOther = s2.makeImageSnapshot();
        }

        var cardMe = PanelUtil.getA1Builder(uBgMe).drowA1(userinfoMe.getHeadURL())
                .drowA2(PanelUtil.getFlag(userdateMe.getJSONObject("country").getString("code")))
                .drowA3(userinfoMe.getName())
                .drowB3("")
                .drowB2("#"+userdateMe.getJSONObject("statistics").getString("global_rank"))
                .drowB1(userdateMe.getJSONObject("country").getString("code")+"#"+userdateMe.getJSONObject("statistics").getString("country_rank"))
                .drowC3("")
                .drowC2(userdateMe.getJSONObject("statistics").getString("hit_accuracy").substring(0,4)+"% Lv."+
                        userdateMe.getJSONObject("statistics").getJSONObject("level").getString("current")+
                        "("+userdateMe.getJSONObject("statistics").getJSONObject("level").getString("progress")+"%)")
                .drowC1(userdateMe.getJSONObject("statistics").getIntValue("pp")+"PP")
                .build();
        var cardOther = PanelUtil.getA1Builder(uBgOther).drowA1(userinfoOther.getHeadURL())
                .drowA2(PanelUtil.getFlag(userdateOther.getJSONObject("country").getString("code")))
                .drowA3(userinfoOther.getName())
                .drowB3("")
                .drowB2("#"+userdateOther.getJSONObject("statistics").getString("global_rank"))
                .drowB1(userdateOther.getJSONObject("country").getString("code")+"#"+userdateOther.getJSONObject("statistics").getString("country_rank"))
                .drowC3("")
                .drowC2(userdateOther.getJSONObject("statistics").getString("hit_accuracy").substring(0,4)+"% Lv."+
                        userdateOther.getJSONObject("statistics").getJSONObject("level").getString("current")+
                        "("+userdateOther.getJSONObject("statistics").getJSONObject("level").getString("progress")+"%)")
                .drowC1(userdateOther.getJSONObject("statistics").getIntValue("pp")+"PP")
                .build();
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
        var panel = PanelUtil.getPPMBulider()
                .drowTopBackground(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/Banner/b3.jpg"))
                .drowImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule.png"))
                .drowLeftCard(cardMe)
                .drowLeftNameN(0,"FAC")
                .drowLeftNameN(1,"PTT")
                .drowLeftNameN(2,"STA")
                .drowLeftNameN(3,"STB")
                .drowLeftNameN(4,"ENG")
                .drowLeftNameN(5,"STH")
                .drowLeftValueN(0, String.valueOf((int)(userinfoMe.getFacc()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getFacc()*100))
                .drowLeftValueN(1, String.valueOf((int)(userinfoMe.getPtt()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getPtt()*100))
                .drowLeftValueN(2, String.valueOf((int)(userinfoMe.getSta()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getSta()*100))
                .drowLeftValueN(3, String.valueOf((int)(userinfoMe.getStb()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getStb()*100))
                .drowLeftValueN(4, String.valueOf((int)(userinfoMe.getEng()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getEng()*100))
                .drowLeftValueN(5, String.valueOf((int)(userinfoMe.getSth()*100)),PanelUtil.cutDecimalPoint(userinfoMe.getSth()*100))
                .drowRightCard(cardOther)
                .drowRightNameN(0, String.valueOf((int)(userinfoOther.getFacc()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getFacc()*100))
                .drowRightNameN(1, String.valueOf((int)(userinfoOther.getPtt()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getPtt()*100))
                .drowRightNameN(2, String.valueOf((int)(userinfoOther.getSta()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getSta()*100))
                .drowRightNameN(3, String.valueOf((int)(userinfoOther.getStb()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getStb()*100))
                .drowRightNameN(4, String.valueOf((int)(userinfoOther.getEng()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getEng()*100))
                .drowRightNameN(5, String.valueOf((int)(userinfoOther.getSth()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getSth()*100))
                .drowRightValueN(0, String.valueOf((int)(userinfoOther.getFacc()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getFacc()*100))
                .drowRightValueN(1, String.valueOf((int)(userinfoOther.getPtt()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getPtt()*100))
                .drowRightValueN(2, String.valueOf((int)(userinfoOther.getSta()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getSta()*100))
                .drowRightValueN(3, String.valueOf((int)(userinfoOther.getStb()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getStb()*100))
                .drowRightValueN(4, String.valueOf((int)(userinfoOther.getEng()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getEng()*100))
                .drowRightValueN(5, String.valueOf((int)(userinfoOther.getSth()*100)),PanelUtil.cutDecimalPoint(userinfoOther.getSth()*100))
                .drowLeftTotal(String.valueOf((int)(userinfoMe.getTtl()*100)), PanelUtil.cutDecimalPoint(userinfoMe.getTtl()*100))
                .drowRightTotal(String.valueOf((int)(userinfoOther.getTtl()*100)), PanelUtil.cutDecimalPoint(userinfoOther.getTtl()*100))
                .drowHexagon(hexOther,false)
                .drowHexagon(hexMe, true);

        switchRank(0, userinfoMe.getFacc(), panel::drowLeftRankN);
        switchRank(1, userinfoMe.getPtt(), panel::drowLeftRankN);
        switchRank(2, userinfoMe.getSta(), panel::drowLeftRankN);
        switchRank(3, userinfoMe.getStb(), panel::drowLeftRankN);
        switchRank(4, userinfoMe.getEng(), panel::drowLeftRankN);
        switchRank(5, userinfoMe.getSth(), panel::drowLeftRankN);

        switchRank(0, userinfoOther.getFacc(), panel::drowRightRankN);
        switchRank(1, userinfoOther.getPtt(), panel::drowRightRankN);
        switchRank(2, userinfoOther.getSta(), panel::drowRightRankN);
        switchRank(3, userinfoOther.getStb(), panel::drowRightRankN);
        switchRank(4, userinfoOther.getEng(), panel::drowRightRankN);
        switchRank(5, userinfoOther.getSth(), panel::drowRightRankN);

        var panelImage = panel.drowImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/overlay-ppminusv3.2.png")).build("PANEL-PPMVS dev.0.0.1");
        try (uBgMe;uBgOther;cardMe;cardOther;panelImage){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panelImage.encodeToData().getBytes()), from));
        }
    }
    private void switchRank(int i, double date, Action3<Integer, String, Integer> temp){
        if (date>0.95){
            temp.call(i, "SS", PanelUtil.COLOR_SS);
        }
        else if(date>0.90){
            temp.call(i, "S", PanelUtil.COLOR_S);
        }
        else if(date>0.85){
            temp.call(i, "A+", PanelUtil.COLOR_A_PLUS);
        }
        else if(date>0.80){
            temp.call(i, "A", PanelUtil.COLOR_A);
        }
        else if(date>0.70){
            temp.call(i, "B", PanelUtil.COLOR_B);
        }
        else if(date>0.60){
            temp.call(i, "C", PanelUtil.COLOR_C);
        }
        else if(date>0){
            temp.call(i, "D", PanelUtil.COLOR_D);
        }
        else {
            temp.call(i, "F", PanelUtil.COLOR_F);
        }
    }
}
