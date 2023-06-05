package com.now.nowbot.service.MessageService;


import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;

@Service("ping")
public class PingService implements MessageService{
    @Override
//    @CheckPermission(roles = {"we","are","winner"})
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        byte[] date = null;
        try (Surface surface = Surface.makeRasterN32Premul(240,240)){
            Canvas canvas = surface.getCanvas();

            Image BG = null;
            try {
                BG = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/help-ping.png");
            } catch (IOException e) {
                throw new RuntimeException("ping failed cuz no BG??!");
            }
            canvas.drawImage(BG,0,0);

            Font x = new Font(SkiaUtil.getTorusRegular(), 60);
            canvas.clear(Color.makeARGB(180,255,255,255));
            TextLine t = TextLine.make("PONG!",x);
            canvas.drawTextLine(t,(240 - t.getWidth())/2, t.getHeight(), new Paint().setARGB(255,192,219,288));

            x.close();
            t.close();
            x = new Font(SkiaUtil.getTorusRegular(),20);
            t = TextLine.make(System.currentTimeMillis() + "ms", x);
            canvas.drawTextLine(t,0,t.getCapHeight() + 4, new Paint().setARGB(200,255,255,255));
            x.close();t.close();
            date = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (date != null) QQMsgUtil.sendImage(from, date).recallIn(5000);

    }
}
