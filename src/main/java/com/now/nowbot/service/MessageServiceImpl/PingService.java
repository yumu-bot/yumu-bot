package com.now.nowbot.service.MessageServiceImpl;


import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import io.github.humbleui.skija.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;

@Service("PING")
public class PingService implements MessageService<Matcher> {

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.PING.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
//    @CheckPermission(roles = {"we","are","winner"})
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        byte[] date = null;
        try (Surface surface = Surface.makeRasterN32Premul(240,240)){
            Canvas canvas = surface.getCanvas();

            Image BG;
            try {
                BG = SkiaImageUtil.getImage(STR."\{NowbotConfig.EXPORT_FILE_PATH}ExportFileV3/help-ping.png");
                canvas.drawImage(BG,0,0);
            } catch (IOException ignored) {
                //throw new RuntimeException("ping failed cuz no BG??!");
            }

            Font x = new Font(SkiaUtil.getTorusRegular(), 60);
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
        if (date != null) {
            from.sendImage(date).recallIn(5000);
        }

    }
}
