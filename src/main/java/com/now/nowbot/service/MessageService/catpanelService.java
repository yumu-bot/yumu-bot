package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.Rect;
import org.jetbrains.skija.Surface;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("cpanel")
public class catpanelService extends MsgSTemp implements MessageService {
    catpanelService() {
        super(Pattern.compile("[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"), "cpanel");
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var rep = event.getMessage().get(QuoteReply.Key);
        Image img = null;
        if (rep != null) {
            img = (Image) rep.getSource().getOriginalMessage().stream().filter(i -> i instanceof Image).findFirst().orElse(null);
        } else {
            img = (Image) event.getMessage().stream().filter(i -> i instanceof Image).findFirst().orElse(null);
        }
        System.out.println(rep);
        System.out.println(rep.getSource());
        System.out.println(rep.getSource().getInternalIds()[0]);
        System.out.println(rep.getSource().getIds()[0]);
        System.out.println(rep.getSource().getTargetId());
        System.out.println(rep.getSource().getOriginalMessage());
        System.out.println(rep.getSource().getOriginalMessage().getClass());
        System.out.println(rep.getSource().getOriginalMessage().contentToString());
        for (var r : rep.getSource().getOriginalMessage()) {
            System.out.println(r.getClass().getName());
            System.out.println((r instanceof Image)+r.toString());
        }
        if (img == null) {
            from.sendMessage("没有图片！");
            return;
        }
        boolean stl = false;
        int tmd = 0;
        while (matcher.find()) {
            if (matcher.group("bk") != null) {
                if (tmd < 0 || tmd > 100) throw new TipsException("数值不合法");
            }
            if (matcher.group("yl") != null) stl = true;

        }
        org.jetbrains.skija.Image netImg = null;
        org.jetbrains.skija.Image topImg = null;

        netImg = SkiaUtil.lodeNetWorkImage(Image.queryUrl(img));
        topImg = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "panel05.png");

        int imgWidth = netImg.getWidth();
        int imgHeight = netImg.getHeight();

        org.jetbrains.skija.Image siImg;
        if (1f * imgWidth / imgHeight < 1200f / 857) {
            org.jetbrains.skija.Image img1 = SkiaUtil.getScaleImage(netImg, 1200, 1200 * imgHeight / imgWidth);
            siImg = SkiaUtil.getCutImage(img1, 0, (img1.getHeight() - 857) / 2, 1200, 857);
        } else {
            org.jetbrains.skija.Image img1 = SkiaUtil.getScaleImage(netImg, 857 * imgWidth / imgHeight, 857);
            siImg = SkiaUtil.getCutImage(img1, (img1.getWidth() - 1200) / 2, 0, 1200, 857);
        }
        byte[] pngBytes;
        try (Surface surface = Surface.makeRasterN32Premul(1200, 857);) {
            Canvas canvas = surface.getCanvas();
            canvas.drawImage(siImg, 0, 0);
            canvas.drawRect(Rect.makeXYWH(0, 0, surface.getWidth(), surface.getHeight()), new Paint().setARGB(255 * tmd / 100, 0, 0, 0));
            canvas.drawImage(topImg, 0, 0);
            try {
                if (stl) {
                    canvas.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ylbx.png"), 0, 0);
                } else {
                    canvas.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "lbx.png"), 0, 0);
                }
            } catch (IOException e) {
                throw new TipsException("服务器文件读取异常，请联系管理员处理");
            }
            pngBytes = surface.makeImageSnapshot().encodeToData().getBytes();

        }
        from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(pngBytes), from));
    }
}
