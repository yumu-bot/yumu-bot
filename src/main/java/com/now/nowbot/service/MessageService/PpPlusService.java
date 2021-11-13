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

            Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPPlusBG.png")));
            Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPHexPanel.png")));
            Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPPlusOverlay.png")));
            Image bg4 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mascot.png")));
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
            TextLine v1 = TextLine.make(dx.format(date1.getJump()),fontB);
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

            PpPlusVsService.drawLhead(canvas, SkiaUtil.lodeNetWorkImage(head1));

            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
}
