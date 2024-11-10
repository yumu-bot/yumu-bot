package com.now.nowbot.service.messageServiceImpl;


import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.tencent.TencentMessageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.OfficialInstruction;
import com.now.nowbot.util.QQMsgUtil;
import io.github.humbleui.skija.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("PING")
public class PingService implements MessageService<Matcher>, TencentMessageService<Matcher> {

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<Matcher> data) {
        var m = Instruction.PING.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public @Nullable Matcher accept(MessageEvent event, String messageText) {
        var m = OfficialInstruction.PING.matcher(messageText);
        if (m.find()) {
            return m;
        }
        return null;
    }

    @Override
    public @Nullable MessageChain reply(@NotNull MessageEvent event, Matcher param) throws Throwable {
        var image = getImage();
        if (image == null) return null;
        return QQMsgUtil.getImage(image);
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var image = getImage();
        if (image != null) {
            from.sendImage(image).recallIn(5000);
        }
    }

    public byte[] getImage() {
        byte[] image;
        try (Surface surface = Surface.makeRasterN32Premul(648, 648)) {
            Canvas canvas = surface.getCanvas();

            try {
                var file = Files.readAllBytes(
                        Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("help-ping.png")
                );
                var BG = Image.makeFromEncoded(file);
                canvas.drawImage(BG, 0, 0);

            } catch (IOException ignored) {
                //throw new RuntimeException("ping failed cuz no BG??!");
            }

            Font x = new Font(DataUtil.getTORUS_REGULAR(), 160);
            TextLine t = TextLine.make("?", x);
            canvas.drawTextLine(t, (648 - t.getWidth()) / 2, 208, new Paint().setARGB(255,191,193,124));

            x.close();
            t.close();

            x = new Font(DataUtil.getTORUS_REGULAR(), 40);
            t = TextLine.make(STR."\{System.currentTimeMillis()}ms", x);
            canvas.drawTextLine(t, 10, t.getCapHeight() + 10, new Paint().setARGB(200,191,193,124));
            x.close();
            t.close();
            image = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        return image;
    }
}
