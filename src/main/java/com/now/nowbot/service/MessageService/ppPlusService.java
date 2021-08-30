package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.entity.FontCfg;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ppp")
public class ppPlusService extends MsgSTemp implements MessageService{
    @Autowired
    OsuGetService osuGetService;
    ppPlusService() {
        super(Pattern.compile("[!！]\\s*(?i)ppp(?![v])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"ppp");
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
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
            throw new Exception("连不上啊连不上！");
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
            Font smileFont = new Font(FontCfg.JP,20);
            Font lagerFont = new Font(FontCfg.JP,50);
            Font middleFont = new Font(FontCfg.JP, 30);
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
