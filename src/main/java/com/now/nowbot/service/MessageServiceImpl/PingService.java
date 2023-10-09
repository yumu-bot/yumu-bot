package com.now.nowbot.service.MessageServiceImpl;


import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import io.github.humbleui.skija.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ping")
public class PingService implements MessageService<Matcher> {

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((ym)?(ping|pi(?!\\w))+|yumu)");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
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

            Image BG = null;
            try {
                BG = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/help-ping.png");
            } catch (IOException e) {
                throw new RuntimeException("ping failed cuz no BG??!");
            }
            canvas.drawImage(BG,0,0);

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
