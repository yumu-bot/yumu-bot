package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.PPPlusDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.PPPlusObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.regex.Matcher;

@Service("ppp")
public class PpPlusService implements MessageService{
    Logger log = LoggerFactory.getLogger(PpPlusService.class);
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    PPPlusDao ppPlusDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable{
        Contact from = event.getSubject();

        BinUser user = null;
        int id;
        At at;
        at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if (at != null){
            user = BindingUtil.readUser(at.getTarget());
            id = user.getOsuID();
        }else {
            String name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                user = BindingUtil.readUser(event.getSender().getId());
                id = user.getOsuID();
            }else {
                id = osuGetService.getOsuId(name);
            }
        }


        String id1,head1;
        id1 = String.valueOf(id);
        if (user != null) {
            head1 = osuGetService.getPlayerOsuInfo(user).getString("avatar_url");
        }else {
            head1 = osuGetService.getPlayerOsuInfo(id).getString("avatar_url");
        }


        PPPlusObject date1 = null;
        try {
            date1 = ppPlusDao.getobject(id1);
        } catch (Exception e) {
//            throw e;
            log.info("ppp",e);
            throw new TipsException("那个破网站连不上");
        }

        float[] hex1 = ppPlusDao.ppsize(date1);

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
            var paths = SkiaUtil.creat6(390, 10, hex1);
            canvas.drawPath(paths[0],new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(paths[0],new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(paths[1],new Paint().setARGB(255,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP+",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();
            canvas.drawImage(bg3,513,74);

            canvas.save();
            canvas.translate(280,440);
            TextLine text = TextLine.make(date1.getName(), fontA);
            if (text.getWidth() > 500) text = TextLine.make(date1.getName().substring(0,8)+"...",fontA);
            canvas.drawTextLine(text, -0.5f*text.getWidth(),0.25f*text.getHeight(),white);
            canvas.restore();

            DecimalFormat dx = new DecimalFormat("0");
            canvas.save();
            canvas.translate(100,520);
            TextLine k1 = TextLine.make("Jump",fontB);
            TextLine v1 = TextLine.make(dx.format(date1.getJunp()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Flow",fontB);
            v1 = TextLine.make(dx.format(date1.getFlow()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Acc",fontB);
            v1 = TextLine.make(dx.format(date1.getAcc()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Sta",fontB);
            v1 = TextLine.make(dx.format(date1.getSta()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Spd",fontB);
            v1 = TextLine.make(dx.format(date1.getSpd()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Pre",fontB);
            v1 = TextLine.make(dx.format(date1.getPre()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            canvas.save();
            canvas.translate(920,880);
            v1 = TextLine.make(dx.format(date1.getTotal()),fontA);
            canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            PpPlusVsService.drowLhead(canvas, SkiaUtil.lodeNetWorkImage(head1));

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
        PPPlusObject js;
        try {
            if (user == null){
                js = ppPlusDao.getobject(""+osuGetService.getOsuId(name.trim()));
            }else {
                js = ppPlusDao.getobject(""+user.getOsuID());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new TipsException("连不上啊连不上！");
        }

        float[] date = osuGetService.ppPlus(new float[]{
                js.getJunp().floatValue(),
                js.getFlow().floatValue(),
                js.getAcc().floatValue(),
                js.getSta().floatValue(),
                js.getSpd().floatValue(),
                js.getPre().floatValue(),
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

            var line = TextLine.make(js.getName(),lagerFont);
            canvas.drawTextLine(line,(600-line.getWidth())/2,line.getHeight(),new Paint().setARGB(255,255,255,255));

            canvas.save();
            canvas.translate(300,325);

            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f)[0], bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)[0], bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f)[0], bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1)[0], bg1);

            Path[] pt = SkiaUtil.creat6(250, 3, date);

            canvas.drawPath(pt[0], new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,240,167,50));
            canvas.drawPath(pt[0], new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 167, 50));
            canvas.drawPath(pt[1], new Paint().setStrokeWidth(3).setStroke(false).setARGB(255,240,167,50));

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
            canvas.drawString("jump:"+(int)js.getJunp().intValue(),60,35,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,0,225,50,10),edP);
            canvas.drawString("flow:"+(int)js.getFlow().intValue(),335,35,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,75,225,50,10),edP);
            canvas.drawString("acc:"+(int)js.getAcc().intValue(),60,110,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,75,225,50,10),edP);
            canvas.drawString("sta:"+(int)js.getSta().intValue(),335,110,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,150,225,50,10),edP);
            canvas.drawString("spd:"+(int)js.getSpd().intValue(),60,185,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,150,225,50,10),edP);
            canvas.drawString("pre:"+(int)js.getPre().intValue(),335,185,middleFont,wp);

            canvas.restore();
            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
}
