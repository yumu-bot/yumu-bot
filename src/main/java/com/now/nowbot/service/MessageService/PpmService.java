package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.PPm.PPmObject;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.regex.Matcher;

@Service("ppm")
public class PpmService implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    @Override
//    @CheckPermission()
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
        //彩蛋 1%触发
        if (Math.random() < 0.01){
            Image spr = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"PPminusSurprise.png");
            try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
                 Typeface fontface = SkiaUtil.getTorusRegular();
                 Font fontA = new Font(fontface, 80);
                 Paint white = new Paint().setARGB(255,255,255,255);
            ){
                var canvas = surface.getCanvas();
                Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPminusBG.png")));
                Image bg_hex = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPHexPanel.png")));
                Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPminusOverlay.png")));
                Image bg4 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mascot.png")));
                Image mode_loge = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mode-"+mode+"-med.png")));
                canvas.drawImage(mode_loge,1800,10, new Paint().setAlpha(214));
                canvas.drawImage(bg1,0,0);
                canvas.drawImage(bg_hex,0,0);
                canvas.drawImage(bg3,513,74);
                canvas.drawImage(bg4,surface.getWidth()-bg4.getWidth(),surface.getHeight()-bg4.getHeight(),new Paint().setAlpha(51));
                canvas.drawImage(spr,0,0);

                Image head1 = SkiaUtil.lodeNetWorkImage(userinfo.getHeadURL());
                PpmVsService.drawLhead(canvas, head1);
                PpmVsService.drawLname(canvas,fontA,white,userinfo.getName());

                var date = surface.makeImageSnapshot().encodeToData().getBytes();
                from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(date),from));
            }
            return;
        }

        //绘制
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface Torus = SkiaUtil.getTorusRegular();
             Typeface Puhuiti = SkiaUtil.getPuhuitiMedium();
             Font torus_2 = new Font(Torus, 80);
             Font torus_1 = new Font(Torus, 64);
             Font puhuiti = new Font(Puhuiti, 64);
             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();
            canvas.save();
            Image background = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPminusBG.png")));
            Image bg_hex = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPHexPanel.png")));
            Image voer_img = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPminusOverlay.png")));
            Image loge = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mascot.png")));
            Image mode_loge = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mode-"+mode+"-med.png")));
            canvas.drawImage(background,0,0);
            canvas.drawImage(mode_loge,1800,10, new Paint().setAlpha(214));
            canvas.drawImage(bg_hex,0,0);

            //在底下
            canvas.drawImage(loge,surface.getWidth()-loge.getWidth(),surface.getHeight()-loge.getHeight(),new Paint().setAlpha(51));
            double[] hex1 = new double[]{
                    Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getFacc() < 0.6 ? 0 : userinfo.getFacc() - 0.6) * 2.5f, 0.8),
            };
            canvas.translate(960,440);
            org.jetbrains.skija.Path pt1[] = SkiaUtil.creat6(390, 5,(float) hex1[0],(float)hex1[1],(float)hex1[2],(float)hex1[3],(float)hex1[4],(float)hex1[5]);
            canvas.drawPath(pt1[0],new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1[0],new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt1[1],new Paint().setARGB(255,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP-",torus_2);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();

            canvas.drawImage(voer_img,513,74);

            Image head1 = SkiaUtil.lodeNetWorkImage(userinfo.getHeadURL());
            PpmVsService.drawLhead(canvas, head1);

            PpmVsService.drawLname(canvas,torus_2,white,userinfo.getName());

            PpmVsService.drawLppm(canvas,torus_1,puhuiti,torus_2,white,new double[]{
                    userinfo.getFacc(),
                    userinfo.getPtt(),
                    userinfo.getSta(),
                    userinfo.getStb(),
                    userinfo.getEng(),
                    userinfo.getSth(),
            }, userinfo.getTtl()*100);

            PpmVsService.drawLpj(canvas,userinfo,torus_1);

            {
                canvas.save();
                canvas.translate(920, 970);
                var tx = TextLine.make("综合", torus_1);
                canvas.drawTextLine(tx, -tx.getWidth(), tx.getCapHeight(), white);
                canvas.restore();
                canvas.save();
                canvas.translate(1000, 880);
                DecimalFormat dx = new DecimalFormat("0.00");
                tx = TextLine.make(dx.format(userinfo.getSan()), torus_2);
                canvas.drawTextLine(tx, 0, tx.getCapHeight(), white);
                canvas.translate(0,90);
                tx = TextLine.make("San", torus_1);
                canvas.drawTextLine(tx, 0, tx.getCapHeight(), white);
                canvas.restore();
            }

            var date = surface.makeImageSnapshot().encodeToData().getBytes();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(date), from));
        }
    }

}
