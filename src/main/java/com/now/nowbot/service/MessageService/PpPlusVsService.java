package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.SkiaUtil;
import com.now.nowbot.throwable.TipsException;
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

@Service("ppvs")
public class PpPlusVsService implements MessageService{
    static final Paint PAINT_ANTIALIAS =  new Paint().setAntiAlias(true).setMode(PaintMode.FILL);
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    StarService starService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());

        StarService.Score score = starService.getScore(user);
        if(!starService.delStart(score,1)){
//            SendmsgUtil.send(from,"您的积分不够1积分！");
            from.sendMessage("您的积分不够1积分！");
            return;
        }
        String id1,id2,head1,head2;
        id1 = String.valueOf(user.getOsuID());
        head1 = osuGetService.getPlayerOsuInfo(user).getString("avatar_url");
        At at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if(at != null){
            BinUser userx = null;
            try {
                userx = BindingUtil.readUser(at.getTarget());
            } catch (Exception e) {
                //拦截错误 此处一般为被@的人没有绑定信息
                throw new TipsException("对比的玩家没有绑定");
            }
            id2 = String.valueOf(userx.getOsuID());
            head2 = osuGetService.getPlayerOsuInfo(userx).getString("avatar_url");
        }else {
            String name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                throw new TipsException("里个瓜娃子到底要vs那个哦,扣你积分！");
            }
            try {
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
            org.jetbrains.skija.Path[] pt1 = SkiaUtil.creat6(390, 5, hex1[0], hex1[1], hex1[2], hex1[3], hex1[4], hex1[5]);
            org.jetbrains.skija.Path[] pt2 = SkiaUtil.creat6(390, 5, hex2[0], hex2[1], hex2[2], hex2[3], hex2[4], hex2[5]);
            canvas.drawPath(pt2[0],new Paint().setARGB(255,223,0,36).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt2[0],new Paint().setARGB(102,223,0,36).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt2[1],new Paint().setARGB(255,223,0,36).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt1[0],new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1[0],new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt1[1],new Paint().setARGB(255,42,98,183).setStroke(false).setStrokeWidth(5));
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
            v1 = TextLine.make(dx.format(date1.getFloatValue("PerformanceTotal")),fontA);
            canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            canvas.save();
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
            v1 = TextLine.make(dx.format(date2.getFloatValue("PerformanceTotal")),fontA);
            canvas.drawTextLine(v1,0,v1.getCapHeight(),white);
            canvas.restore();

            drowLhead(canvas, SkiaUtil.lodeNetWorkImage(head1));
            drowRhead(canvas, SkiaUtil.lodeNetWorkImage(head2));

            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
    static void drowLhead(Canvas canvas, Image head){
        canvas.save();
        canvas.translate(130,80);
        try(var ss = Surface.makeRasterN32Premul(300,300);) {
            ss.getCanvas()
                    .setMatrix(Matrix33.makeScale(300f / head.getWidth(), 300f / head.getHeight()))
                    .drawImage(head, 0, 0);
            head = ss.makeImageSnapshot();
        }
        canvas.clipRRect(RRect.makeXYWH(0,0,300,300,40));
        canvas.drawImage(head,0,0, PAINT_ANTIALIAS);
        canvas.restore();
    }
    static void drowRhead(Canvas canvas, Image head){
        canvas.save();
        canvas.translate(1490,80);
        try(var ss = Surface.makeRasterN32Premul(300,300);) {
            ss.getCanvas()
                    .setMatrix(Matrix33.makeScale(300f / head.getWidth(), 300f / head.getHeight()))
                    .drawImage(head, 0, 0);
            head = ss.makeImageSnapshot();
        }
        canvas.clipRRect(RRect.makeXYWH(0,0,300,300,40));
        canvas.drawImage(head,0,0, PAINT_ANTIALIAS);
        canvas.restore();
    }
}
