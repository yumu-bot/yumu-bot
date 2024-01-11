package com.now.nowbot.service.MessageServiceImpl;


import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import io.github.humbleui.skija.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        byte[] data;
        try (Surface surface = Surface.makeRasterN32Premul(240,240)){
            Canvas canvas = surface.getCanvas();

            try {
                var file = Files.readAllBytes(
                        Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("help-ping.png")
                );
                var BG = Image.makeDeferredFromEncodedBytes(file);
                canvas.drawImage(BG,0,0);

            } catch (IOException ignored) {
                //throw new RuntimeException("ping failed cuz no BG??!");
            }

            Font x = new Font(DataUtil.getTorusRegular(), 60);
            TextLine t = TextLine.make("PONG!",x);
            canvas.drawTextLine(t,(240 - t.getWidth())/2, t.getHeight(), new Paint().setARGB(255,192,219,288));

            x.close();
            t.close();
            x = new Font(DataUtil.getTorusRegular(),20);
            t = TextLine.make(System.currentTimeMillis() + "ms", x);
            canvas.drawTextLine(t,0,t.getCapHeight() + 4, new Paint().setARGB(200,255,255,255));
            x.close();t.close();
            data = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (data != null) {
            from.sendImage(data).recallIn(5000);
        }

    }
}
