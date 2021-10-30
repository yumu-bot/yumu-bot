package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.OsuMode;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.Surface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("testppm")
public class TestPPmService implements MessageService{
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //获得可能的 at
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        PPmObject userinfo = null;
        JSONObject userdate;
        var mode = OsuMode.getMode(matcher.group("mode"));
        //分别区分每种模式
        switch (mode){
            default:{
                mode = OsuMode.OSU;
            }
            case OSU:{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    userdate = osuGetService.getPlayerOsuInfo(user);
                    var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                    userinfo = PPmObject.presOsu(userdate, bpdate);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        userdate = osuGetService.getPlayerOsuInfo(id);
                        var bpdate = osuGetService.getOsuBestMap(id, 0, 100);
                        userinfo = PPmObject.presOsu(userdate, bpdate);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        userdate = osuGetService.getPlayerOsuInfo(user);
                        var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                        userinfo = PPmObject.presOsu(userdate, bpdate);
                    }
                }
            } break;
            case TAIKO:{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    userdate = osuGetService.getPlayerTaikoInfo(user);
                    var bpdate = osuGetService.getTaikoBestMap(user, 0, 100);
                    userinfo = PPmObject.presTaiko(userdate, bpdate);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        userdate = osuGetService.getPlayerTaikoInfo(id);
                        var bpdate = osuGetService.getTaikoBestMap(id, 0, 100);
                        userinfo = PPmObject.presTaiko(userdate, bpdate);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        userdate = osuGetService.getPlayerTaikoInfo(user);
                        var bpdate = osuGetService.getTaikoBestMap(user, 0, 100);
                        userinfo = PPmObject.presTaiko(userdate, bpdate);
                    }
                }
            } break;
            case CATCH:{
                if (at != null) {
                    var user = BindingUtil.readUser(at.getTarget());
                    userdate = osuGetService.getPlayerCatchInfo(user);
                    var bpdate = osuGetService.getCatchBestMap(user, 0, 100);
                    userinfo = PPmObject.presCatch(userdate, bpdate);
                } else {
                    if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                        int id = osuGetService.getOsuId(matcher.group("name").trim());
                        userdate = osuGetService.getPlayerCatchInfo(id);
                        var bpdate = osuGetService.getCatchBestMap(id, 0, 100);
                        userinfo = PPmObject.presCatch(userdate, bpdate);
                    }else {
                        var user = BindingUtil.readUser(event.getSender().getId());
                        userdate = osuGetService.getPlayerCatchInfo(user);
                        var bpdate = osuGetService.getCatchBestMap(user, 0, 100);
                        userinfo = PPmObject.presCatch(userdate, bpdate);
                    }
                }
            } break;
            case MANIA:{
                throw new TipsException("等哪天mania社区风气变好了，或许就有PPM-mania了吧...");
            }
//            default:{
//                throw new TipsException("「邪恶的 osu! 玩家，我以 Bot 一族」…呃，这里不会读…「Bot 大魔王之名，否定你添加新模式的资格！」「除非你干掉 peppy，通过」…呃…「接受」…呃… 有几个词，波特不认识…");
//            }
        }
        if (userinfo == null) throw new TipsException("波特被玩坏了uwu");
        if (userinfo.getPtime()<60 || userinfo.getPcont()<30){
            throw new TipsException("游戏时长太短了，快去多玩几局吧！");
        }
        var u_head = SkiaUtil.lodeNetWorkImage(userinfo.getHeadURL());
        var u_bg_t = SkiaUtil.lodeNetWorkImage(userinfo.getBackgroundURL());
        Surface s = Surface.makeRasterN32Premul(u_bg_t.getWidth(),u_bg_t.getHeight());
        Image u_bg;
        try (u_bg_t;s){
            s.getCanvas().clear(Color.makeRGB(0,0,0));
            var n = s.makeImageSnapshot();
            s.getCanvas().drawImage(u_bg_t,0,0);
            s.getCanvas().drawImage(n,0,0,new Paint().setAlphaf(0.4f));
            u_bg = s.makeImageSnapshot();
        }
        var card = PanelUtil.getA1Builder(u_bg)
                .drowA1(userinfo.getHeadURL())
                .drowA2()
                .drowA3(userinfo.getName())
                .drowB3("")
                .drowB2("#"+userdate.getJSONObject("statistics").getString("global_rank"))
                .drowB1(userdate.getJSONObject("country").getString("code")+"#"+userdate.getJSONObject("statistics").getString("country_rank"))
                .drowC3("")
                .drowC2(userdate.getJSONObject("statistics").getString("hit_accuracy").substring(0,4)+"% LV"+
                        userdate.getJSONObject("statistics").getJSONObject("level").getString("current")+
                        "("+userdate.getJSONObject("statistics").getJSONObject("level").getString("progress")+"%)")
                .drowC1(userdate.getJSONObject("statistics").getString("pp")+"PP")
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
                .drowTopBackground(u_bg)
                .drowImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule.png"))
                .drowLeftCard(card)
                .drowLeftNameN(0,"FAC")
                .drowLeftNameN(1,"PTT")
                .drowLeftNameN(2,"STA")
                .drowLeftNameN(3,"STB")
                .drowLeftNameN(4,"ENG")
                .drowLeftNameN(5,"STH")
                .drowLeftValueN(0, String.valueOf((int)(userinfo.getFacc()*100)),PanelUtil.cutDecimalPoint(userinfo.getFacc()*100))
                .drowLeftValueN(1, String.valueOf((int)(userinfo.getPtt()*100)),PanelUtil.cutDecimalPoint(userinfo.getPtt()*100))
                .drowLeftValueN(2, String.valueOf((int)(userinfo.getSta()*100)),PanelUtil.cutDecimalPoint(userinfo.getSta()*100))
                .drowLeftValueN(3, String.valueOf((int)(userinfo.getStb()*100)),PanelUtil.cutDecimalPoint(userinfo.getStb()*100))
                .drowLeftValueN(4, String.valueOf((int)(userinfo.getEng()*100)),PanelUtil.cutDecimalPoint(userinfo.getEng()*100))
                .drowLeftValueN(5, String.valueOf((int)(userinfo.getSth()*100)),PanelUtil.cutDecimalPoint(userinfo.getSth()*100))
                .drowLeftTotal(String.valueOf((int)(userinfo.getTtl()*100)), PanelUtil.cutDecimalPoint(userinfo.getTtl()))
                .drowRightTotal(String.valueOf((int)(userinfo.getSan())), PanelUtil.cutDecimalPoint(userinfo.getSan()))
                .drowHexagon(hex, true).drowImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ExportFileV3/overlay-ppminusv3.2.png")).build();
        try (u_head;u_bg;card;panel){
            var b = from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panel.encodeToData().getBytes()), from));
            if (b != null) return;
        }
    }
}
