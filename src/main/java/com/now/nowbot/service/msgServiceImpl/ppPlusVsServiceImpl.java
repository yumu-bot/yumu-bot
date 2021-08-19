package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.entity.FontCfg;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ppPlusVsServiceImpl extends MessageService{
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    StarService starService;
    public ppPlusVsServiceImpl(){
        super("ppvs");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        At at = null;
        for (var m : event.getMessage()){
            if (m instanceof At){
                at = (At) m;
                break;
            }
        }

        String name1;
        BinUser us = BindingUtil.readUser(event.getSender().getId());
        String name2 = null;
        if (us == null) {
            from.sendMessage(new At(event.getSender().getId()).plus("您未绑定，请绑定后使用"));
            return;
        }
        StarService.score score = starService.getScore(us);
        if(!starService.delStart(score,1)){
            from.sendMessage("您的积分不够1积分！");
            return;
        }
        name1 = us.getOsuName();
        String fkid;
        if (at == null) {
            Pattern p = Pattern.compile(getKey().toLowerCase() + "\\s+(?<nm>[0-9a-zA-Z\\[\\]\\-_ ]*)?");
            Matcher m = p.matcher(event.getMessage().contentToString());
            if (m.find()) {
                name2 = m.group("nm").trim();
            }
            if (name2 == null || name2.equals("")) {
                from.sendMessage("里个瓜娃子到底要vs那个哦,扣你积分！");
                return;
            }
            fkid = name2;
        }else {
            BinUser r = BindingUtil.readUser(at.getTarget());
            if (r == null){
                from.sendMessage(at.plus("该用户未绑定"));
                starService.addStart(score,1);
                return;
            }
            fkid = r.getOsuID()+"";
            name2 = r.getOsuName();
        }

        JSONObject user1 = null;
        JSONObject user2 = null;
        user1 = osuGetService.ppPlus(us.getOsuID()+"");
        user2 = osuGetService.ppPlus(fkid);
        if (user1 == null || user2 == null){
            from.sendMessage("api请求失败");
            starService.addStart(score,1);
            return;
        }
        try {

            float[] date = osuGetService.ppPlus(new float[]{
                    user1.getFloatValue("JumpAimTotal"),
                    user1.getFloatValue("FlowAimTotal"),
                    user1.getFloatValue("AccuracyTotal"),
                    user1.getFloatValue("StaminaTotal"),
                    user1.getFloatValue("SpeedTotal"),
                    user1.getFloatValue("PrecisionTotal"),
            });

            float[] datev = osuGetService.ppPlus(new float[]{
                    user2.getFloatValue("JumpAimTotal"),
                    user2.getFloatValue("FlowAimTotal"),
                    user2.getFloatValue("AccuracyTotal"),
                    user2.getFloatValue("StaminaTotal"),
                    user2.getFloatValue("SpeedTotal"),
                    user2.getFloatValue("PrecisionTotal"),
            });

            byte[] datebyte = null;
            try(Surface surface = Surface.makeRasterN32Premul(600,1025);
                Font smileFont = new Font(FontCfg.face,20);
                Font lagerFont = new Font(FontCfg.face,35);
                Font middleFont = new Font(FontCfg.face, 30);
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
            if (datebyte != null ){
                from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
