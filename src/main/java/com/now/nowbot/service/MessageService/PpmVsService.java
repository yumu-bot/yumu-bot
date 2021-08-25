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
        super(Pattern.compile("[!！]\\s?(?i)ppmvs(?<s>(?i)s)?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"ppmvs");
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

        if (matcher.group("s") != null || Math.random()<=0.01){
            Image spr = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"PPminusSurprise.png");
            PPmObject userinfo1;
            getuser:{
                var userdate = osuGetService.getPlayerOsuInfo(user);
                var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                userinfo1 = PPmObject.presOsu(userdate, bpdate);
            }
            try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
                 Typeface fontface = FontCfg.TORUS_REGULAR;
                 Font fontA = new Font(fontface, 80);
                 Paint white = new Paint().setARGB(255,255,255,255);
            ){
                var canvas = surface.getCanvas();
                Image img = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"mascot.png");
                canvas.drawImage(img,surface.getWidth()-img.getWidth(), surface.getHeight()-img.getHeight());
                Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPminusBG.png")));
                Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"PPHexPanel.png")));
                Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH+"mascot.png")));
                canvas.drawImage(bg1,0,0);
                canvas.drawImage(bg2,0,0);
                canvas.drawImage(bg3,surface.getWidth()-img.getWidth(),surface.getHeight()-img.getHeight(),new Paint().setAlpha(51));
                canvas.drawImage(spr,0,0);

                drowLname(canvas,fontA,white,userinfo1.getName());

                var date = surface.makeImageSnapshot().encodeToData().getBytes();
                from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(date),from));
            }
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
        byte[] datebyte = drow(userinfo1,userinfo2);
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
    }
    static byte[] drow(PPmObject userinfo1, PPmObject userinfo2) throws Exception{
        byte[] date;
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = FontCfg.TORUS_REGULAR;
             Font fontA = new Font(fontface, 80);
             Font fontB = new Font(fontface, 64);

             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();
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
                    Math.pow((userinfo1.getPtt() < 0.6 ? 0 : userinfo1.getPtt() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo1.getSta() < 0.6 ? 0 : userinfo1.getSta() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo1.getStb() < 0.6 ? 0 : userinfo1.getStb() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo1.getEng() < 0.6 ? 0 : userinfo1.getEng() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo1.getSth() < 0.6 ? 0 : userinfo1.getSth() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo1.getFa() < 0.6 ? 0 : userinfo1.getFa() - 0.6) * 2.5f, 0.8),
            };
            double[] hex2 = new double[]{
                    Math.pow((userinfo2.getPtt() < 0.6 ? 0 : userinfo2.getPtt() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo2.getSta() < 0.6 ? 0 : userinfo2.getSta() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo2.getStb() < 0.6 ? 0 : userinfo2.getStb() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo2.getEng() < 0.6 ? 0 : userinfo2.getEng() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo2.getSth() < 0.6 ? 0 : userinfo2.getSth() - 0.6) * 2.5f, 0.8),
                    Math.pow((userinfo2.getFa() < 0.6 ? 0 : userinfo2.getFa() - 0.6) * 2.5f, 0.8),
            };
            canvas.translate(960,440);
            org.jetbrains.skija.Path pt1 = SkiaUtil.creat6(390, 5,(float) hex1[0],(float)hex1[1],(float)hex1[2],(float)hex1[3],(float)hex1[4],(float)hex1[5]);
            org.jetbrains.skija.Path pt2 = SkiaUtil.creat6(390, 5,(float) hex2[0],(float) hex2[1],(float) hex2[2],(float) hex2[3],(float) hex2[4],(float) hex2[5]);
            canvas.drawPath(pt2,new Paint().setARGB(255,223,0,36).setStroke(true).setStrokeWidth(5)); 
            canvas.drawPath(pt2,new Paint().setARGB(102,223,0,36).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(pt1,new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(pt1,new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP-",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();


            Image head1 = SkiaUtil.lodeNetWorkImage(userinfo1.headURL);
            drowLhead(canvas, head1);

            Image head2 = SkiaUtil.lodeNetWorkImage(userinfo2.headURL);
            drowRhead(canvas, head2);

            drowLname(canvas,fontA,white,userinfo1.getName());
            drowRname(canvas,fontA,white,userinfo2.getName());

            drowLppm(canvas,fontB,fontA,white,new double[]{
                    userinfo1.getFa(),
                    userinfo1.getPtt(),
                    userinfo1.getSta(),
                    userinfo1.getStb(),
                    userinfo1.getEng(),
                    userinfo1.getSth(),
            }, userinfo1.getTtl()*100);
            drowRppm(canvas,fontB,fontA,white,new double[]{
                    userinfo2.getFa(),
                    userinfo2.getPtt(),
                    userinfo2.getSta(),
                    userinfo2.getStb(),
                    userinfo2.getEng(),
                    userinfo2.getSth(),
            }, userinfo2.getTtl()*100);


            date = surface.makeImageSnapshot().encodeToData().getBytes();

        }
        return date;
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
        canvas.drawImage(head,0,0);
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
        canvas.drawImage(head,0,0);
        canvas.restore();
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
