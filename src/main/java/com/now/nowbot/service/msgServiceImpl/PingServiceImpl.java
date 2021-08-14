package com.now.nowbot.service.msgServiceImpl;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.stereotype.Service;

@Service
public class PingServiceImpl extends MessageService{

    public PingServiceImpl() {
        super("ping");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }
        byte[] data = null;
        try (Surface surface = Surface.makeRasterN32Premul(500,300)){
            Canvas canvas = surface.getCanvas();
            Typeface face = FriendServiceImpl.face;

            canvas.clear(Color.makeRGB(0,169,248));
            Font x = new Font(face, 100);
            TextLine t = TextLine.make("PONG!",x);
            canvas.drawTextLine(t,(500-t.getWidth())/2, t.getHeight(),new Paint().setARGB(255,192,219,288));

            data = surface.makeImageSnapshot().encodeToData().getBytes();
        }catch (Exception e){}
        if (data != null) from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(data), from)).recallIn(2000);
    }
}
