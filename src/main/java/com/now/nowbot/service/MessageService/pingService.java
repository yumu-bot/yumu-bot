package com.now.nowbot.service.MessageService;

import com.now.nowbot.entity.FontCfg;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ping")
public class pingService  extends MsgSTemp implements MessageService{
    pingService(){
        super(Pattern.compile("[!！]\\s?(?i)ping"),"ping");
    }

    @Override
//    @CheckPermission(roles = {"we","are","winner"})
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        byte[] data = null;
        try (Surface surface = Surface.makeRasterN32Premul(500,180)){
            Canvas canvas = surface.getCanvas();
            Typeface face = FontCfg.JP;

            canvas.clear(Color.makeRGB(0,169,248));
            Font x = new Font(face, 100);
            TextLine t = TextLine.make("PONG!",x);
            canvas.drawTextLine(t,(500-t.getWidth())/2, t.getHeight(),new Paint().setARGB(255,192,219,288));

            data = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (data != null) from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(data), from)).recallIn(2000);

    }
}
