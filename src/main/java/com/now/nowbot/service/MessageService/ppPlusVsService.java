package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.entity.FontCfg;
import com.now.nowbot.entity.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
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
import java.util.regex.Pattern;

@Service("ppvs")
public class ppPlusVsService extends MsgSTemp implements MessageService{
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    StarService starService;

    ppPlusVsService() {
        super(Pattern.compile("[!！]\\s?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"ppvs");
//        super(Pattern.compile("^owo$"),"ppvs");
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());

        StarService.Score score = starService.getScore(user);
        if(!starService.delStart(score,1)){
            from.sendMessage("您的积分不够1积分！");
            return;
        }
        String user1,user2,id1,id2,head1,head2;
        user1 = user.getOsuName();
        id1 = String.valueOf(user.getOsuID());
        head1 = osuGetService.getPlayerOsuInfo(user).getString("avatar_url");
        At at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if(at != null){
            BinUser userx = BindingUtil.readUser(at.getTarget());
            user2 = userx.getOsuName();
            id2 = String.valueOf(userx.getOsuID());
            head2 = osuGetService.getPlayerOsuInfo(userx).getString("avatar_url");
        }else {
            String name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                from.sendMessage("里个瓜娃子到底要vs那个哦,扣你积分！");
                return;
            }
            try {
                user2 = name;
                var user2d = osuGetService.getPlayerOsuInfo(name);
                id2 = user2d.getString("id");
                head2 = user2d.getString("avatar_url");
            } catch (Exception e) {
                starService.addStart(score,1);
                throw e;
            }
        }

        JSONObject date1 = null;
        JSONObject date2 = null;
        date1 = osuGetService.ppPlus(id1);
        date2 = osuGetService.ppPlus(id2);
        if (date1 == null || date2 == null){
            starService.addStart(score,1);
            throw new TipsError("那个破网站连不上");
        }

        float[] hex1 = osuGetService.ppPlus(new float[]{
                date1.getFloatValue("JumpAimTotal"),
                date1.getFloatValue("FlowAimTotal"),
                date1.getFloatValue("AccuracyTotal"),
                date1.getFloatValue("StaminaTotal"),
                date1.getFloatValue("SpeedTotal"),
                date1.getFloatValue("PrecisionTotal"),
        });

        float[] hex2 = osuGetService.ppPlus(new float[]{
                date2.getFloatValue("JumpAimTotal"),
                date2.getFloatValue("FlowAimTotal"),
                date2.getFloatValue("AccuracyTotal"),
                date2.getFloatValue("StaminaTotal"),
                date2.getFloatValue("SpeedTotal"),
                date2.getFloatValue("PrecisionTotal"),
        });

        byte[] datebyte = null;
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = FontCfg.getTorusRegular();
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
            org.jetbrains.skija.Path pt2 = SkiaUtil.creat6(390, 5, hex2[0], hex2[1], hex2[2], hex2[3], hex2[4], hex2[5]);
            canvas.drawPath(pt2,new Paint().setARGB(255,223,0,36).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt2,new Paint().setARGB(102,223,0,36).setStroke(false).setStrokeWidth(5));
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

            canvas.save();
            canvas.translate(1640,440);
            text = TextLine.make(date2.getString("UserName"), fontA);
            if (text.getWidth() > 500) text = TextLine.make(date2.getString("UserName").substring(0,8)+"...",fontA);
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
            v1 = TextLine.make(dx.format(date1.getString("PerformanceTotal")),fontA);
            canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            canvas.translate(1460,520);
            k1 = TextLine.make("Jump",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("JumpAimTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Flow",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("FlowAimTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Acc",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("AccuracyTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Sta",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("StaminaTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Spd",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("SpeedTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Pre",fontB);
            v1 = TextLine.make(dx.format(date2.getFloatValue("PrecisionTotal")),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();
            canvas.save();
            canvas.translate(1000,880);
            v1 = TextLine.make(date2.getString("PerformanceTotal"),fontA);
            canvas.drawTextLine(v1,0,v1.getCapHeight(),white);
            canvas.restore();

            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }

            /*
        try(Surface surface = Surface.makeRasterN32Premul(600,1025);
            Font smileFont = new Font(FontCfg.JP,20);
            Font lagerFont = new Font(FontCfg.JP,35);
            Font middleFont = new Font(FontCfg.JP, 30);
            Paint bg1 = new Paint().setARGB(40,0,0,0);
            Paint bg2 = new Paint().setARGB(220,0,0,0);
            Paint wp = new Paint().setARGB(255,200,200,200);
            Paint wp1 = new Paint().setARGB(255,50,196,233);
            Paint wp2 = new Paint().setARGB(255,240,0,110);
            Paint edP = new Paint().setARGB(200,0,0,0);
        ) {
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(65, 40, 49));

            {
                var line1 = TextLine.make(name1, lagerFont);
                var line2 = TextLine.make(name2, lagerFont);
                var vs = TextLine.make("VS", lagerFont);
                var textk = (surface.getWidth() - vs.getWidth()) / 2;
                canvas.drawTextLine(line1, (textk-line1.getWidth())/2, line1.getHeight() + 20, wp1);
                canvas.drawTextLine(line2, (surface.getWidth() + vs.getWidth())/2+(textk-line1.getWidth())/2, line2.getHeight() + 20, wp2);
                canvas.drawTextLine(vs, (600 - vs.getWidth()) / 2, vs.getHeight() + 20, wp);
            }

            canvas.save();
            canvas.translate(300,325);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);

            Path pt = SkiaUtil.creat6(250, 3, date[0], date[1], date[2], date[3], date[4], date[5]);
            Path ptv = SkiaUtil.creat6(250, 3, datev[0], datev[1], datev[2], datev[3], datev[4], datev[5]);

            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,50,196,233));
            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 50,196,233));

            canvas.drawPath(ptv, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,240,0,110));
            canvas.drawPath(ptv, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 0, 110));

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

            TextLine temp;
            canvas.drawRRect(RRect.makeXYWH(50,0,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("JumpAimTotal"),60,35,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("JumpAimTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),35,wp2);
            temp = TextLine.make("jump", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,35,wp);

            canvas.drawRRect(RRect.makeXYWH(50,75,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("FlowAimTotal"),60,110,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("FlowAimTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),110,wp2);
            temp = TextLine.make("flow", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,110,wp);

            canvas.drawRRect(RRect.makeXYWH(50,150,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("AccuracyTotal"),60,185,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("AccuracyTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),185,wp2);
            temp = TextLine.make("acc", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,185,wp);

            canvas.drawRRect(RRect.makeXYWH(50,225,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("StaminaTotal"),60,260,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("StaminaTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),260,wp2);
            temp = TextLine.make("sta", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,260,wp);

            canvas.drawRRect(RRect.makeXYWH(50,300,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("SpeedTotal"),60,345,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("SpeedTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),345,wp2);
            temp = TextLine.make("spd", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,345,wp);

            canvas.drawRRect(RRect.makeXYWH(50,375,500,50,10),edP);
            canvas.drawString(""+(int)user1.getFloatValue("PrecisionTotal"),60,410,middleFont,wp1);
            temp = TextLine.make(""+(int)user2.getFloatValue("PrecisionTotal"), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),410,wp2);
            temp = TextLine.make("pre", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,410,wp);

            canvas.restore();
            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
             */
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
}
