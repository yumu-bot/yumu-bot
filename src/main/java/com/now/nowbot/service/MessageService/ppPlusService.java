package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.SkiaUtil;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.regex.Matcher;

@Service("ppp")
public class ppPlusService implements MessageService{
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable{
        Contact from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());


        String id1,head1;
        id1 = String.valueOf(user.getOsuID());
        head1 = osuGetService.getPlayerOsuInfo(user).getString("avatar_url");


        JSONObject date1 = null;
        date1 = osuGetService.ppPlus(id1);
        if (date1 == null){
            throw new TipsException("那个破网站连不上");
        }

        float[] hex1 = osuGetService.ppPlus(new float[]{
                date1.getFloatValue("JumpAimTotal"),
                date1.getFloatValue("FlowAimTotal"),
                date1.getFloatValue("AccuracyTotal"),
                date1.getFloatValue("StaminaTotal"),
                date1.getFloatValue("SpeedTotal"),
                date1.getFloatValue("PrecisionTotal"),
        });

        byte[] datebyte = null;
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = SkiaUtil.getTorusRegular();
             Font fontA = new Font(fontface, 80);
             Font fontB = new Font(fontface, 64);
             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();

            Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPPlusBG.png")));
            Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPHexPanel.png")));
            Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPPlusOverlay.png")));
            Image bg4 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"mascot.png")));
            canvas.drawImage(bg1,0,0);
            canvas.drawImage(bg2,0,0);
            //在底下
            canvas.drawImage(bg4,surface.getWidth()-bg4.getWidth(),surface.getHeight()-bg4.getHeight(),new Paint().setAlpha(51));

            canvas.save();
            canvas.translate(960,440);
            org.jetbrains.skija.Path pt1 = SkiaUtil.creat6(390, 5, hex1[0], hex1[1], hex1[2], hex1[3], hex1[4], hex1[5]);
            canvas.drawPath(pt1,new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1,new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP+",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();
            canvas.drawImage(bg3,513,74);

            canvas.save();
            canvas.translate(280,440);
            TextLine text = TextLine.make(date1.getString("UserName"), fontA);
            if (text.getWidth() > 500) text = TextLine.make(date1.getString("UserName").substring(0,8)+"...",fontA);
            canvas.drawTextLine(text, -0.5f*text.getWidth(),0.25f*text.getHeight(),white);
            canvas.restore();

            DecimalFormat dx = new DecimalFormat("0");
            canvas.save();
            canvas.translate(100,520);
            TextLine k1 = TextLine.make("Jump",fontB);
            TextLine v1 = TextLine.make(dx.format(date1.getFloatValue("JumpAimTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Flow",fontB);
            v1 = TextLine.make(dx.format(date1.getFloatValue("FlowAimTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Acc",fontB);
            v1 = TextLine.make(dx.format(date1.getFloatValue("AccuracyTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Sta",fontB);
            v1 = TextLine.make(dx.format(date1.getFloatValue("StaminaTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Spd",fontB);
            v1 = TextLine.make(dx.format(date1.getFloatValue("SpeedTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Pre",fontB);
            v1 = TextLine.make(dx.format(date1.getFloatValue("PrecisionTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            canvas.save();
            canvas.translate(920,880);
            v1 = TextLine.make(dx.format(date1.getFloatValue("PerformanceTotal")),fontA);
            canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            ppPlusVsService.drowLhead(canvas, SkiaUtil.lodeNetWorkImage(head1));

            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
    public void HandleMessage_old(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        String name = "";
        BinUser user = null;
        At at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if(at != null){
            user = BindingUtil.readUser(at.getTarget());
        }else {
            name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                user = BindingUtil.readUser(event.getSender().getId());
            }
        }
        JSONObject js = null;
        if (user == null){
            js = osuGetService.ppPlus(""+osuGetService.getOsuId(name.trim()));
        }else {
            js = osuGetService.ppPlus(""+user.getOsuID());
        }
        if (js == null){
            throw new TipsException("连不上啊连不上！");
        }

        float[] date = osuGetService.ppPlus(new float[]{
                js.getFloatValue("JumpAimTotal"),
                js.getFloatValue("FlowAimTotal"),
                js.getFloatValue("AccuracyTotal"),
                js.getFloatValue("StaminaTotal"),
                js.getFloatValue("SpeedTotal"),
                js.getFloatValue("PrecisionTotal"),
        });

        byte[] datebyte = null;
        try(Surface surface = Surface.makeRasterN32Premul(600,800);
            Font smileFont = new Font(SkiaUtil.getTorusRegular(),20);
            Font lagerFont = new Font(SkiaUtil.getTorusRegular(),50);
            Font middleFont = new Font(SkiaUtil.getTorusRegular(), 30);
            Paint bg1 = new Paint().setARGB(40,0,0,0);
            Paint bg2 = new Paint().setARGB(220,0,0,0);
            Paint wp = new Paint().setARGB(255,200,200,200);
            Paint edP = new Paint().setARGB(200,0,0,0);
        ) {
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(65, 40, 49));

            var line = TextLine.make(js.getString("UserName"),lagerFont);
            canvas.drawTextLine(line,(600-line.getWidth())/2,line.getHeight(),new Paint().setARGB(255,255,255,255));

            canvas.save();
            canvas.translate(300,325);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);

            Path pt = SkiaUtil.creat6(250, 3, date[0], date[1], date[2], date[3], date[4], date[5]);

            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,240,167,50));
            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 167, 50));

            canvas.drawRRect(RRect.makeXYWH(-150,-226.5f,60,25,5),bg2);
            canvas.drawString("jump",-144,-208f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(100,-226.5f,60,25,5),bg2);
            canvas.drawString("flow",108,-208.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(230,-10,50,25,5),bg2);
            canvas.drawString("acc",239,7,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(105,206.5f,50,25,5),bg2);
            canvas.drawString("sta",114,223.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-145,206.5f,50,25,5),bg2);
            canvas.drawString("spd",-137,223.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-270,-10,50,25,5),bg2);
            canvas.drawString("pre",-261,7f,smileFont,wp);

            canvas.restore();
            canvas.translate(0,575);

            canvas.drawRRect(RRect.makeXYWH(50,0,225,50,10),edP);
            canvas.drawString("jump:"+(int)js.getFloatValue("JumpAimTotal"),60,35,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,0,225,50,10),edP);
            canvas.drawString("flow:"+(int)js.getFloatValue("FlowAimTotal"),335,35,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,75,225,50,10),edP);
            canvas.drawString("acc:"+(int)js.getFloatValue("AccuracyTotal"),60,110,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,75,225,50,10),edP);
            canvas.drawString("sta:"+(int)js.getFloatValue("StaminaTotal"),335,110,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,150,225,50,10),edP);
            canvas.drawString("spd:"+(int)js.getFloatValue("SpeedTotal"),60,185,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,150,225,50,10),edP);
            canvas.drawString("pre:"+(int)js.getFloatValue("PrecisionTotal"),335,185,middleFont,wp);

            canvas.restore();
            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
}
