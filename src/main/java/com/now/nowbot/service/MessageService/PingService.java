package com.now.nowbot.service.MessageService;


import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("ping")
public class PingService implements MessageService{
    @Override
//    @CheckPermission(roles = {"we","are","winner"})
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        byte[] date = null;
        try (Surface surface = Surface.makeRasterN32Premul(500,180)){
            Canvas canvas = surface.getCanvas();

            Font x = new Font(SkiaUtil.getTorusRegular(), 100);
            canvas.clear(Color.makeRGB(0,169,248));
            TextLine t = TextLine.make("PONG!",x);
            canvas.drawTextLine(t,(500-t.getWidth())/2, t.getHeight(),new Paint().setARGB(255,192,219,288));
            x.close();t.close();
            x = new Font(SkiaUtil.getTorusRegular(),20);
            t = TextLine.make(System.currentTimeMillis()+"ms", x);
            canvas.drawTextLine(t,0,t.getCapHeight()+4, new Paint().setARGB(255,192,219,288));
            x.close();t.close();
            date = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (date != null) QQMsgUtil.sendImage(from, date).recallIn(2000);

    }
}
