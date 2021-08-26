package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.FontCfg;
import com.now.nowbot.entity.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ppm")
public class ppmService extends MsgSTemp implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    ppmService() {
        super(Pattern.compile("[!！]\\s?(?i)ppm(?!v)(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"ppm");
    }

    @Override
//    @CheckPermission()
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);

        PPmObject userinfo;
        JSONObject userdate;
        if (at != null) {
            var user = BindingUtil.readUser(at.getTarget());
            if (user == null) {
                from.sendMessage(new QuoteReply(event.getMessage()).plus("该用户未绑定!"));
                return;
            }
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
                if (user == null) {
//                    from.sendMessage(PokeMessage.ChuoYiChuo);
                    from.sendMessage("您未绑定，请绑定后使用");
                    return;
                }
                userdate = osuGetService.getPlayerOsuInfo(user);
                var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                userinfo = PPmObject.presOsu(userdate, bpdate);
            }
        }

        if(userinfo.getPtime()<60 || userinfo.getPcont()<30){
            from.sendMessage("游戏时常过短,可能为新号，无法计算");
            return;
        }
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = FontCfg.TORUS_REGULAR;
             Font fontA = new Font(fontface, 80);
             Font fontB = new Font(fontface, 64);
             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();
            canvas.save();
            Image img = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"mascot.png");
            canvas.drawImage(img,surface.getWidth()-img.getWidth(), surface.getHeight()-img.getHeight());
            Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPminusBG.png")));
            Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPHexPanel.png")));
            Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"mascot.png")));
            canvas.drawImage(bg1,0,0);
            canvas.drawImage(bg2,0,0);
            canvas.drawImage(bg3,surface.getWidth()-img.getWidth(),surface.getHeight()-img.getHeight(),new Paint().setAlpha(51));
/***
 * (float) Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
 *                     (float) Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
 *                     (float) Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
 *                     (float) Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8),
 *                     (float) Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
 *                     (float) Math.pow((userinfo.getFa() < 0.6 ? 0 : userinfo.getFa() - 0.6) * 2.5f, 0.8));
 */
            double[] hex1 = new double[]{
                    Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo.getFa() < 0.6 ? 0 : userinfo.getFa() - 0.6) * 2.5f, 0.8),
            };
            canvas.translate(960,440);
            org.jetbrains.skija.Path pt1 = SkiaUtil.creat6(390, 5,(float) hex1[0],(float)hex1[1],(float)hex1[2],(float)hex1[3],(float)hex1[4],(float)hex1[5]);
            canvas.drawPath(pt1,new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1,new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP-",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();


            Image head1 = SkiaUtil.lodeNetWorkImage(userinfo.headURL);
            PpmVsService.drowLhead(canvas, head1);

            PpmVsService.drowLname(canvas,fontA,white,userinfo.getName());

            PpmVsService.drowLppm(canvas,fontB,fontA,white,new double[]{
                    userinfo.getFa(),
                    userinfo.getPtt(),
                    userinfo.getSta(),
                    userinfo.getStb(),
                    userinfo.getEng(),
                    userinfo.getSth(),
            }, userinfo.getTtl()*100);

            {
                canvas.save();
                canvas.translate(920, 970);
                var tx = TextLine.make("总计", fontA);
                canvas.drawTextLine(tx, -tx.getWidth(), tx.getCapHeight(), white);
                canvas.restore();
                canvas.save();
                canvas.translate(1000, 880);
                DecimalFormat dx = new DecimalFormat("0.00");
                tx = TextLine.make(dx.format(userinfo.getSan()), fontB);
                canvas.drawTextLine(tx, 0, tx.getCapHeight(), white);
                canvas.translate(0,90);
                tx = TextLine.make("San值", fontA);
                canvas.drawTextLine(tx, 0, tx.getCapHeight(), white);
                canvas.restore();
            }

            var date = surface.makeImageSnapshot().encodeToData().getBytes();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(date), from));
        }
//        try(Surface surface = Surface.makeRasterN32Premul(600,830);
//            Font smileFont = new Font(FontCfg.JP,20);
//            Font lagerFont = new Font(FontCfg.JP,50);
//            Font middleFont = new Font(FontCfg.JP, 30);
//            Paint bg1 = new Paint().setARGB(40,0,0,0);
//            Paint bg2 = new Paint().setARGB(220,0,0,0);
//            Paint wp = new Paint().setARGB(255,200,200,200);
//            Paint edP = new Paint().setARGB(200,0,0,0)) {
//
//            var canvas = surface.getCanvas();
//            canvas.clear(Color.makeRGB(65, 40, 49));
//
//            var line = TextLine.make(userinfo.getName(), lagerFont);
//            canvas.drawTextLine(line, (600 - line.getWidth()) / 2, line.getHeight(), new Paint().setARGB(255, 255, 255, 255));
//
//            canvas.save();
//            canvas.translate(300, 325);
//            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
//            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
//            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
//            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);
//
//
//            Path pt = SkiaUtil.creat6(250, 3,
//                    (float) Math.pow((userinfo.getFa() < 0.6 ? 0 : userinfo.getFa() - 0.6) * 2.5f, 0.8),
//                    (float) Math.pow((userinfo.getPtt() < 0.6 ? 0 : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
//                    (float) Math.pow((userinfo.getSta() < 0.6 ? 0 : userinfo.getSta() - 0.6) * 2.5f, 0.8),
//                    (float) Math.pow((userinfo.getStb() < 0.6 ? 0 : userinfo.getStb() - 0.6) * 2.5f, 0.8),
//                    (float) Math.pow((userinfo.getEng() < 0.6 ? 0 : userinfo.getEng() - 0.6) * 2.5f, 0.8),
//                    (float) Math.pow((userinfo.getSth() < 0.6 ? 0 : userinfo.getSth() - 0.6) * 2.5f, 0.8)
//            );
//
//
//            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255, 240, 167, 50));
//            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 167, 50));
//
//            canvas.drawRRect(RRect.makeXYWH(-150, -226.5f, 60, 25, 5), bg2);
//            canvas.drawString("acc", -144, -208f, smileFont, wp);//1
//
//            canvas.drawRRect(RRect.makeXYWH(100, -226.5f, 60, 25, 5), bg2);
//            canvas.drawString("ptt", 108, -208.5f, smileFont, wp);//2
//
//            canvas.drawRRect(RRect.makeXYWH(230, -10, 50, 25, 5), bg2);
//            canvas.drawString("sta", 240, 8, smileFont, wp);//6
//
//            canvas.drawRRect(RRect.makeXYWH(105, 206.5f, 50, 25, 5), bg2);
//            canvas.drawString("stb", 114, 224.5f, smileFont, wp);//3
//
//            canvas.drawRRect(RRect.makeXYWH(-145, 206.5f, 50, 25, 5), bg2);
//            canvas.drawString("eng", -139, 223.5f, smileFont, wp);//5
//
//            canvas.drawRRect(RRect.makeXYWH(-270, -10, 50, 25, 5), bg2);
//            canvas.drawString("sth", -260, 8f, smileFont, wp);//4
//
//            canvas.restore();
//            canvas.translate(0, 575);
//
//            DecimalFormat dx = new DecimalFormat("0.00");
//            canvas.drawRRect(RRect.makeXYWH(50, 0, 225, 50, 10), edP);
//            canvas.drawString("ACC:" + dx.format(userinfo.getFa() * 100), 60, 35, middleFont, wp);
//            canvas.drawRRect(RRect.makeXYWH(325, 0, 225, 50, 10), edP);
//            canvas.drawString("PTT:" + dx.format(userinfo.getPtt() * 100), 335, 35, middleFont, wp);
//
//            canvas.drawRRect(RRect.makeXYWH(50, 75, 225, 50, 10), edP);
//            canvas.drawString("STH:" + dx.format(userinfo.getSth() * 100), 60, 110, middleFont, wp);
//            canvas.drawRRect(RRect.makeXYWH(325, 75, 225, 50, 10), edP);
//            canvas.drawString("STA:" + dx.format(userinfo.getSta() * 100), 335, 110, middleFont, wp);
//
//            canvas.drawRRect(RRect.makeXYWH(50, 150, 225, 50, 10), edP);
//            canvas.drawString("ENG:" + dx.format(userinfo.getEng() * 100), 60, 185, middleFont, wp);
//            canvas.drawRRect(RRect.makeXYWH(325, 150, 225, 50, 10), edP);
//            canvas.drawString("STB:" + dx.format(userinfo.getStb() * 100), 335, 185, middleFont, wp);
//
//            var datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
//            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte), from));
//        }
    }

}
