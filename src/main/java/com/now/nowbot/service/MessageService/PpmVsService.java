package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.BinUser;
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

@Service("ppmvs")
public class PpmVsService extends MsgSTemp implements MessageService{

    @Autowired
    OsuGetService osuGetService;
    PpmVsService(){
        super(Pattern.compile("[!！]\\s?(?i)ppmvs(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"ppmvs");
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable{
        var from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if (user == null){
//            from.sendMessage(PokeMessage.ChuoYiChuo);
            from.sendMessage("您未绑定，请绑定后使用");
            return;
        }


        PPmObject userinfo1;
        PPmObject userinfo2;
        At at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if(at != null){
            BinUser user2 = BindingUtil.readUser(at.getTarget());
            if(user2 == null) {
                from.sendMessage(new QuoteReply(event.getMessage()).plus("该用户没有绑定，无法查询！"));
                return;
            }
            var userdate = osuGetService.getPlayerOsuInfo(user2);
            var bpdate = osuGetService.getOsuBestMap(user2, 0, 100);
            userinfo2 = PPmObject.presOsu(userdate,bpdate);
        }else {
            String name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                from.sendMessage("里个瓜娃子到底要vs那个哦");
                return;
            }
            var id = osuGetService.getOsuId(name.trim());
            var userdate = osuGetService.getPlayerOsuInfo(id);
            var bpdate = osuGetService.getOsuBestMap(id, 0, 100);
            userinfo2 = PPmObject.presOsu(userdate,bpdate);
        }
        {
            var userdate = osuGetService.getPlayerOsuInfo(user);
            var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
            userinfo1 = PPmObject.presOsu(userdate, bpdate);
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
            Paint edP = new Paint().setARGB(200,0,0,0)){

            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(65, 40, 49));

            {
                var line1 = TextLine.make(userinfo1.getName(), lagerFont);
                var line2 = TextLine.make(userinfo2.getName(), lagerFont);
                var vs = TextLine.make("VS", lagerFont);
                var textk = (surface.getWidth() - vs.getWidth()) / 2;
                canvas.drawTextLine(line1, (textk-line1.getWidth())/2, line1.getHeight() + 20, wp1);
                canvas.drawTextLine(line2, ((surface.getWidth()*3/2)-line2.getWidth())/2, line2.getHeight() + 20, wp2);
                canvas.drawTextLine(vs, (600 - vs.getWidth()) / 2, vs.getHeight() + 20, wp);
            }
            canvas.save();
            canvas.translate(300,325);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);

            Path pt1 = SkiaUtil.creat6(250, 3,
                    (float)Math.pow((userinfo1.getPtt()<0.6?0.01f:userinfo1.getPtt()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo1.getSta()<0.6?0.01f:userinfo1.getSta()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo1.getStb()<0.6?0.01f:userinfo1.getStb()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo1.getSth()<0.6?0.01f:userinfo1.getSth()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo1.getEng()<0.6?0.01f:userinfo1.getEng()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo1.getFa()<0.6?0.01f:userinfo1.getFa()-0.6)*2.5f,0.8));


            Path pt2 = SkiaUtil.creat6(250, 3,
                    (float)Math.pow((userinfo2.getPtt()<0.6?0.01f:userinfo2.getPtt()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo2.getSta()<0.6?0.01f:userinfo2.getSta()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo2.getStb()<0.6?0.01f:userinfo2.getStb()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo2.getSth()<0.6?0.01f:userinfo2.getSth()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo2.getEng()<0.6?0.01f:userinfo2.getEng()-0.6)*2.5f,0.8),
                    (float)Math.pow((userinfo2.getFa()<0.6?0.01f:userinfo2.getFa()-0.6)*2.5f,0.8));


            canvas.drawPath(pt1, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,50,196,233));
            canvas.drawPath(pt1, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 50,196,233));

            canvas.drawPath(pt2, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,240,0,110));
            canvas.drawPath(pt2, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 0, 110));


            canvas.drawRRect(RRect.makeXYWH(-150,-226.5f,60,25,5),bg2);
            canvas.drawString("ptt",-144,-208f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(100,-226.5f,60,25,5),bg2);
            canvas.drawString("sta",108,-208.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(230,-10,50,25,5),bg2);
            canvas.drawString("stb",240,8,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(105,206.5f,50,25,5),bg2);
            canvas.drawString("sth",114,224.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-145,206.5f,50,25   ,5),bg2);
            canvas.drawString("eng",-139,223.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-270,-10,50,25,5),bg2);
            canvas.drawString("acc",-260,8f,smileFont,wp);

            canvas.restore();
            canvas.translate(0,575);

            TextLine temp;
            DecimalFormat dx = new DecimalFormat("0.00");
            canvas.drawRRect(RRect.makeXYWH(50,0,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getPtt()*100),60,35,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getPtt()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),35,wp2);
            temp = TextLine.make("PTT", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,35,wp);

            canvas.drawRRect(RRect.makeXYWH(50,75,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getSta()*100),60,110,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getSta()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),110,wp2);
            temp = TextLine.make("STA", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,110,wp);

            canvas.drawRRect(RRect.makeXYWH(50,150,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getStb()*100),60,185,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getStb()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),185,wp2);
            temp = TextLine.make("STB", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,185,wp);

            canvas.drawRRect(RRect.makeXYWH(50,225,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getFa()*100),60,260,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getFa()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),260,wp2);
            temp = TextLine.make("ACC", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,260,wp);

            canvas.drawRRect(RRect.makeXYWH(50,300,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getSth()*100),60,345,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getSth()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),345,wp2);
            temp = TextLine.make("STH", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,345,wp);

            canvas.drawRRect(RRect.makeXYWH(50,375,500,50,10),edP);
            canvas.drawString(dx.format(userinfo1.getEng()*100),60,410,middleFont,wp1);
            temp = TextLine.make(dx.format(userinfo2.getEng()*100), middleFont);
            canvas.drawTextLine(temp,540-temp.getWidth(),410,wp2);
            temp = TextLine.make("ENG", middleFont);
            canvas.drawTextLine(temp,50+(500-temp.getWidth())/2,410,wp);


//            var fromx = TextLine.make("Thanks for Muziyami",smileFont);
//            canvas.drawTextLine(fromx,(600-line.getWidth())/2,surface.getHeight()-2*line.getHeight(),new Paint().setARGB(255,255,255,255));

            var datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
        */
        byte[] datebyte = drow(userinfo1,userinfo2);
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
    }
    byte[] drow(PPmObject userinfo1, PPmObject userinfo2) throws Exception{
        byte[] date;
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = FontCfg.TORUS_REGULAR;
             Font fontA = new Font(fontface, 80);
             Font fontB = new Font(fontface, 64);

             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();
            canvas.save();
            Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPminusBG.png")));
            Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPHexPanel.png")));
            canvas.drawImage(bg1,0,0);
            canvas.drawImage(bg2,0,0);

            double[] hex1 = new double[]{
                    userinfo1.getFa(),
                    userinfo1.getPtt(),
                    userinfo1.getSta(),
                    userinfo1.getStb(),
                    userinfo1.getEng(),
                    userinfo1.getSth(),
            };
            double[] hex2 = new double[]{
                    userinfo2.getFa(),
                    userinfo2.getPtt(),
                    userinfo2.getSta(),
                    userinfo2.getStb(),
                    userinfo2.getEng(),
                    userinfo2.getSth(),
            };
            canvas.translate(960,440);
            org.jetbrains.skija.Path pt1 = SkiaUtil.creat6(390, 5,(float) hex1[0],(float)hex1[1],(float)hex1[2],(float)hex1[3],(float)hex1[4],(float)hex1[5]);
            org.jetbrains.skija.Path pt2 = SkiaUtil.creat6(390, 5,(float) hex2[0],(float) hex2[1],(float) hex2[2],(float) hex2[3],(float) hex2[4],(float) hex2[5]);
            canvas.drawPath(pt1,new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1,new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt2,new Paint().setARGB(255,223,0,36).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt2,new Paint().setARGB(102,223,0,36).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP-",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();

            canvas.save();
            canvas.translate(130,80);
            Image head1 = SkiaUtil.lodeNetWorkImage(userinfo1.headURL);
            var ss = Surface.makeRasterN32Premul(300,300);
            ss.getCanvas()
                    .setMatrix(Matrix33.makeScale(300f/head1.getWidth(),300f/head1.getHeight()))
                    .drawImage(head1,0,0);
            head1 = ss.makeImageSnapshot();
            canvas.clipRRect(RRect.makeXYWH(0,0,300,300,40));
            canvas.drawImage(head1,0,0);
            canvas.restore();

            canvas.save();
            canvas.translate(130,80);
            Image head2 = SkiaUtil.lodeNetWorkImage(userinfo2.headURL);
            ss = Surface.makeRasterN32Premul(300,300);
            ss.getCanvas()
                    .setMatrix(Matrix33.makeScale(300f/head2.getWidth(),300f/head2.getHeight()))
                    .drawImage(head2,0,0);
            head2 = ss.makeImageSnapshot();
            canvas.clipRRect(RRect.makeXYWH(0,0,300,300,40));
            canvas.drawImage(head2,0,0);
            canvas.restore();

            drowLname(canvas,fontA,white,userinfo1.getName());
            drowRname(canvas,fontA,white,userinfo2.getName());

            drowLppm(canvas,fontB,fontA,white,hex1, userinfo1.getTtl());
            drowRppm(canvas,fontB,fontA,white,hex2, userinfo2.getTtl());

            TextLine thx = TextLine.make("本功能的算法及UI设计均来源与Muziyami设计",new Font(fontface,30));
            canvas.drawTextLine(thx, 0.5f*(surface.getWidth()-thx.getWidth()),(surface.getHeight()-30),white);
            date = surface.makeImageSnapshot().encodeToData().getBytes();

        }
        return date;
    }
    public static void drowLname(Canvas canvas, Font font, Paint white, String name){
        canvas.save();
        canvas.translate(280,440);
        TextLine text = TextLine.make(name, font);
        if (text.getWidth() > 500) text = TextLine.make(name.substring(0,8)+"...",font);
        canvas.drawTextLine(text, -0.5f*text.getWidth(),0.25f*text.getHeight(),white);
        canvas.restore();
    }
    public static void drowRname(Canvas canvas, Font font, Paint white, String name){
        canvas.save();
        canvas.translate(1640,440);
        TextLine text = TextLine.make(name, font);
        if (text.getWidth() > 500) text = TextLine.make(name.substring(0,8)+"...",font);
        canvas.drawTextLine(text, -0.5f*text.getWidth(),0.25f*text.getHeight(),white);
        canvas.restore();
    }
    public static void drowLppm(Canvas canvas, Font font, Font B, Paint white, double[] val, double all){
        if (val.length != 6) return;
        double[] date = new double[val.length];
        for (int i = 0; i < date.length; i++) {
            date[i] = val[i]*100;
        }
        DecimalFormat dx = new DecimalFormat("0.00");
        canvas.save();
        canvas.translate(100,520);
        TextLine k1 = TextLine.make("准度",font);
        TextLine v1 = TextLine.make(dx.format(date[0]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("潜力",font);
        v1 = TextLine.make(dx.format(date[1]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("耐力",font);
        v1 = TextLine.make(dx.format(date[2]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("稳定",font);
        v1 = TextLine.make(dx.format(date[3]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("肝力",font);
        v1 = TextLine.make(dx.format(date[4]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("实力",font);
        v1 = TextLine.make(dx.format(date[5]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.restore();
        canvas.save();
        canvas.translate(920,880);
        v1 = TextLine.make(dx.format(all),B);
        canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
        canvas.restore();
    }
    public static void drowRppm(Canvas canvas, Font font, Font B, Paint white, double[] val, double all){
        if (val.length != 6) return;
        double[] date = new double[val.length];
        for (int i = 0; i < date.length; i++) {
            date[i] = val[i]*100;
        }
        DecimalFormat dx = new DecimalFormat("0.00");
        canvas.save();
        canvas.translate(1460,520);
        TextLine k1 = TextLine.make("准度",font);
        TextLine v1 = TextLine.make(dx.format(date[0]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("潜力",font);
        v1 = TextLine.make(dx.format(date[1]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("耐力",font);
        v1 = TextLine.make(dx.format(date[2]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("稳定",font);
        v1 = TextLine.make(dx.format(date[3]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("肝力",font);
        v1 = TextLine.make(dx.format(date[4]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.translate(0,90);
        k1 = TextLine.make("实力",font);
        v1 = TextLine.make(dx.format(date[5]),font);
        canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
        canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
        canvas.restore();
        canvas.save();
        canvas.translate(1000,880);
        v1 = TextLine.make(dx.format(all),B);
        canvas.drawTextLine(v1,0,v1.getCapHeight(),white);
        canvas.restore();
    }
}
